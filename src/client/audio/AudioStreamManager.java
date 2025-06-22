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
            socket = new DatagramSocket(localPort);
            socket.setSoTimeout(100); // 设置超时，以便能及时关闭
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
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            DatagramPacket packet = new DatagramPacket(
                    audioData, offset, length,
                    remoteAddress,
                    remotePort
            );

            socket.send(packet);
        } catch (UnknownHostException e) {
            System.err.println("发送音频数据失败: 无法解析远程主机地址 " + remoteHost + ": " + e.getMessage());
            throw new IOException("无法解析远程主机地址: " + remoteHost, e);
        } catch (IOException e) {
            System.err.println("发送音频数据失败: " + e.getMessage());
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

        executorService.submit(() -> {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (receiving.get()) {
                    try {
                        socket.receive(packet);

                        // 创建数据副本
                        byte[] audioData = Arrays.copyOf(packet.getData(), packet.getLength());

                        // 通知所有监听器
                        for (AudioStreamListener listener : listeners.values()) {
                            listener.onAudioDataReceived(audioData);
                        }

                        // 重置包大小，准备下一次接收
                        packet.setLength(buffer.length);

                    } catch (SocketTimeoutException e) {
                        // 超时，继续循环
                    } catch (Exception e) {
                        if (receiving.get()) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
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