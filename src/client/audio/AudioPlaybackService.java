package client.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;

/**
 * 音频播放服务
 * 负责播放收到的音频数据
 */
public class AudioPlaybackService {
    // 音频格式
    private final AudioFormat audioFormat;
    // 音频线路
    private SourceDataLine line;
    // 控制播放的标志
    private final AtomicBoolean running = new AtomicBoolean(false);
    // 音频数据队列
    private final BlockingQueue<byte[]> audioDataQueue = new LinkedBlockingQueue<>();
    // 单独的线程用于处理音频播放
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 通话ID到音频数据的映射，用于跟踪每个通话的音频
    private final Map<Integer, Boolean> activeCallMap = new HashMap<>();

    /**
     * 默认音频格式
     * 采样率：16kHz，16位深度，单声道，有符号，小端字节序
     */
    private static final AudioFormat DEFAULT_FORMAT = new AudioFormat(
            16000, // 采样率 Hz
            16,     // 位深度
            1,      // 声道数 (1=单声道)
            true,   // 有符号
            false   // 小端字节序
    );

    /**
     * 构造函数
     */
    public AudioPlaybackService() {
        this(DEFAULT_FORMAT);
    }

    /**
     * 构造函数
     * @param audioFormat 音频格式
     */
    public AudioPlaybackService(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    /**
     * 开始播放
     * @throws LineUnavailableException 如果音频线路不可用
     */
    public void startPlayback() throws LineUnavailableException {
        if (running.get()) {
            return; // 已经在播放
        }

        // 获取音频线路
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("不支持的音频格式: " + audioFormat);
        }

        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            System.out.println("音频播放服务启动成功: 缓冲区大小=" + line.getBufferSize() + " 字节");
        } catch (LineUnavailableException e) {
            System.err.println("音频线路不可用: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        running.set(true);

        // 在单独的线程中处理音频播放
        executor.submit(() -> {
            try {
                playAudioFromQueue();
            } catch (Exception e) {
                System.err.println("音频播放线程出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 停止播放
     */
    public void stopPlayback() {
        running.set(false);
        audioDataQueue.clear();
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }

    /**
     * 从队列播放音频数据
     */
    private void playAudioFromQueue() {
        byte[] buffer = new byte[4096]; // 4K 缓冲区
        int totalBytesPlayed = 0;
        int audioChunksPlayed = 0;
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("音频播放线程启动");
            
            while (running.get()) {
                try {
                    // 从队列获取音频数据
                    byte[] audioData = audioDataQueue.take();

                    // 添加日志记录播放的音频数据
                    totalBytesPlayed += audioData.length;
                    audioChunksPlayed++;
                    
                    // 每10个音频块记录一次，或者每5秒记录一次
                    if (audioChunksPlayed % 10 == 0 || System.currentTimeMillis() - startTime > 5000) {
                        System.out.println("正在播放音频数据: 大小=" + audioData.length + 
                                          " 字节, 累计播放=" + totalBytesPlayed + 
                                          " 字节, 块数=" + audioChunksPlayed +
                                          ", 队列大小=" + audioDataQueue.size());
                        startTime = System.currentTimeMillis();
                    }

                    // 检查线路状态，如果关闭则重新打开
                    if (line == null || !line.isOpen() || !line.isActive()) {
                        System.out.println("音频线路未打开或未激活，尝试重新打开");
                        try {
                            if (line != null) {
                                line.close();
                            }
                            
                            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                            if (!AudioSystem.isLineSupported(info)) {
                                throw new LineUnavailableException("不支持的音频格式: " + audioFormat);
                            }
                            
                            line = (SourceDataLine) AudioSystem.getLine(info);
                            line.open(audioFormat);
                            line.start();
                            System.out.println("音频线路重新打开成功");
                        } catch (Exception e) {
                            System.err.println("重新打开音频线路失败: " + e.getMessage());
                            e.printStackTrace();
                            continue; // 跳过这个音频数据
                        }
                    }

                    // 播放音频数据
                    try {
                        int written = line.write(audioData, 0, audioData.length);
                        if (written != audioData.length) {
                            System.out.println("警告: 写入的音频数据不完整，预期=" + audioData.length + 
                                              ", 实际写入=" + written);
                        }
                        
                        // 确保数据被播放出来
                        if (!line.isRunning()) {
                            line.start();
                        }
                    } catch (Exception e) {
                        System.err.println("播放音频数据时出错: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 尝试重新初始化音频线路
                        try {
                            if (line != null) {
                                line.close();
                            }
                            
                            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                            line = (SourceDataLine) AudioSystem.getLine(info);
                            line.open(audioFormat);
                            line.start();
                            System.out.println("音频线路重新初始化成功");
                        } catch (Exception e2) {
                            System.err.println("重新初始化音频线路失败: " + e2.getMessage());
                            e2.printStackTrace();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println("音频播放线程结束: 共播放 " + audioChunksPlayed + " 个音频块, " + 
                              totalBytesPlayed + " 字节");
            
        } catch (Exception e) {
            System.err.println("音频播放出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止特定通话ID的音频播放
     * @param callId 通话ID
     */
    public void stopPlaybackForCall(int callId) {
        // 将通话标记为非活动状态
        activeCallMap.remove(callId);
        System.out.println("已停止通话ID " + callId + " 的音频播放");
    }

    /**
     * 标记通话为活动状态
     * @param callId 通话ID
     */
    public void registerCall(int callId) {
        activeCallMap.put(callId, true);
        System.out.println("已注册通话ID " + callId + " 的音频播放");
    }

    /**
     * 将音频数据添加到播放队列
     * @param audioData 音频数据
     * @param callId 关联的通话ID
     */
    public void queueAudio(byte[] audioData, int callId) {
        if (running.get() && audioData != null && audioData.length > 0) {
            // 检查通话是否处于活动状态
            if (activeCallMap.containsKey(callId)) {
                // 添加日志记录收到的音频数据
                System.out.println("音频数据入队: 通话ID=" + callId + 
                                  ", 数据大小=" + audioData.length + 
                                  " 字节, 队列大小=" + audioDataQueue.size() +
                                  ", 时间戳=" + System.currentTimeMillis());
                
                // 检查队列大小，避免内存溢出
                if (audioDataQueue.size() > 100) {
                    System.out.println("警告: 音频队列过大，可能导致延迟，清除部分旧数据");
                    audioDataQueue.clear(); // 清除所有旧数据
                }
                
                audioDataQueue.offer(audioData.clone()); // 克隆数据，避免外部修改
                
                // 如果是首次接收到数据，播放一个提示音
                if (audioDataQueue.size() == 1) {
                    System.out.println("首次接收到音频数据，开始播放");
                }
            } else {
                System.out.println("丢弃音频数据: 通话ID=" + callId + " 不在活动状态");
            }
        } else if (!running.get()) {
            System.out.println("丢弃音频数据: 播放服务未运行");
        } else if (audioData == null || audioData.length == 0) {
            System.out.println("丢弃音频数据: 数据为空或长度为0");
        }
    }

    /**
     * 将音频数据添加到播放队列
     * @param audioData 音频数据
     */
    public void queueAudio(byte[] audioData) {
        if (running.get() && audioData != null && audioData.length > 0) {
            audioDataQueue.offer(audioData.clone()); // 克隆数据，避免外部修改
        }
    }

    /**
     * 立即播放音频数据
     * @param audioData 音频数据
     */
    public void playAudioImmediately(byte[] audioData) {
        if (line != null && line.isOpen() && audioData != null && audioData.length > 0) {
            line.write(audioData, 0, audioData.length);
        }
    }

    /**
     * 播放音频字节流
     * @param audioData 音频数据
     * @throws LineUnavailableException 如果音频线路不可用
     */
    public void playAudioStream(byte[] audioData) throws LineUnavailableException {
        if (audioData == null || audioData.length == 0) {
            return;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream ais = new AudioInputStream(bais, audioFormat, audioData.length / audioFormat.getFrameSize());

        try {
            AudioSystem.getAudioInputStream(audioFormat, ais);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine tempLine = (SourceDataLine) AudioSystem.getLine(info);
            tempLine.open(audioFormat);
            tempLine.start();

            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int bytesRead = 0;

            while ((bytesRead = ais.read(buffer, 0, buffer.length)) != -1) {
                tempLine.write(buffer, 0, bytesRead);
            }

            tempLine.drain();
            tempLine.close();
            ais.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        stopPlayback();
        activeCallMap.clear();
        executor.shutdown();
    }

    /**
     * 获取当前音频格式
     * @return 音频格式
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }
}

