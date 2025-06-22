package client.controller;

import client.MessageSender;
import client.audio.AudioCaptureService;
import client.audio.AudioPlaybackService;
import client.audio.AudioStreamManager;
import client.model.ClientModel;
import client.view.VoiceCallView;
import info.Voice_info;
import info.encap_info;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音通话控制器
 * 管理语音通话的状态和用户界面
 */
public class VoiceCallController {
    // 客户端模型
    private final ClientModel model;
    // 消息发送器
    private final MessageSender messageSender;
    // 当前活动的通话窗口
    private Map<Integer, VoiceCallView> activeCallViews = new ConcurrentHashMap<>();
    // 音频流管理器
    private Map<Integer, AudioStreamManager> audioStreamManagers = new ConcurrentHashMap<>();
    // 音频播放服务
    private AudioPlaybackService playbackService;
    // 基础UDP端口
    private static final int BASE_PORT = 10000;
    // 用于存储远程主机信息的映射
    private Map<Integer, Voice_info> pendingRemoteEndpoints = null;

    /**
     * 构造函数
     * @param model 客户端模型
     * @param messageSender 消息发送器
     */
    public VoiceCallController(ClientModel model, MessageSender messageSender) {
        this.model = model;
        this.messageSender = messageSender;

        // 初始化音频播放服务
        this.playbackService = new AudioPlaybackService();
        try {
            this.playbackService.startPlayback();
        } catch (LineUnavailableException e) {
            System.err.println("初始化音频播放服务失败: " + e.getMessage());
        }
    }

    /**
     * 发起一对一语音通话
     * @param targetUsername 目标用户名
     */
    public void initiateCall(String targetUsername) {
        // 获取当前用户名
        String currentUsername = model.getCurrentUser();

        // 生成通话ID
        int callId = new Random().nextInt(1000000);

        // 创建语音信息
        Voice_info voiceInfo = new Voice_info();
        voiceInfo.setCall_id(callId);
        voiceInfo.setFrom_username(currentUsername);
        voiceInfo.addParticipant(targetUsername);
        voiceInfo.setIs_conference(false);
        voiceInfo.setStatus(Voice_info.CallStatus.REQUESTING);
        voiceInfo.setCallType(Voice_info.CallType.AUDIO_ONLY);

        // 设置UDP端口和本地主机地址
        int localPort = BASE_PORT + callId % 1000;
        String localHost = getLocalIpAddress();
        voiceInfo.setHost(localHost);
        voiceInfo.setPort(localPort);

        System.out.println("发起语音通话: 目标=" + targetUsername);

        // 发送请求
        sendVoiceCallMessage(voiceInfo);

        // 显示呼叫界面
        showCallView(voiceInfo);
    }

    /**
     * 发起多方语音会议
     * @param participants 参与者列表
     */
    public void initiateConference(List<String> participants) {
        // 获取当前用户名
        String currentUsername = model.getCurrentUser();

        // 生成通话ID和会议ID
        int callId = new Random().nextInt(1000000);
        int conferenceId = new Random().nextInt(1000000);

        // 创建语音信息
        Voice_info voiceInfo = new Voice_info();
        voiceInfo.setCall_id(callId);
        voiceInfo.setConference_id(conferenceId);
        voiceInfo.setFrom_username(currentUsername);
        voiceInfo.setIs_conference(true);
        voiceInfo.setStatus(Voice_info.CallStatus.REQUESTING);
        voiceInfo.setCallType(Voice_info.CallType.AUDIO_ONLY);

        // 添加参与者
        for (String participant : participants) {
            if (!participant.equals(currentUsername)) {
                voiceInfo.addParticipant(participant);
            }
        }

        // 设置UDP端口和本地主机地址
        int localPort = BASE_PORT + conferenceId % 1000;
        String localHost = getLocalIpAddress();
        voiceInfo.setHost(localHost);
        voiceInfo.setPort(localPort);

        System.out.println("发起语音会议: 参与者=" + participants.size() + "人");

        // 发送请求
        sendVoiceCallMessage(voiceInfo);

        // 显示呼叫界面
        showCallView(voiceInfo);
    }

    /**
     * 接受语音通话
     * @param callId 通话ID
     */
    public void acceptCall(int callId) {
        // 检查是否已有该通话的视图
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) {
            System.err.println("找不到通话ID为 " + callId + " 的通话视图");
            return;
        }

        try {
            // 获取通话信息
            Voice_info voiceInfo = callView.getVoiceInfo();
            
            // 为通话设置本地端口
            int localPort = BASE_PORT + callId % 1000;
            
            // 尝试找到可用端口
            while (true) {
                try {
                    new java.net.DatagramSocket(localPort).close();
                    break; // 端口可用
                } catch (java.net.BindException e) {
                    localPort++; // 尝试下一个端口
                }
            }

            String localHost = getLocalIpAddress();

            // 保存对方的主机信息，用于后续音频流建立
            String remoteHost = voiceInfo.getHost();
            int remotePort = voiceInfo.getPort();

            // 设置本地主机信息
            voiceInfo.setHost(localHost);
            voiceInfo.setPort(localPort);
            voiceInfo.setStatus(Voice_info.CallStatus.ACCEPTED);

            System.out.println("接受通话: ID=" + callId);

            // 发送接受消息
            sendVoiceCallMessage(voiceInfo);

            // 更新UI状态
            callView.updateCallStatus(Voice_info.CallStatus.CONNECTING);

            // 保存远程主机信息
            if (pendingRemoteEndpoints == null) {
                pendingRemoteEndpoints = new HashMap<>();
            }
            
            Voice_info remoteInfo = new Voice_info();
            remoteInfo.setCall_id(callId);
            remoteInfo.setHost(remoteHost);
            remoteInfo.setPort(remotePort);
            pendingRemoteEndpoints.put(callId, remoteInfo);

        } catch (Exception e) {
            System.err.println("接受通话失败: " + e.getMessage());
            // 显示错误信息
            callView.showCallError(new Voice_info());
        }
    }

    /**
     * 拒绝语音通话
     * @param callId 通话ID
     */
    public void rejectCall(int callId) {
        // 检查是否已有该通话的视图
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) {
            return;
        }

        // 获取通话信息
        Voice_info voiceInfo = callView.getVoiceInfo();
        voiceInfo.setStatus(Voice_info.CallStatus.REJECTED);

        // 发送拒绝消息
        sendVoiceCallMessage(voiceInfo);

        // 关闭通话窗口
        callView.dispose();
        activeCallViews.remove(callId);
    }

    /**
     * 结束语音通话
     * @param callId 通话ID
     */
    public void endCall(int callId) {
        // 检查是否已有该通话的视图
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) {
            return;
        }

        // 获取通话信息
        Voice_info voiceInfo = callView.getVoiceInfo();
        voiceInfo.setStatus(Voice_info.CallStatus.ENDED);

        // 确保所有参与者都收到通话结束消息
        String currentUser = model.getCurrentUser();
        if (!voiceInfo.isIs_conference()) {
            // 确保参与者列表包含对方
            String otherParty = voiceInfo.getFrom_username().equals(currentUser) ?
                    (voiceInfo.getParticipants().isEmpty() ? null : voiceInfo.getParticipants().get(0)) :
                    voiceInfo.getFrom_username();
            
            if (otherParty != null && !voiceInfo.getParticipants().contains(otherParty)) {
                voiceInfo.addParticipant(otherParty);
            }
        }

        // 发送结束消息
        for (int i = 0; i < 3; i++) { // 重试三次
            sendVoiceCallMessage(voiceInfo);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 停止音频流
        stopAudioStream(callId);

        // 关闭通话窗口
        callView.showCallEnded();
        
        // 延迟关闭窗口
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callView.dispose();
                activeCallViews.remove(callId);
            }
        }, 2000);
    }

    /**
     * 处理收到的语音通话消息
     * @param voiceInfo 语音信息
     */
    public void handleVoiceCallMessage(Voice_info voiceInfo) {
        if (voiceInfo == null) {
            System.err.println("收到空的语音通话消息");
            return;
        }
        
        int callId = voiceInfo.getCall_id();
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);
        boolean isParticipant = false;
        
        for (String participant : voiceInfo.getParticipants()) {
            if (participant.equals(currentUsername)) {
                isParticipant = true;
                break;
            }
        }

        // 忽略与当前用户无关的消息
        if (!isInitiator && !isParticipant) {
            return;
        }

        System.out.println("收到语音通话消息: 状态=" + voiceInfo.getStatus() + ", ID=" + callId);

        switch (voiceInfo.getStatus()) {
            case REQUESTING:
                handleIncomingCall(voiceInfo);
                break;
            case ACCEPTED:
                handleCallAccepted(voiceInfo);
                break;
            case REJECTED:
                handleCallRejected(voiceInfo);
                break;
            case ENDED:
                handleCallEnded(voiceInfo);
                break;
            case CONNECTED:
                handleCallConnected(voiceInfo);
                break;
            case ERROR:
                handleCallError(voiceInfo);
                break;
        }
    }

    /**
     * 处理传入的通话请求
     * @param voiceInfo 语音信息
     */
    private void handleIncomingCall(Voice_info voiceInfo) {
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);
        
        // 如果当前用户是呼叫发起方，但已经有通话视图，则不重复处理
        if (isInitiator && activeCallViews.containsKey(voiceInfo.getCall_id())) {
            return;
        }

        // 显示来电界面
        showCallView(voiceInfo);
    }

    /**
     * 处理通话被接受
     * @param voiceInfo 语音信息
     */
    private void handleCallAccepted(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) return;
        
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        // 更新UI状态
        callView.updateCallStatus(Voice_info.CallStatus.CONNECTING);

        if (isInitiator) {
            // 保存远程主机信息
            String remoteHost = voiceInfo.getHost();
            int remotePort = voiceInfo.getPort();
            
            // 检查是否是服务器中继模式
            boolean isServerRelay = "SERVER_RELAY".equals(remoteHost);
            
            if (remoteHost != null && !remoteHost.isEmpty() && !isServerRelay) {
                // 保存远程主机信息
                if (pendingRemoteEndpoints == null) {
                    pendingRemoteEndpoints = new HashMap<>();
                }
                
                Voice_info remoteInfo = new Voice_info();
                remoteInfo.setCall_id(callId);
                remoteInfo.setHost(remoteHost);
                remoteInfo.setPort(remotePort);
                pendingRemoteEndpoints.put(callId, remoteInfo);
                
                // 开始音频流
                startAudioStream(voiceInfo);
            } else {
                // 服务器中继模式或无效地址
                System.out.println("检测到服务器中继模式或无效地址");
                // 尝试使用替代地址
                tryAlternativeAddresses(voiceInfo);
            }
        } else {
            // 检查是否有保存的远程主机信息
            if (pendingRemoteEndpoints != null && pendingRemoteEndpoints.containsKey(callId)) {
                Voice_info remoteInfo = pendingRemoteEndpoints.get(callId);
                voiceInfo.setHost(remoteInfo.getHost());
                voiceInfo.setPort(remoteInfo.getPort());
            }

            // 开始音频流
            startAudioStream(voiceInfo);

            // 发送连接成功消息
            Voice_info connectedInfo = new Voice_info();
            connectedInfo.setCall_id(callId);
            connectedInfo.setConference_id(voiceInfo.getConference_id());
            connectedInfo.setFrom_username(currentUsername);
            connectedInfo.setIs_conference(voiceInfo.isIs_conference());
            connectedInfo.setCallType(voiceInfo.getCallType());
            connectedInfo.setStatus(Voice_info.CallStatus.CONNECTED);
            connectedInfo.setHost(voiceInfo.getHost());
            connectedInfo.setPort(voiceInfo.getPort());
            connectedInfo.addParticipant(voiceInfo.getFrom_username());

            // 发送连接成功消息
            sendVoiceCallMessage(connectedInfo);
        }

        // 更新通话信息
        callView.setVoiceInfo(voiceInfo);
    }

    /**
     * 尝试使用替代地址
     * @param voiceInfo 语音信息
     */
    private void tryAlternativeAddresses(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        
        // 尝试从服务器获取的最后一个已知地址
        String remoteHost = voiceInfo.getFrom_username().equals(model.getCurrentUser()) ? 
                (voiceInfo.getParticipants().isEmpty() ? null : voiceInfo.getParticipants().get(0)) :
                voiceInfo.getFrom_username();
        
        if (remoteHost != null) {
            System.out.println("尝试使用替代地址连接用户: " + remoteHost);
            
            // 使用服务器中继模式时，尝试多个常见的局域网IP范围
            List<String> commonIPs = new ArrayList<>();
            
            // 添加一些常见的局域网IP范围进行尝试
            for (int i = 1; i <= 255; i++) {
                if (i % 50 == 0) {
                    commonIPs.add("192.168.1." + i);
                    commonIPs.add("10.0.0." + i);
                }
            }
            
            // 添加当前IP的相邻地址
            String localIP = getLocalIpAddress();
            if (!localIP.equals("SERVER_RELAY") && !localIP.equals("127.0.0.1")) {
                String[] parts = localIP.split("\\.");
                if (parts.length == 4) {
                    int lastOctet = Integer.parseInt(parts[3]);
                    
                    // 添加当前网段的一些IP进行尝试
                    for (int i = 1; i <= 20; i++) {
                        int newOctet = lastOctet + i;
                        if (newOctet <= 255) {
                            commonIPs.add(parts[0] + "." + parts[1] + "." + parts[2] + "." + newOctet);
                        }
                        
                        newOctet = lastOctet - i;
                        if (newOctet > 0) {
                            commonIPs.add(parts[0] + "." + parts[1] + "." + parts[2] + "." + newOctet);
                        }
                    }
                }
            }
            
            // 保存这些地址，让startAudioStream尝试连接
            if (pendingRemoteEndpoints == null) {
                pendingRemoteEndpoints = new HashMap<>();
            }
            
            Voice_info remoteInfo = new Voice_info();
            remoteInfo.setCall_id(callId);
            remoteInfo.setHost("MULTI_TRY");
            remoteInfo.setPort(voiceInfo.getPort());
            remoteInfo.setExtra(commonIPs); // 使用extra字段存储多个IP
            pendingRemoteEndpoints.put(callId, remoteInfo);
            
            // 开始音频流
            startAudioStream(voiceInfo);
        } else {
            System.err.println("无法获取远程用户信息，无法建立连接");
        }
    }

    /**
     * 处理通话被拒绝
     * @param voiceInfo 语音信息
     */
    private void handleCallRejected(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) return;

        // 显示被拒绝信息并延迟关闭
        callView.showCallRejected();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callView.dispose();
                activeCallViews.remove(callId);
            }
        }, 2000);
    }

    /**
     * 处理通话结束
     * @param voiceInfo 语音信息
     */
    private void handleCallEnded(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        
        // 停止音频流
        stopAudioStream(callId);

        // 关闭通话窗口
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView != null) {
            callView.showCallEnded();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    callView.dispose();
                    activeCallViews.remove(callId);
                }
            }, 2000);
        }
        
        // 清理资源
        if (pendingRemoteEndpoints != null) {
            pendingRemoteEndpoints.remove(callId);
        }
    }

    /**
     * 处理通话已连接
     * @param voiceInfo 语音信息
     */
    private void handleCallConnected(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) return;

        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        // 更新UI状态
        callView.updateCallStatus(Voice_info.CallStatus.CONNECTED);

        // 检查音频流是否已启动
        if (!audioStreamManagers.containsKey(callId)) {
            startAudioStream(voiceInfo);
        }

        // 更新通话信息
        callView.setVoiceInfo(voiceInfo);

        // 开始计时
        callView.startDurationTimer();

        System.out.println("通话已连接: ID=" + callId);
    }

    /**
     * 处理通话错误
     * @param voiceInfo 语音信息
     */
    private void handleCallError(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) return;

        // 显示错误信息
        callView.showCallError(voiceInfo);

        // 延迟关闭窗口
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callView.dispose();
                activeCallViews.remove(callId);
            }
        }, 2000);
    }

    /**
     * 显示通话界面
     * @param voiceInfo 语音信息
     */
    private void showCallView(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();

        // 检查是否已有该通话的视图
        if (activeCallViews.containsKey(callId)) {
            return;
        }

        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        // 创建通话视图
        VoiceCallView callView = new VoiceCallView(this, voiceInfo, currentUsername);
        activeCallViews.put(callId, callView);

        // 显示通话界面
        callView.setVisible(true);
    }

    /**
     * 开始音频流
     * @param voiceInfo 语音信息
     */
    private void startAudioStream(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();

        // 检查是否已经有音频流管理器
        if (audioStreamManagers.containsKey(callId)) {
            return;
        }

        try {
            // 设置本地端口
            int localPort = BASE_PORT + callId % 1000;
            
            // 尝试找到可用端口
            while (true) {
                try {
                    new java.net.DatagramSocket(localPort).close();
                    break; // 端口可用
                } catch (java.net.BindException e) {
                    localPort++; // 尝试下一个端口
                }
            }
            
            // 获取本地IP地址
            String localHost = getLocalIpAddress();

            // 获取远程主机信息
            String remoteHost = voiceInfo.getHost();
            int remotePort = voiceInfo.getPort();

            // 检查是否需要从pendingRemoteEndpoints获取远程主机信息
            boolean isMultiTry = false;
            List<String> alternativeIPs = null;
            
            if ((remoteHost == null || remoteHost.isEmpty() || remotePort == 0 || 
                 "SERVER_RELAY".equals(remoteHost)) && 
                pendingRemoteEndpoints != null && 
                pendingRemoteEndpoints.containsKey(callId)) {
                
                Voice_info remoteInfo = pendingRemoteEndpoints.get(callId);
                String tempRemoteHost = remoteInfo.getHost();
                int tempRemotePort = remoteInfo.getPort();
                
                // 检查是否是多IP尝试模式
                if ("MULTI_TRY".equals(tempRemoteHost) && remoteInfo.getExtra() != null) {
                    isMultiTry = true;
                    alternativeIPs = (List<String>) remoteInfo.getExtra();
                } else {
                    remoteHost = tempRemoteHost;
                    remotePort = tempRemotePort;
                }
            }

            System.out.println("启动音频流: 本地端口=" + localPort + 
                              ", 远程=" + (isMultiTry ? "多IP尝试模式" : remoteHost + ":" + remotePort));

            // 创建音频流管理器
            AudioStreamManager streamManager = new AudioStreamManager(localPort);
            streamManager.initialize();
            
            // 设置远程端点
            if (!isMultiTry && remoteHost != null && !remoteHost.isEmpty() && 
                !remoteHost.equals("SERVER_RELAY") && remotePort > 0) {
                streamManager.setRemoteEndpoint(remoteHost, remotePort);
            }

            // 注册通话ID到音频播放服务
            playbackService.registerCall(callId);

            // 设置音频数据监听器
            streamManager.addListener("default", new AudioStreamManager.AudioStreamListener() {
                @Override
                public void onAudioDataReceived(byte[] audioData) {
                    // 将音频数据发送到播放服务
                    playbackService.queueAudio(audioData, callId);
                }
            });

            // 开始接收
            streamManager.startReceiving();
            
            // 开始发送
            streamManager.startSending(null);

            // 保存音频流管理器
            audioStreamManagers.put(callId, streamManager);
            
            // 如果是多IP尝试模式，启动一个线程尝试多个IP
            if (isMultiTry && alternativeIPs != null && !alternativeIPs.isEmpty()) {
                final AudioStreamManager finalStreamManager = streamManager;
                final List<String> finalAlternativeIPs = alternativeIPs;
                final int finalRemotePort = remotePort; // 创建一个final副本
                
                new Thread(() -> {
                    System.out.println("开始尝试多个IP地址...");
                    
                    for (String ip : finalAlternativeIPs) {
                        try {
                            System.out.println("尝试连接IP: " + ip + ":" + finalRemotePort);
                            finalStreamManager.setRemoteEndpoint(ip, finalRemotePort);
                            
                            // 发送测试数据包
                            byte[] testData = "TEST".getBytes();
                            finalStreamManager.sendAudioData(testData, 0, testData.length);
                            
                            // 等待一段时间，看是否有响应
                            Thread.sleep(500);
                        } catch (Exception e) {
                            // 忽略错误，继续尝试下一个IP
                        }
                    }
                    
                    System.out.println("多IP尝试完成");
                }).start();
            }

            // 更新UI状态
            VoiceCallView callView = activeCallViews.get(callId);
            if (callView != null) {
                callView.updateCallStatus(Voice_info.CallStatus.CONNECTED);
            }

        } catch (Exception e) {
            System.err.println("创建音频流失败: " + e.getMessage());

            // 更新UI状态
            VoiceCallView callView = activeCallViews.get(callId);
            if (callView != null) {
                Voice_info errorInfo = new Voice_info();
                errorInfo.setCall_id(callId);
                errorInfo.setError("创建音频流失败");
                callView.showCallError(errorInfo);
            }
        }
    }

    /**
     * 获取本地IP地址
     * @return 本地IP地址
     */
    private String getLocalIpAddress() {
        try {
            // 优先获取局域网IP地址（非回环地址）
            List<String> allIPs = new ArrayList<>();
            List<String> lanIPs = new ArrayList<>();
            
            java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                java.net.NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                // 跳过禁用的、回环和虚拟接口
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        allIPs.add(ip);
                        
                        // 识别局域网IP地址
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || 
                            (ip.startsWith("172.") && 
                             Integer.parseInt(ip.split("\\.")[1]) >= 16 && 
                             Integer.parseInt(ip.split("\\.")[1]) <= 31)) {
                            lanIPs.add(ip);
                            System.out.println("找到局域网IP: " + ip);
                        }
                    }
                }
            }
            
            // 优先使用局域网IP
            if (!lanIPs.isEmpty()) {
                System.out.println("使用局域网IP: " + lanIPs.get(0));
                return lanIPs.get(0);
            }
            
            // 其次使用任何非回环IPv4地址
            if (!allIPs.isEmpty()) {
                System.out.println("使用非回环IP: " + allIPs.get(0));
                return allIPs.get(0);
            }
            
            // 如果无法获取有效的IP，使用服务器中继模式
            // 在这种模式下，我们返回一个特殊标记，表示需要通过服务器中继
            System.out.println("无法获取有效IP地址，将使用服务器中继模式");
            return "SERVER_RELAY";
        } catch (Exception e) {
            System.err.println("获取本地IP地址失败: " + e.getMessage());
            // 默认使用服务器中继模式
            return "SERVER_RELAY";
        }
    }

    /**
     * 停止音频流
     * @param callId 通话ID
     */
    private void stopAudioStream(int callId) {
        try {
            // 关闭音频流管理器
            AudioStreamManager streamManager = audioStreamManagers.get(callId);
            if (streamManager != null) {
                streamManager.close();
                audioStreamManagers.remove(callId);
            }
            
            // 停止音频播放
            if (playbackService != null) {
                playbackService.stopPlaybackForCall(callId);
            }
        } catch (Exception e) {
            System.err.println("停止音频流时出错: " + e.getMessage());
        }
    }

    /**
     * 发送语音通话消息
     * @param voiceInfo 语音信息
     */
    private void sendVoiceCallMessage(Voice_info voiceInfo) {
        encap_info encapInfo = new encap_info();
        encapInfo.set_type(8);
        encapInfo.set_voice_info(voiceInfo);
        messageSender.sendVoiceCallMessage(encapInfo);
    }

    /**
     * 关闭控制器
     */
    public void shutdown() {
        // 关闭所有通话
        for (Integer callId : new ArrayList<>(activeCallViews.keySet())) {
            endCall(callId);
        }

        // 关闭音频播放服务
        if (playbackService != null) {
            playbackService.shutdown();
        }
    }
}

