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

        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        running.set(true);

        // 在单独的线程中处理音频播放
        executor.submit(() -> {
            try {
                playAudioFromQueue();
            } catch (Exception e) {
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

        try {
            while (running.get()) {
                try {
                    // 从队列获取音频数据
                    byte[] audioData = audioDataQueue.take();

                    // 播放音频数据
                    line.write(audioData, 0, audioData.length);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
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
                audioDataQueue.offer(audioData.clone()); // 克隆数据，避免外部修改
            }
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

