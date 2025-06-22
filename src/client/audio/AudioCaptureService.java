package client.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频捕获服务
 * 负责从麦克风捕获音频数据
 */
public class AudioCaptureService {
    // 音频格式
    private final AudioFormat audioFormat;
    // 音频线路
    private TargetDataLine line;
    // 音频数据处理器
    private AudioDataProcessor processor;
    // 控制录音的标志
    private final AtomicBoolean running = new AtomicBoolean(false);
    // 单独的线程用于处理音频捕获
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
     * @param processor 音频数据处理器
     */
    public AudioCaptureService(AudioDataProcessor processor) {
        this(DEFAULT_FORMAT, processor);
    }

    /**
     * 构造函数
     * @param audioFormat 音频格式
     * @param processor 音频数据处理器
     */
    public AudioCaptureService(AudioFormat audioFormat, AudioDataProcessor processor) {
        this.audioFormat = audioFormat;
        this.processor = processor;
    }

    /**
     * 开始录音
     * @throws LineUnavailableException 如果音频线路不可用
     */
    public void startCapture() throws LineUnavailableException {
        if (running.get()) {
            return; // 已经在录音
        }

        // 获取音频线路
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("不支持的音频格式: " + audioFormat);
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        running.set(true);

        // 在单独的线程中处理音频捕获
        executor.submit(() -> {
            try {
                captureAndProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 停止录音
     */
    public void stopCapture() {
        running.set(false);
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    /**
     * 录音并处理音频数据
     */
    private void captureAndProcess() {
        byte[] buffer = new byte[4096]; // 4K 缓冲区
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            while (running.get()) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    // 处理音频数据
                    if (processor != null) {
                        processor.processAudioData(buffer, 0, count);
                    }

                    // 存储到输出流（可选）
                    out.write(buffer, 0, count);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置音频数据处理器
     * @param processor 处理器
     */
    public void setProcessor(AudioDataProcessor processor) {
        this.processor = processor;
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        stopCapture();
        executor.shutdown();
    }

    /**
     * 音频数据处理器接口
     */
    public interface AudioDataProcessor {
        /**
         * 处理音频数据
         * @param data 音频数据字节数组
         * @param offset 起始偏移量
         * @param length 数据长度
         */
        void processAudioData(byte[] data, int offset, int length);
    }
}
