
package io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable; // 确保导入 Serializable
import java.net.*;

/**
 * UdpIO 类，用于在 UDP 连接上发送和接收可序列化的 Java 对象。
 * 它处理对象到字节数组的转换，并将这些字节数组封装到 UDP 数据包中。
 */
public class UdpIO {

    /**
     * 发送一个可序列化的 Java 对象作为 UDP 数据包。
     * 该方法会将 Java 对象序列化为字节数组，然后放入 DatagramPacket 并发送。
     *
     * @param socket 用于发送数据包的 DatagramSocket。
     * @param message 要发送的 Java 对象，必须实现 Serializable 接口。
     * @param targetAddress 目标主机的 IP 地址。
     * @return 如果成功发送数据包则返回 true，否则返回 false。
     */
    private static final int port = 6689;

    public static boolean sendObject(DatagramSocket socket, Serializable message,InetAddress ip) {//只要实现了可序列号的，都可以发送
        try {
            // 1. 将 Java 对象序列化为字节数组
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush(); // 确保所有对象数据都被写入 ByteArrayOutputStream
            byte[] data = bos.toByteArray();

            // 2. 将字节数组封装到 DatagramPacket 中
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);

            // 3. 通过 DatagramSocket 发送数据包
            socket.send(packet);
            return true;
        } catch (SocketException e) {
            System.err.println("UDP Socket error during send: " + e.getMessage());
            // 通常是 Socket 已关闭或无法连接
            return false;
        } catch (IOException e) {
            System.err.println("Error serializing or sending UDP packet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 接收一个 UDP 数据包并将其内容反序列化为 Java 对象。
     * 该方法会创建一个 DatagramPacket 来接收数据，然后从数据包中提取字节数组并反序列化。
     * 注意：此方法会阻塞，直到接收到一个数据包。
     *
     * @param socket 用于接收数据包的 DatagramSocket。
     * @param bufferSize 接收缓冲区的大小。应足够大以容纳最大的预期消息。
     * @return 反序列化后的 Java 对象，如果发生错误或中断则返回 null。
     */
    public static Object receiveObject(DatagramSocket socket, int bufferSize) {
        byte[] buffer = new byte[bufferSize]; // 创建一个缓冲区来接收数据
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            // 1. 接收 UDP 数据包 (此方法会阻塞)
            socket.receive(packet);

            // 2. 从数据包中提取实际接收到的数据字节数组
            // 注意：packet.getData() 返回的是整个缓冲区，packet.getLength() 才是实际数据长度
            ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            // 3. 将字节数组反序列化为 Java 对象
            return ois.readObject();

        } catch (SocketException e) {
            // Socket 可能已被关闭，通常在线程停止时发生
            System.err.println("UDP Socket error during receive: " + e.getMessage());
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error deserializing or receiving UDP packet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}