package client.audio;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;

/**
 * 音频流管理器
 * 负责通过UDP发送和接收音频数据
 */
public class AudioStreamManager {
    // UDP发送接收
    private DatagramSocket socket;
    // 本地端口
    private int localPort;
    // 远程主机
    private String remoteHost;
    // 远程端口
    private int remotePort;
    // 发送控制
    private final AtomicBoolean sending = new AtomicBoolean(false);
    // 接收控制
    private final AtomicBoolean receiving = new AtomicBoolean(false);
    // 缓冲区大小
    private static final int BUFFER_SIZE = 4096;
    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // 音频流监听器列表
    private final Map<String, AudioStreamListener> listeners = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param localPort 本地端口
     */
    public AudioStreamManager(int localPort) {
        this.localPort = localPort;
    }

    /**
     * 构造函数
     * @param localPort 本地端口
     * @param remoteHost 远程主机
     * @param remotePort 远程端口
     */
    public AudioStreamManager(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    /**
     * 初始化UDP Socket
     * @throws SocketException 如果创建Socket失败
     */
    public void initialize() throws SocketException {
        if (socket == null || socket.isClosed()) {
            try {
                // 简化Socket创建过程，只使用最可靠的方式
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(localPort));
                System.out.println("UDP Socket创建成功: 本地端口=" + localPort);
            } catch (Exception e) {
                System.err.println("UDP Socket创建失败: " + e.getMessage());
                // 备用方案
                socket = new DatagramSocket(localPort);
            }
            
            socket.setSoTimeout(100);
            
            // 简化Socket配置
            socket.setBroadcast(true);
            socket.setReceiveBufferSize(65536);
            socket.setSendBufferSize(65536);
            socket.setTrafficClass(0x10); // 低延迟
            
            System.out.println("UDP Socket就绪: 本地端口=" + localPort);
        }
    }

    /**
     * 设置远程端点
     * @param host 远程主机
     * @param port 远程端口
     */
    public void setRemoteEndpoint(String host, int port) {
        this.remoteHost = host;
        this.remotePort = port;
    }

    /**
     * 发送音频数据
     * @param audioData 音频数据
     * @param offset 偏移量
     * @param length 长度
     * @throws IOException 如果发送失败
     */
    public void sendAudioData(byte[] audioData, int offset, int length) throws IOException {
        if (socket == null || socket.isClosed()) {
            System.err.println("发送音频数据失败: Socket未初始化或已关闭");
            throw new IOException("Socket未初始化或已关闭");
        }

        if (remoteHost == null || remoteHost.isEmpty()) {
            System.err.println("发送音频数据失败: 未设置远程主机地址");
            throw new IOException("Socket未初始化或未设置远程主机");
        }

        try {
            // 尝试多种方式解析远程主机地址
            InetAddress remoteAddress = null;
            try {
                // 方法1: 直接解析
                remoteAddress = InetAddress.getByName(remoteHost);
                System.out.println("远程地址解析成功(直接解析): " + remoteAddress.getHostAddress());
            } catch (Exception e) {
                System.err.println("远程地址解析失败(直接解析): " + e.getMessage());
                
                try {
                    // 方法2: 使用主机名
                    remoteAddress = InetAddress.getByName(remoteHost);
                    System.out.println("远程地址解析成功(使用主机名): " + remoteAddress.getHostAddress());
                } catch (Exception e2) {
                    System.err.println("远程地址解析失败(使用主机名): " + e2.getMessage());
                    
                    // 方法3: 直接使用IP地址
                    if (remoteHost.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        String[] parts = remoteHost.split("\\.");
                        byte[] addr = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            addr[i] = (byte) Integer.parseInt(parts[i]);
                        }
                        remoteAddress = InetAddress.getByAddress(addr);
                        System.out.println("远程地址解析成功(直接使用IP地址): " + remoteAddress.getHostAddress());
                    } else {
                        throw new IOException("无法解析远程主机地址: " + remoteHost);
                    }
                }
            }
            
            // 创建数据包
            DatagramPacket packet = new DatagramPacket(
                    audioData, offset, length,
                    remoteAddress,
                    remotePort
            );

            // 发送数据包
            socket.send(packet);
            
            // 记录发送情况
            System.out.println("发送音频数据: 大小=" + length + " 字节, 目标=" + 
                              remoteAddress.getHostAddress() + ":" + remotePort);
            
        } catch (UnknownHostException e) {
            System.err.println("发送音频数据失败: 无法解析远程主机地址 " + remoteHost + ": " + e.getMessage());
            e.printStackTrace();
            throw new IOException("无法解析远程主机地址: " + remoteHost, e);
        } catch (IOException e) {
            System.err.println("发送音频数据失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 开始发送音频数据
     * @param processor 音频数据源
     * @throws IOException 如果发送失败
     */
    public void startSending(AudioCaptureService.AudioDataProcessor processor) throws IOException {
        if (sending.get()) {
            return; // 已经在发送
        }

        // 检查远程主机信息是否已设置
        if (remoteHost == null || remoteHost.isEmpty()) {
            System.err.println("无法开始发送音频数据: 未设置远程主机地址");
            throw new IOException("未设置远程主机地址");
        }

        // 确保Socket已初始化
        if (socket == null || socket.isClosed()) {
            initialize();
        }

        System.out.println("开始发送音频数据到 " + remoteHost + ":" + remotePort);
        sending.set(true);
        
        // 发送连接建立包
        sendConnectPacket();

        executorService.submit(() -> {
            try {
                AudioCaptureService captureService = new AudioCaptureService(new AudioCaptureService.AudioDataProcessor() {
                    @Override
                    public void processAudioData(byte[] data, int offset, int length) {
                        if (sending.get()) {
                            try {
                                sendAudioData(data, offset, length);

                                // 同时转发给processor进行其他处理（如果有的话）
                                if (processor != null) {
                                    processor.processAudioData(data, offset, length);
                                }
                            } catch (IOException e) {
                                System.err.println("发送音频数据失败: " + e.getMessage());
                            }
                        }
                    }
                });

                try {
                    captureService.startCapture();

                    // 简化心跳机制，只保留基本保活功能
                    long lastKeepAliveTime = 0;
                    
                    while (sending.get()) {
                        try {
                            long now = System.currentTimeMillis();
                            if (now - lastKeepAliveTime > 30000) { // 每30秒
                                sendKeepAlivePacket();
                                lastKeepAliveTime = now;
                            }
                            
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            System.err.println("发送保活包失败: " + e.getMessage());
                        }
                    }
                } finally {
                    captureService.stopCapture();
                    captureService.shutdown();
                }

            } catch (Exception e) {
                System.err.println("音频捕获失败: " + e.getMessage());
                sending.set(false);
            }
        });
    }

    /**
     * 停止发送音频数据
     */
    public void stopSending() {
        sending.set(false);
    }

    /**
     * 开始接收音频数据
     */
    public void startReceiving() {
        if (receiving.get()) {
            return; // 已经在接收
        }

        receiving.set(true);
        System.out.println("开始接收音频数据: 本地端口=" + localPort);

        executorService.submit(() -> {
            try {
                // 确保socket已初始化
                if (socket == null || socket.isClosed()) {
                    initialize();
                }
                
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                int packetsReceived = 0;
                int totalBytesReceived = 0;
                long startTime = System.currentTimeMillis();
                long lastReceiveTime = System.currentTimeMillis();
                boolean receivedAny = false;
                
                // 用于动态发现和记录远程端点
                Map<String, Integer> remoteEndpointHits = new HashMap<>();
                String currentRemoteHost = remoteHost;
                int currentRemotePort = remotePort;

                while (receiving.get()) {
                    try {
                        // 检查socket是否仍然有效
                        if (socket == null || socket.isClosed()) {
                            initialize();
                        }
                        
                        socket.setSoTimeout(1000);
                        
                        try {
                            // 接收数据包
                            socket.receive(packet);
                            lastReceiveTime = System.currentTimeMillis();
                            receivedAny = true;
                            
                            // 获取发送方地址和端口
                            String senderHost = packet.getAddress().getHostAddress();
                            int senderPort = packet.getPort();
                            
                            // 记录远程端点命中次数，用于动态发现最佳端点
                            String endpointKey = senderHost + ":" + senderPort;
                            remoteEndpointHits.put(endpointKey, 
                                remoteEndpointHits.getOrDefault(endpointKey, 0) + 1);
                            
                            // 如果收到的包数超过一定阈值，检查是否需要切换远程端点
                            if (packetsReceived % 20 == 0 && !remoteEndpointHits.isEmpty()) {
                                // 找出命中次数最多的端点
                                String bestEndpoint = null;
                                int maxHits = 0;
                                
                                for (Map.Entry<String, Integer> entry : remoteEndpointHits.entrySet()) {
                                    if (entry.getValue() > maxHits) {
                                        maxHits = entry.getValue();
                                        bestEndpoint = entry.getKey();
                                    }
                                }
                                
                                if (bestEndpoint != null) {
                                    String[] parts = bestEndpoint.split(":");
                                    String bestHost = parts[0];
                                    int bestPort = Integer.parseInt(parts[1]);
                                    
                                    // 如果最佳端点与当前不同，切换远程端点
                                    if (!bestHost.equals(currentRemoteHost) || bestPort != currentRemotePort) {
                                        System.out.println("动态切换远程端点到: " + bestEndpoint + 
                                                         " (命中率: " + maxHits + ")");
                                        remoteHost = bestHost;
                                        remotePort = bestPort;
                                        currentRemoteHost = bestHost;
                                        currentRemotePort = bestPort;
                                    }
                                }
                            }
                            
                            // 如果远程主机地址未设置，则使用第一个接收到的包的源地址和端口
                            if (remoteHost == null || remoteHost.isEmpty() || remotePort == 0) {
                                remoteHost = senderHost;
                                remotePort = senderPort;
                                currentRemoteHost = senderHost;
                                currentRemotePort = senderPort;
                                System.out.println("自动设置远程主机: " + remoteHost + ":" + remotePort);
                            }
                            
                            // 检查数据包大小
                            int packetLength = packet.getLength();
                            
                            // 处理控制包
                            if (packetLength == 4) {
                                byte[] controlData = Arrays.copyOf(packet.getData(), 4);
                                int controlType = java.nio.ByteBuffer.wrap(controlData).getInt();
                                
                                // 控制包类型
                                if (controlType == 0x434F4E4E) { // "CONN"的ASCII码
                                    System.out.println("收到连接包，建立连接: " + senderHost + ":" + senderPort);
                                    sendConnectAckPacket(packet.getAddress(), packet.getPort());
                                    
                                    // 更新远程端点为发送连接包的地址
                                    remoteHost = senderHost;
                                    remotePort = senderPort;
                                    currentRemoteHost = senderHost;
                                    currentRemotePort = senderPort;
                                    
                                    continue;
                                } else if (controlType == 0x4B414C56) { // "KALV"的ASCII码
                                    // 收到保活包，只记录日志
                                    System.out.println("收到保活包");
                                    continue;
                                } else if (controlType == 0x54455354) { // "TEST"的ASCII码
                                    // 收到测试包，回复确认
                                    System.out.println("收到测试包，回复确认: " + senderHost + ":" + senderPort);
                                    sendTestAckPacket(packet.getAddress(), packet.getPort());
                                    
                                    // 更新远程端点为发送测试包的地址
                                    remoteHost = senderHost;
                                    remotePort = senderPort;
                                    currentRemoteHost = senderHost;
                                    currentRemotePort = senderPort;
                                    
                                    continue;
                                }
                            } else if (packetLength == 4 && new String(Arrays.copyOf(packet.getData(), 4)).equals("TEST")) {
                                // 兼容文本格式的测试包
                                System.out.println("收到文本测试包，回复确认: " + senderHost + ":" + senderPort);
                                sendTestAckPacket(packet.getAddress(), packet.getPort());
                                
                                // 更新远程端点为发送测试包的地址
                                remoteHost = senderHost;
                                remotePort = senderPort;
                                currentRemoteHost = senderHost;
                                currentRemotePort = senderPort;
                                
                                continue;
                            }
                            
                            // 检查数据长度是否符合音频帧要求(2字节的整数倍)
                            if (packetLength % 2 != 0) {
                                System.out.println("警告: 收到非整数帧的音频数据，跳过处理");
                                continue;
                            }
                            
                            // 记录接收到的数据包
                            packetsReceived++;
                            totalBytesReceived += packetLength;
                            
                            // 减少日志输出频率，只在必要时记录
                            if (packetsReceived % 50 == 0) {
                                System.out.println("已接收音频数据: " + packetsReceived + "个包, " + 
                                                  totalBytesReceived + "字节");
                            }

                            // 创建数据副本
                            byte[] audioData = Arrays.copyOf(packet.getData(), packetLength);

                            // 通知所有监听器
                            for (AudioStreamListener listener : listeners.values()) {
                                listener.onAudioDataReceived(audioData);
                            }
                        } catch (SocketTimeoutException e) {
                            // 超时，检查是否长时间未收到数据
                            long now = System.currentTimeMillis();
                            
                            // 如果已收到数据但长时间未接收，尝试发送保活包
                            if (receivedAny && now - lastReceiveTime > 15000) {
                                System.out.println("已超过15秒未收到音频数据，发送保活包");
                                if (remoteHost != null && !remoteHost.isEmpty()) {
                                    try {
                                        sendConnectPacket();
                                    } catch (Exception e2) {
                                        System.err.println("发送保活包失败");
                                    }
                                }
                            }
                        }
                        
                        // 重置包大小，准备下一次接收
                        packet.setLength(buffer.length);

                    } catch (Exception e) {
                        if (receiving.get()) {
                            System.err.println("接收音频数据时出错: " + e.getMessage());
                            // 避免频繁错误日志
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                
                System.out.println("接收线程结束: 共接收 " + packetsReceived + " 个包");
                
            } catch (Exception e) {
                System.err.println("音频接收线程出错: " + e.getMessage());
            }
        });
    }

    /**
     * 停止接收音频数据
     */
    public void stopReceiving() {
        receiving.set(false);
    }

    /**
     * 添加音频流监听器
     * @param id 监听器ID
     * @param listener 监听器
     */
    public void addListener(String id, AudioStreamListener listener) {
        listeners.put(id, listener);
    }

    /**
     * 移除音频流监听器
     * @param id 监听器ID
     */
    public void removeListener(String id) {
        listeners.remove(id);
    }

    /**
     * 关闭管理器
     */
    public void close() {
        stopSending();
        stopReceiving();

        executorService.shutdown();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * 音频流监听器接口
     */
    public interface AudioStreamListener {
        /**
         * 当接收到音频数据时调用
         * @param audioData 接收到的音频数据
         */
        void onAudioDataReceived(byte[] audioData);
    }

    /**
     * 发送连接包，用于建立连接和NAT穿透
     * @throws IOException 如果发送失败
     */
    private void sendConnectPacket() throws IOException {
        if (socket == null || socket.isClosed() || remoteHost == null || remoteHost.isEmpty()) {
            return;
        }

        try {
            // 解析远程主机地址
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            
            // 创建一个4字节的控制包，ASCII码为"CONN"
            byte[] connectData = new byte[4];
            java.nio.ByteBuffer.wrap(connectData).putInt(0x434F4E4E); // "CONN"
            
            // 创建数据包
            DatagramPacket packet = new DatagramPacket(
                    connectData, connectData.length,
                    remoteAddress,
                    remotePort
            );

            // 发送数据包
            socket.send(packet);
            System.out.println("发送连接包: " + remoteHost + ":" + remotePort);
            
        } catch (Exception e) {
            System.err.println("发送连接包失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送连接确认包
     * @param address 目标地址
     * @param port 目标端口
     * @throws IOException 如果发送失败
     */
    private void sendConnectAckPacket(InetAddress address, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            // 创建一个4字节的控制包，ASCII码为"CACK"
            byte[] ackData = new byte[4];
            java.nio.ByteBuffer.wrap(ackData).putInt(0x4341434B); // "CACK"
            
            // 创建数据包
            DatagramPacket packet = new DatagramPacket(
                    ackData, ackData.length,
                    address,
                    port
            );

            // 发送数据包
            socket.send(packet);
            System.out.println("发送连接确认包: " + address.getHostAddress() + ":" + port);
            
        } catch (Exception e) {
            System.err.println("发送连接确认包失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送保活包
     * @throws IOException 如果发送失败
     */
    private void sendKeepAlivePacket() throws IOException {
        if (socket == null || socket.isClosed() || remoteHost == null || remoteHost.isEmpty()) {
            return;
        }

        try {
            // 解析远程主机地址
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            
            // 创建一个4字节的控制包，ASCII码为"KALV"
            byte[] keepAliveData = new byte[4];
            java.nio.ByteBuffer.wrap(keepAliveData).putInt(0x4B414C56); // "KALV"
            
            // 创建数据包
            DatagramPacket packet = new DatagramPacket(
                    keepAliveData, keepAliveData.length,
                    remoteAddress,
                    remotePort
            );

            // 发送数据包
            socket.send(packet);
            System.out.println("发送保活包: " + remoteHost + ":" + remotePort);
            
        } catch (Exception e) {
            System.err.println("发送保活包失败: " + e.getMessage());
        }
    }

    /**
     * 发送测试确认包
     * @param address 目标地址
     * @param port 目标端口
     * @throws IOException 如果发送失败
     */
    private void sendTestAckPacket(InetAddress address, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            // 创建一个4字节的控制包，ASCII码为"TACK"
            byte[] ackData = new byte[4];
            java.nio.ByteBuffer.wrap(ackData).putInt(0x5441434B); // "TACK"
            
            // 创建数据包
            DatagramPacket packet = new DatagramPacket(
                    ackData, ackData.length,
                    address,
                    port
            );

            // 发送数据包
            socket.send(packet);
            System.out.println("发送测试确认包: " + address.getHostAddress() + ":" + port);
            
        } catch (Exception e) {
            System.err.println("发送测试确认包失败: " + e.getMessage());
        }
    }
}