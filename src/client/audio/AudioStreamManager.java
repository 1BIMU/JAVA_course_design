package client.audio;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
                // 尝试方法1: 使用标准构造函数
                socket = new DatagramSocket(null); // 先不绑定
                socket.setReuseAddress(true); // 允许地址重用
                socket.bind(new InetSocketAddress(localPort)); // 然后绑定
                System.out.println("Socket创建成功(方法1): 本地端口=" + localPort);
            } catch (Exception e) {
                System.err.println("Socket创建失败(方法1): " + e.getMessage());
                
                try {
                    // 尝试方法2: 直接指定端口
                    socket = new DatagramSocket(localPort);
                    System.out.println("Socket创建成功(方法2): 本地端口=" + localPort);
                } catch (Exception e2) {
                    System.err.println("Socket创建失败(方法2): " + e2.getMessage());
                    
                    try {
                        // 尝试方法3: 使用通配地址
                        socket = new DatagramSocket(localPort, InetAddress.getByName("0.0.0.0"));
                        System.out.println("Socket创建成功(方法3): 本地端口=" + localPort);
                    } catch (UnknownHostException uhe) {
                        System.err.println("Socket创建失败(方法3): 无法解析通配地址: " + uhe.getMessage());
                        // 尝试方法4: 不指定地址
                        socket = new DatagramSocket(localPort);
                        System.out.println("Socket创建成功(方法4): 本地端口=" + localPort);
                    }
                }
            }
            
            socket.setSoTimeout(100); // 设置超时，以便能及时关闭
            
            // 增加配置以提高可靠性
            try {
                // 允许广播
                socket.setBroadcast(true);
                
                // 设置更大的发送和接收缓冲区
                socket.setReceiveBufferSize(65536); // 64KB
                socket.setSendBufferSize(65536); // 64KB
                
                // 设置性能选项
                socket.setTrafficClass(0x10); // 低延迟
                
                // 禁用回环模式检查
                try {
                    java.lang.reflect.Field field = socket.getClass().getDeclaredField("socket");
                    field.setAccessible(true);
                    Object socketImpl = field.get(socket);
                    
                    java.lang.reflect.Method setOption = socketImpl.getClass().getDeclaredMethod(
                            "setOption", int.class, Object.class);
                    setOption.setAccessible(true);
                    
                    // 禁用回环模式检查 (IP_MULTICAST_LOOP = 18)
                    setOption.invoke(socketImpl, 18, false);
                    System.out.println("禁用回环模式检查成功");
                } catch (Exception e) {
                    System.err.println("禁用回环模式检查失败: " + e.getMessage());
                }
                
                System.out.println("UDP Socket配置: " +
                                  "本地端口=" + localPort + 
                                  ", 接收缓冲区=" + socket.getReceiveBufferSize() + 
                                  ", 发送缓冲区=" + socket.getSendBufferSize() + 
                                  ", 广播=" + socket.getBroadcast() + 
                                  ", 地址重用=" + socket.getReuseAddress());
                
                // 测试Socket是否正常工作
                try {
                    byte[] testData = new byte[10];
                    DatagramPacket testPacket;
                    try {
                        testPacket = new DatagramPacket(
                                testData, testData.length,
                                InetAddress.getByName("127.0.0.1"),
                                localPort
                        );
                        socket.send(testPacket);
                        System.out.println("Socket测试发送成功");
                    } catch (UnknownHostException uhe) {
                        System.err.println("Socket测试发送失败: 无法解析本地回环地址: " + uhe.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Socket测试发送失败: " + e.getMessage());
                }
                
            } catch (Exception e) {
                System.err.println("设置Socket选项失败，但将继续使用: " + e.getMessage());
            }
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
                System.out.println("远程地址解析成功(方法1): " + remoteAddress.getHostAddress());
            } catch (Exception e) {
                System.err.println("远程地址解析失败(方法1): " + e.getMessage());
                
                try {
                    // 方法2: 使用主机名
                    remoteAddress = InetAddress.getByName(remoteHost);
                    System.out.println("远程地址解析成功(方法2): " + remoteAddress.getHostAddress());
                } catch (Exception e2) {
                    System.err.println("远程地址解析失败(方法2): " + e2.getMessage());
                    
                    // 方法3: 直接使用IP地址
                    if (remoteHost.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        String[] parts = remoteHost.split("\\.");
                        byte[] addr = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            addr[i] = (byte) Integer.parseInt(parts[i]);
                        }
                        remoteAddress = InetAddress.getByAddress(addr);
                        System.out.println("远程地址解析成功(方法3): " + remoteAddress.getHostAddress());
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
            System.err.println("无法开始发送音频数据: Socket未初始化或已关闭");
            initialize();
        }

        System.out.println("开始发送音频数据到 " + remoteHost + ":" + remotePort);
        sending.set(true);

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
                                if (sending.get()) {
                                    // 如果仍然在发送状态，则记录错误但不停止捕获
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });

                try {
                    captureService.startCapture();

                    // 持续发送直到停止标志设置为false
                    while (sending.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    captureService.stopCapture();
                    captureService.shutdown();
                }

            } catch (Exception e) {
                System.err.println("音频捕获失败: " + e.getMessage());
                e.printStackTrace();
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
                    try {
                        System.out.println("接收线程中重新初始化socket");
                        initialize();
                    } catch (Exception e) {
                        System.err.println("接收线程中初始化socket失败: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                int packetsReceived = 0;
                int totalBytesReceived = 0;
                long startTime = System.currentTimeMillis();
                long lastReceiveTime = System.currentTimeMillis();
                boolean receivedAny = false;

                while (receiving.get()) {
                    try {
                        // 检查socket是否仍然有效
                        if (socket == null || socket.isClosed()) {
                            System.err.println("接收线程中发现socket已关闭，尝试重新初始化");
                            try {
                                initialize();
                            } catch (Exception e) {
                                System.err.println("接收线程中重新初始化socket失败: " + e.getMessage());
                                break;
                            }
                        }
                        
                        // 设置超时
                        socket.setSoTimeout(1000);
                        
                        // 接收数据包
                        socket.receive(packet);
                        lastReceiveTime = System.currentTimeMillis();
                        receivedAny = true;
                        
                        // 记录接收到的数据包
                        packetsReceived++;
                        totalBytesReceived += packet.getLength();
                        
                        // 每接收10个包或每5秒记录一次统计信息
                        if (packetsReceived % 10 == 0 || System.currentTimeMillis() - startTime > 5000) {
                            System.out.println("UDP接收统计: 收到包数=" + packetsReceived + 
                                              ", 总字节数=" + totalBytesReceived +
                                              ", 来源=" + packet.getAddress().getHostAddress() + 
                                              ":" + packet.getPort());
                            startTime = System.currentTimeMillis();
                        }

                        // 创建数据副本
                        byte[] audioData = Arrays.copyOf(packet.getData(), packet.getLength());

                        // 通知所有监听器
                        for (AudioStreamListener listener : listeners.values()) {
                            listener.onAudioDataReceived(audioData);
                        }

                        // 重置包大小，准备下一次接收
                        packet.setLength(buffer.length);

                    } catch (SocketTimeoutException e) {
                        // 超时，检查是否长时间未收到数据
                        long now = System.currentTimeMillis();
                        if (receivedAny && now - lastReceiveTime > 10000) { // 10秒未收到数据
                            System.out.println("警告: 已超过10秒未收到音频数据");
                        }
                    } catch (Exception e) {
                        if (receiving.get()) {
                            System.err.println("接收音频数据时出错: " + e.getMessage());
                            e.printStackTrace();
                            
                            // 短暂暂停，避免CPU占用过高
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                
                System.out.println("接收线程结束: 共接收 " + packetsReceived + " 个包, " + 
                                  totalBytesReceived + " 字节");
                
            } catch (Exception e) {
                System.err.println("音频接收线程出错: " + e.getMessage());
                e.printStackTrace();
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
}