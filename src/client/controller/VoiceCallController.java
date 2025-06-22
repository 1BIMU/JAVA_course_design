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
        voiceInfo.setFrom_username(currentUsername); // 确保设置正确的发起人
        voiceInfo.addParticipant(targetUsername);
        voiceInfo.setIs_conference(false);
        voiceInfo.setStatus(Voice_info.CallStatus.REQUESTING);
        voiceInfo.setCallType(Voice_info.CallType.AUDIO_ONLY);

        // 设置UDP端口和本地主机地址
        int localPort = BASE_PORT + callId % 1000;
        String localHost = getLocalIpAddress();
        voiceInfo.setHost(localHost);
        voiceInfo.setPort(localPort);

        System.out.println("发起语音通话: " +
                "发起人=" + currentUsername +
                ", 目标=" + targetUsername +
                ", 本地主机=" + localHost +
                ", 本地端口=" + localPort);

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
        voiceInfo.setFrom_username(currentUsername); // 确保设置正确的发起人
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

        System.out.println("发起语音会议: " +
                "发起人=" + currentUsername +
                ", 参与者=" + participants.size() + "人" +
                ", 本地主机=" + localHost +
                ", 本地端口=" + localPort);

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

        // 获取通话信息
        Voice_info voiceInfo = callView.getVoiceInfo();

        try {
            // 为通话设置本地端口
            int localPort = BASE_PORT + callId % 1000;
            int maxRetries = 5;
            int retryCount = 0;
            boolean portAvailable = false;

            // 尝试找到可用端口
            while (!portAvailable && retryCount < maxRetries) {
                try {
                    // 测试端口是否可用
                    java.net.DatagramSocket testSocket = new java.net.DatagramSocket(localPort);
                    testSocket.close();
                    portAvailable = true;
                } catch (java.net.BindException e) {
                    // 端口被占用，尝试下一个端口
                    System.out.println("端口 " + localPort + " 已被占用，尝试下一个端口");
                    localPort++;
                    retryCount++;
                }
            }

            if (!portAvailable) {
                throw new java.net.BindException("无法找到可用的UDP端口");
            }

            String localHost = getLocalIpAddress();

            // 保存对方的主机信息，用于后续音频流建立
            String remoteHost = voiceInfo.getHost();
            int remotePort = voiceInfo.getPort();

            // 设置本地主机信息
            voiceInfo.setHost(localHost);
            voiceInfo.setPort(localPort);
            voiceInfo.setStatus(Voice_info.CallStatus.ACCEPTED);

            System.out.println("接受通话: " +
                    "通话ID=" + callId +
                    ", 本地主机=" + localHost +
                    ", 本地端口=" + localPort +
                    ", 远程主机=" + remoteHost +
                    ", 远程端口=" + remotePort);

            // 发送接受消息
            sendVoiceCallMessage(voiceInfo);

            // 更新UI状态
            callView.updateCallStatus(Voice_info.CallStatus.CONNECTING);

            // 创建一个临时的Voice_info对象来保存远程主机信息
            Voice_info tempInfo = new Voice_info();
            tempInfo.setCall_id(callId);
            tempInfo.setHost(remoteHost);
            tempInfo.setPort(remotePort);

            // 将临时对象存储在某个地方，以便后续使用
            // 这里我们可以创建一个映射来存储这些信息
            if (pendingRemoteEndpoints == null) {
                pendingRemoteEndpoints = new HashMap<>();
            }
            pendingRemoteEndpoints.put(callId, tempInfo);

        } catch (Exception e) {
            System.err.println("接受通话失败: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            Voice_info errorInfo = new Voice_info();
            errorInfo.setCall_id(callId);
            errorInfo.setError("接受通话失败: " + e.getMessage());
            callView.showCallError(errorInfo);
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
            System.err.println("找不到通话ID为 " + callId + " 的通话视图");
            return;
        }

        // 获取通话信息
        Voice_info voiceInfo = callView.getVoiceInfo();
        voiceInfo.setStatus(Voice_info.CallStatus.REJECTED);

        // 发送拒绝消息
        sendVoiceCallMessage(voiceInfo);

        // 关闭通话窗口
        endCall(callId);
    }

    /**
     * 结束语音通话
     * @param callId 通话ID
     */
    public void endCall(int callId) {
        // 检查是否已有该通话的视图
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView == null) {
            System.err.println("找不到通话ID为 " + callId + " 的通话视图");
            return;
        }

        // 获取通话信息
        Voice_info voiceInfo = callView.getVoiceInfo();
        voiceInfo.setStatus(Voice_info.CallStatus.ENDED);

        // 确保所有参与者都收到通话结束消息
        // 如果是一对一通话
        if (!voiceInfo.isIs_conference()) {
            // 确保参与者列表包含对方
            String currentUser = model.getCurrentUser();
            String otherParty = null;
            
            // 找出对方用户名
            if (voiceInfo.getFrom_username().equals(currentUser)) {
                // 如果当前用户是发起方，对方是参与者
                if (!voiceInfo.getParticipants().isEmpty()) {
                    otherParty = voiceInfo.getParticipants().get(0);
                }
            } else {
                // 如果当前用户是参与者，对方是发起方
                otherParty = voiceInfo.getFrom_username();
            }
            
            if (otherParty != null) {
                // 确保参与者列表包含对方
                if (!voiceInfo.getParticipants().contains(otherParty)) {
                    voiceInfo.addParticipant(otherParty);
                }
            }
        }

        // 发送结束消息（使用重试机制）
        sendEndCallMessage(voiceInfo, 3);

        // 停止音频流
        stopAudioStream(callId);

        // 关闭通话窗口
        callView.showCallEnded();
        
        // 延迟关闭窗口，给用户时间看到通话已结束的状态
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callView.dispose();
                activeCallViews.remove(callId);
            }
        }, 3000);
    }
    
    /**
     * 发送通话结束消息，带有重试机制
     * @param voiceInfo 语音信息
     * @param maxRetries 最大重试次数
     */
    private void sendEndCallMessage(Voice_info voiceInfo, int maxRetries) {
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount < maxRetries) {
            try {
                // 发送结束消息
                sendVoiceCallMessage(voiceInfo);
                success = true;
                System.out.println("成功发送通话结束消息，通话ID: " + voiceInfo.getCall_id());
            } catch (Exception e) {
                retryCount++;
                System.err.println("发送通话结束消息失败，尝试重试 (" + retryCount + "/" + maxRetries + "): " + e.getMessage());
                
                // 等待一段时间后重试
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (!success) {
            System.err.println("无法发送通话结束消息，已达到最大重试次数");
        }
    }

    /**
     * 处理收到的语音通话消息
     * @param voiceInfo 语音信息
     */
    public void handleVoiceCallMessage(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        System.out.println("收到语音通话消息: " +
                "状态=" + voiceInfo.getStatus() +
                ", 当前用户=" + currentUsername +
                ", 呼叫方=" + voiceInfo.getFrom_username() +
                ", 是否为发起方=" + isInitiator +
                ", 主机=" + voiceInfo.getHost() +
                ", 端口=" + voiceInfo.getPort());

        // 对于REQUESTING状态，检查当前用户是否为呼叫参与者而非发起者
        if (voiceInfo.getStatus() == Voice_info.CallStatus.REQUESTING) {
            if (isInitiator) {
                // 如果当前用户是呼叫发起者，且已有该通话的视图，则不重复处理
                if (activeCallViews.containsKey(callId)) {
                    System.out.println("忽略自己发送的请求消息，因为已有通话视图");
                    return;
                }
            } else {
                // 检查当前用户是否为呼叫参与者
                boolean isParticipant = false;
                for (String participant : voiceInfo.getParticipants()) {
                    if (participant.equals(currentUsername)) {
                        isParticipant = true;
                        break;
                    }
                }

                if (!isParticipant) {
                    System.out.println("忽略不相关的通话请求");
                    return;
                }
            }
        } else {
            // 对于非REQUESTING状态，检查是否有对应的通话视图
            if (!activeCallViews.containsKey(callId)) {
                System.out.println("忽略未知通话ID的消息: " + callId);
                return;
            }
        }

        switch (voiceInfo.getStatus()) {
            case REQUESTING:
                // 收到呼叫请求
                handleIncomingCall(voiceInfo);
                break;

            case ACCEPTED:
                // 通话被接受
                handleCallAccepted(voiceInfo);
                break;

            case REJECTED:
                // 通话被拒绝
                handleCallRejected(voiceInfo);
                break;

            case ENDED:
                // 通话结束
                handleCallEnded(voiceInfo);
                break;

            case CONNECTED:
                // 通话已连接
                handleCallConnected(voiceInfo);
                break;

            case ERROR:
                // 通话错误
                handleCallError(voiceInfo);
                break;

            default:
                System.err.println("未知的通话状态: " + voiceInfo.getStatus());
        }
    }

    /**
     * 处理传入的通话请求
     * @param voiceInfo 语音信息
     */
    private void handleIncomingCall(Voice_info voiceInfo) {
        // 获取当前用户名
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        System.out.println("处理通话请求: " +
                "当前用户=" + currentUsername +
                ", 呼叫方=" + voiceInfo.getFrom_username() +
                ", 是否为发起方=" + isInitiator +
                ", 参与者=" + voiceInfo.getParticipants());

        // 如果当前用户是呼叫发起方，但已经有通话视图，则不重复处理
        if (isInitiator && activeCallViews.containsKey(voiceInfo.getCall_id())) {
            System.out.println("已有通话视图，不重复处理");
            return;
        }

        // 检查主机信息是否已设置
        if (voiceInfo.getHost() == null || voiceInfo.getHost().isEmpty()) {
            System.err.println("来电请求中未包含主机信息，可能无法建立连接");

            // 记录主机信息缺失
            voiceInfo.setError("呼叫方未提供主机信息");
        }

        // 显示来电界面
        showCallView(voiceInfo);

        // 在控制台输出来电信息
        System.out.println("显示" + (isInitiator ? "呼出" : "来电") + "界面: " +
                "来自=" + voiceInfo.getFrom_username() +
                ", 主机=" + voiceInfo.getHost() +
                ", 端口=" + voiceInfo.getPort() +
                ", 会议=" + voiceInfo.isIs_conference());
    }

    /**
     * 处理通话被接受
     * @param voiceInfo 语音信息
     */
    private void handleCallAccepted(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);

        // 获取当前用户名
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        System.out.println("处理通话接受: " +
                "当前用户=" + currentUsername +
                ", 呼叫方=" + voiceInfo.getFrom_username() +
                ", 是否为发起方=" + isInitiator +
                ", 主机=" + voiceInfo.getHost() +
                ", 端口=" + voiceInfo.getPort());

        if (callView != null) {
            // 更新UI状态
            callView.updateCallStatus(Voice_info.CallStatus.CONNECTING);

            // 如果是呼叫发起方，需要获取被呼叫方的主机信息
            if (isInitiator) {
                // 检查是否已有远程主机信息
                if (voiceInfo.getHost() != null && !voiceInfo.getHost().isEmpty()) {
                    // 保存远程主机信息
                    String remoteHost = voiceInfo.getHost();
                    int remotePort = voiceInfo.getPort();
                    
                    System.out.println("保存远程主机信息: " +
                            "通话ID=" + callId +
                            ", 远程主机=" + remoteHost +
                            ", 远程端口=" + remotePort);
                    
                    // 检查音频流是否已启动
                    if (!audioStreamManagers.containsKey(callId)) {
                        // 创建一个临时的Voice_info对象，包含正确的远程主机信息
                        Voice_info tempInfo = new Voice_info();
                        tempInfo.setCall_id(callId);
                        tempInfo.setConference_id(voiceInfo.getConference_id());
                        tempInfo.setFrom_username(voiceInfo.getFrom_username());
                        tempInfo.setIs_conference(voiceInfo.isIs_conference());
                        tempInfo.setCallType(voiceInfo.getCallType());
                        tempInfo.setStatus(voiceInfo.getStatus());
                        
                        // 设置本地主机信息
                        String localHost = getLocalIpAddress();
                        int localPort = BASE_PORT + callId % 1000;
                        tempInfo.setHost(localHost);
                        tempInfo.setPort(localPort);
                        
                        // 保存远程主机信息到pendingRemoteEndpoints
                        if (pendingRemoteEndpoints == null) {
                            pendingRemoteEndpoints = new HashMap<>();
                        }
                        
                        Voice_info remoteInfo = new Voice_info();
                        remoteInfo.setCall_id(callId);
                        remoteInfo.setHost(remoteHost);
                        remoteInfo.setPort(remotePort);
                        pendingRemoteEndpoints.put(callId, remoteInfo);
                        
                        // 开始音频流，使用临时对象
                        startAudioStream(tempInfo);
                    } else {
                        System.out.println("音频流已启动，无需重复创建");
                        
                        // 更新远程主机信息
                        AudioStreamManager streamManager = audioStreamManagers.get(callId);
                        if (streamManager != null) {
                            streamManager.setRemoteEndpoint(remoteHost, remotePort);
                            System.out.println("更新音频流的远程端点: " +
                                    "通话ID=" + callId +
                                    ", 远程主机=" + remoteHost +
                                    ", 远程端口=" + remotePort);
                        }
                    }
                } else {
                    System.err.println("通话被接受，但未收到远程主机信息");
                }
            } else {
                // 如果是被呼叫方，需要设置自己的主机信息并启动音频流
                try {
                    // 检查是否有保存的远程主机信息
                    if (pendingRemoteEndpoints != null && pendingRemoteEndpoints.containsKey(callId)) {
                        Voice_info remoteInfo = pendingRemoteEndpoints.get(callId);
                        String remoteHost = remoteInfo.getHost();
                        int remotePort = remoteInfo.getPort();

                        System.out.println("使用保存的远程主机信息: " +
                                "远程主机=" + remoteHost +
                                ", 远程端口=" + remotePort);

                        // 设置远程主机信息
                        voiceInfo.setHost(remoteHost);
                        voiceInfo.setPort(remotePort);
                    }

                    // 检查音频流是否已启动
                    if (!audioStreamManagers.containsKey(callId)) {
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

                        // 获取本地主机和端口
                        String localHost = getLocalIpAddress();
                        int localPort = BASE_PORT + callId % 1000;
                        connectedInfo.setHost(localHost);
                        connectedInfo.setPort(localPort);

                        // 添加参与者
                        connectedInfo.addParticipant(voiceInfo.getFrom_username());

                        // 发送连接成功消息
                        sendVoiceCallMessage(connectedInfo);
                    } else {
                        System.out.println("音频流已启动，无需重复创建");
                    }

                } catch (Exception e) {
                    System.err.println("设置本地主机信息失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 更新通话信息
            callView.setVoiceInfo(voiceInfo);
        }
    }

    /**
     * 处理通话被拒绝
     * @param voiceInfo 语音信息
     */
    private void handleCallRejected(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);

        if (callView != null) {
            // 显示被拒绝信息
            callView.showCallRejected();

            // 延迟关闭窗口
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    callView.dispose();
                    activeCallViews.remove(callId);
                }
            }, 3000);
        }
    }

    /**
     * 处理通话结束
     * @param voiceInfo 语音信息
     */
    private void handleCallEnded(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        
        System.out.println("收到通话结束消息: 通话ID=" + callId + 
                ", 发送方=" + voiceInfo.getFrom_username() + 
                ", 参与者=" + voiceInfo.getParticipants());

        // 停止音频流
        stopAudioStream(callId);

        // 关闭通话窗口
        VoiceCallView callView = activeCallViews.get(callId);
        if (callView != null) {
            // 更新通话信息
            callView.setVoiceInfo(voiceInfo);
            callView.showCallEnded();
            
            System.out.println("通话已结束: 通话ID=" + callId);

            // 延迟关闭窗口
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    callView.dispose();
                    activeCallViews.remove(callId);
                    System.out.println("通话窗口已关闭: 通话ID=" + callId);
                }
            }, 3000);
        } else {
            System.err.println("无法找到通话ID为 " + callId + " 的通话视图，可能已关闭");
        }
        
        // 清理相关资源
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

        // 获取当前用户名
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        System.out.println("处理通话连接: " +
                "当前用户=" + currentUsername +
                ", 呼叫方=" + voiceInfo.getFrom_username() +
                ", 是否为发起方=" + isInitiator +
                ", 主机=" + voiceInfo.getHost() +
                ", 端口=" + voiceInfo.getPort());

        if (callView != null) {
            // 更新UI状态
            callView.updateCallStatus(Voice_info.CallStatus.CONNECTED);

            // 检查音频流是否已启动
            AudioStreamManager streamManager = audioStreamManagers.get(callId);
            if (streamManager == null) {
                // 如果音频流尚未启动，则启动它
                try {
                    // 如果是对方发送的CONNECTED消息，且当前用户不是发起方，则启动音频流
                    // 如果当前用户是发起方，则应该已经在ACCEPTED阶段启动了音频流
                    if (!isInitiator && !voiceInfo.getFrom_username().equals(currentUsername)) {
                        startAudioStream(voiceInfo);
                    }
                } catch (Exception e) {
                    System.err.println("启动音频流失败: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("音频流已启动，无需重复创建");
            }

            // 更新通话信息
            callView.setVoiceInfo(voiceInfo);

            // 开始计时
            callView.startDurationTimer();

            // 输出连接信息
            System.out.println("通话已连接: ID=" + callId +
                    ", 远程主机=" + voiceInfo.getHost() +
                    ", 远程端口=" + voiceInfo.getPort());
        }
    }

    /**
     * 处理通话错误
     * @param voiceInfo 语音信息
     */
    private void handleCallError(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();
        VoiceCallView callView = activeCallViews.get(callId);

        if (callView != null) {
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
            }, 3000);
        }
    }

    /**
     * 显示通话界面
     * @param voiceInfo 语音信息
     */
    private void showCallView(Voice_info voiceInfo) {
        int callId = voiceInfo.getCall_id();

        // 检查是否已有该通话的视图
        if (activeCallViews.containsKey(callId)) {
            System.out.println("通话ID为 " + callId + " 的通话视图已存在");
            return;
        }

        // 获取当前用户名
        String currentUsername = model.getCurrentUser();
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        System.out.println("创建" + (isInitiator ? "呼出" : "来电") + "界面: " +
                "通话ID=" + callId +
                ", 当前用户=" + currentUsername +
                ", 呼叫方=" + voiceInfo.getFrom_username());

        // 创建通话视图，传递当前用户名
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
            System.out.println("通话ID为 " + callId + " 的音频流已存在，无需重复创建");
            return;
        }

        try {
            // 为每个通话创建唯一的本地端口
            int localPort = BASE_PORT + callId % 1000;
            int maxRetries = 5;
            int retryCount = 0;
            boolean portAvailable = false;

            // 尝试找到可用端口
            while (!portAvailable && retryCount < maxRetries) {
                try {
                    // 测试端口是否可用
                    java.net.DatagramSocket testSocket = new java.net.DatagramSocket(localPort);
                    testSocket.close();
                    portAvailable = true;
                } catch (java.net.BindException e) {
                    // 端口被占用，尝试下一个端口
                    System.out.println("端口 " + localPort + " 已被占用，尝试下一个端口");
                    localPort++;
                    retryCount++;
                }
            }

            if (!portAvailable) {
                throw new java.net.BindException("无法找到可用的UDP端口");
            }

            // 设置本地IP地址
            String localHost = getLocalIpAddress();

            // 获取当前用户名
            String currentUsername = model.getCurrentUser();
            boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

            // 获取远程主机信息
            String remoteHost = voiceInfo.getHost();
            int remotePort = voiceInfo.getPort();

            // 检查是否需要从pendingRemoteEndpoints获取远程主机信息
            if ((remoteHost == null || remoteHost.isEmpty() || remotePort == 0) && 
                pendingRemoteEndpoints != null && pendingRemoteEndpoints.containsKey(callId)) {
                Voice_info remoteInfo = pendingRemoteEndpoints.get(callId);
                remoteHost = remoteInfo.getHost();
                remotePort = remoteInfo.getPort();
                
                System.out.println("使用保存的远程主机信息: " +
                        "通话ID=" + callId +
                        ", 远程主机=" + remoteHost +
                        ", 远程端口=" + remotePort);
            }

            System.out.println("启动音频流: " +
                    "当前用户=" + currentUsername +
                    ", 呼叫方=" + voiceInfo.getFrom_username() +
                    ", 是否为发起方=" + isInitiator +
                    ", 本地主机=" + localHost +
                    ", 本地端口=" + localPort +
                    ", 远程主机=" + remoteHost +
                    ", 远程端口=" + remotePort);

            // 检查是否已有远程主机信息
            if (remoteHost == null || remoteHost.isEmpty()) {
                System.err.println("远程主机地址未设置，无法启动音频流");
                return; // 无法启动音频流
            }

            // 创建音频流管理器
            AudioStreamManager streamManager = new AudioStreamManager(localPort);
            streamManager.initialize(); // 先初始化socket
            streamManager.setRemoteEndpoint(remoteHost, remotePort); // 然后设置远程端点

            // 注册通话ID到音频播放服务
            playbackService.registerCall(callId);

            // 设置音频数据监听器
            streamManager.addListener("default", new AudioStreamManager.AudioStreamListener() {
                @Override
                public void onAudioDataReceived(byte[] audioData) {
                    // 添加日志记录接收到的音频数据
                    System.out.println("接收到音频数据: 通话ID=" + callId + 
                                      ", 数据大小=" + audioData.length + " 字节");
                    
                    // 将接收到的音频数据发送到播放服务，并关联通话ID
                    playbackService.queueAudio(audioData, callId);
                }
            });

            // 开始接收
            streamManager.startReceiving();
            System.out.println("开始接收音频数据: 通话ID=" + callId + 
                              ", 本地端口=" + localPort);

            // 开始发送
            streamManager.startSending(null);
            System.out.println("开始发送音频数据: 通话ID=" + callId + 
                              ", 远程主机=" + remoteHost + 
                              ", 远程端口=" + remotePort);

            // 保存音频流管理器
            audioStreamManagers.put(callId, streamManager);

            // 更新UI状态
            VoiceCallView callView = activeCallViews.get(callId);
            if (callView != null) {
                callView.updateCallStatus(Voice_info.CallStatus.CONNECTED);
            }

            // 如果是发起方，发送连接成功消息
            if (isInitiator) {
                Voice_info connectedInfo = new Voice_info();
                connectedInfo.setCall_id(callId);
                connectedInfo.setConference_id(voiceInfo.getConference_id());
                connectedInfo.setFrom_username(currentUsername);
                connectedInfo.setIs_conference(voiceInfo.isIs_conference());
                connectedInfo.setCallType(voiceInfo.getCallType());
                connectedInfo.setStatus(Voice_info.CallStatus.CONNECTED);
                connectedInfo.setHost(localHost);
                connectedInfo.setPort(localPort);

                // 添加参与者
                for (String participant : voiceInfo.getParticipants()) {
                    connectedInfo.addParticipant(participant);
                }

                // 发送连接成功消息
                sendVoiceCallMessage(connectedInfo);
            }

            System.out.println("音频流启动成功: " +
                    "通话ID=" + callId +
                    ", 本地端口=" + localPort +
                    ", 远程主机=" + remoteHost +
                    ", 远程端口=" + remotePort);

        } catch (Exception e) {
            System.err.println("创建音频流失败: " + e.getMessage());
            e.printStackTrace();

            // 更新UI状态
            VoiceCallView callView = activeCallViews.get(callId);
            if (callView != null) {
                Voice_info errorInfo = new Voice_info();
                errorInfo.setCall_id(callId);
                errorInfo.setError("创建音频流失败: " + e.getMessage());
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
            // 尝试获取非回环地址
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                // 跳过禁用的接口
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    // 只考虑IPv4地址
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        System.out.println("找到本地IP地址: " + ip);
                        return ip;
                    }
                }
            }
            
            // 如果没有找到合适的地址，尝试获取本地主机地址
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            System.out.println("使用本地主机地址: " + ip);
            return ip;
        } catch (Exception e) {
            System.err.println("获取本地IP地址失败: " + e.getMessage());
            e.printStackTrace();
            System.out.println("使用默认地址: 127.0.0.1");
            return "127.0.0.1"; // 默认本地回环地址
        }
    }

    /**
     * 停止音频流
     * @param callId 通话ID
     */
    private void stopAudioStream(int callId) {
        System.out.println("停止音频流: 通话ID=" + callId);
        
        try {
            AudioStreamManager streamManager = audioStreamManagers.get(callId);
            if (streamManager != null) {
                // 关闭音频流管理器
                streamManager.close();
                audioStreamManagers.remove(callId);
                System.out.println("音频流已关闭: 通话ID=" + callId);
            } else {
                System.out.println("未找到音频流管理器: 通话ID=" + callId);
            }
            
            // 确保释放所有相关资源
            if (playbackService != null) {
                playbackService.stopPlaybackForCall(callId);
                System.out.println("已停止音频播放: 通话ID=" + callId);
            }
        } catch (Exception e) {
            System.err.println("停止音频流时发生错误: " + e.getMessage());
            e.printStackTrace();
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
