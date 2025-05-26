package io;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioIO {
    // 默认音频格式参数
    private static final float SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 2;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = true;

    // 音频数据容器
    public static class AudioData {
        private final AudioFormat format;
        private final byte[] rawData;

        public AudioData(AudioFormat format, byte[] rawData) {
            this.format = format;
            this.rawData = rawData;
        }

        public AudioFormat getFormat() {
            return format;
        }

        public byte[] getRawData() {
            return rawData;
        }
    }

    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private ByteArrayOutputStream recordingBuffer;
    private volatile boolean isRecording;

    // 获取默认音频格式
    public static AudioFormat getDefaultFormat() {
        return new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
        );
    }

    /**
     * 开始录制音频
     * @return 包含音频数据的对象
     * @throws LineUnavailableException 如果音频设备不可用
     */
    public AudioData startRecording() throws LineUnavailableException {
        AudioFormat format = getDefaultFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // 检查并打开麦克风
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("不支持当前音频格式");
        }
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        // 初始化录音缓冲区
        recordingBuffer = new ByteArrayOutputStream();
        isRecording = true;

        // 启动录音线程
        new Thread(this::captureAudio).start();

        // 返回包含当前格式的空数据对象（实时录音中持续更新）
        return new AudioData(format, new byte[0]);
    }

    /**
     * 停止录音并返回完整音频数据
     */
    public AudioData stopRecording() {
        isRecording = false;
        microphone.stop();
        microphone.close();
        return new AudioData(getDefaultFormat(), recordingBuffer.toByteArray());
    }

    // 实际录音逻辑
    private void captureAudio() {
        byte[] buffer = new byte[4096];
        while (isRecording) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                recordingBuffer.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 播放音频数据
     * @param audioData 要播放的音频数据对象
     * @throws LineUnavailableException 如果音频输出设备不可用
     */
    public void play(AudioData audioData) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(
                SourceDataLine.class,
                audioData.getFormat()
        );

        // 检查并打开扬声器
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("不支持当前音频格式");
        }
        speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(audioData.getFormat());
        speaker.start();

        // 启动播放线程
        new Thread(() -> {
            speaker.write(audioData.getRawData(), 0, audioData.getRawData().length);
            speaker.drain();
            speaker.close();
        }).start();
    }
}