package Server.model;

import info.Conference_info;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有界抖动缓冲区，用于存储和排序 Conference_info 类型的音频包。
 * 考虑到多线程访问，内部使用锁进行同步。
 */
public class BoundedJitterBuffer {
    private final TreeMap<Integer, Conference_info> buffer;
    private final int capacity; // 缓冲区最大容量
    private final ReentrantLock lock = new ReentrantLock(); // 用于同步访问缓冲区

    public BoundedJitterBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new TreeMap<>(); // TreeMap 自动按键排序（即序列号）
    }

    /**
     * 将音频包添加到缓冲区。如果缓冲区满了，可能会移除最旧的包。
     * @param sequenceNumber 数据包的序列号
     * @param audioInfo 包含音频数据的 Conference_info 对象
     */
    public void addPacket(int sequenceNumber, Conference_info audioInfo) {
        lock.lock();
        try {
            buffer.put(sequenceNumber, audioInfo);
            // 简单地移除最旧的包以维持容量
            while (buffer.size() > capacity && !buffer.isEmpty()) {
                buffer.remove(buffer.firstKey());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取下一个预期序列号的数据包，并从缓冲区中移除。
     * @param expectedSequenceNumber 期望的下一个序列号
     * @return 如果找到，则返回 Conference_info 对象；否则返回 null。
     */
    public Conference_info getNextPacket(int expectedSequenceNumber) {
        lock.lock();
        try {
            if (buffer.containsKey(expectedSequenceNumber)) {
                return buffer.remove(expectedSequenceNumber);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查缓冲区是否包含某个序列号的数据包。
     * @param sequenceNumber 要检查的序列号
     * @return 如果包含则返回 true，否则返回 false。
     */
    public boolean containsPacket(int sequenceNumber) {
        lock.lock();
        try {
            return buffer.containsKey(sequenceNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区中最小的序列号（最早的包）。
     * @return 最小序列号，如果缓冲区为空则返回 -1。
     */
    public int getFirstSequenceNumber() {
        lock.lock();
        try {
            return buffer.isEmpty() ? -1 : buffer.firstKey();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区中最大的序列号（最新的包）。
     * @return 最大序列号，如果缓冲区为空则返回 -1。
     */
    public int getLastSequenceNumber() {
        lock.lock();
        try {
            return buffer.isEmpty() ? -1 : buffer.lastKey();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理缓冲区中所有小于给定序列号的旧数据包。
     * 这有助于防止缓冲区无限增长，即使数据包已播放或永久丢失。
     * @param sequenceThreshold 序列号阈值，小于此阈值的包将被移除。
     */
    public void cleanUpOldPackets(int sequenceThreshold) {
        lock.lock();
        try {
            buffer.headMap(sequenceThreshold).clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取当前缓冲区中数据包的数量。
     * @return 数据包数量。
     */
    public int size() {
        lock.lock();
        try {
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }
}