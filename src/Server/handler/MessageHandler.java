package Server.handler;

import java.io.IOException;
import java.net.Socket;
import info.encap_info;

/**
 * 消息处理器接口
 * 用于处理不同类型的消息
 */
public interface MessageHandler {
    /**
     * 处理消息
     * @param message 接收到的消息
     * @param socket 客户端连接的Socket
     * @param response 响应消息
     * @return 是否继续处理后续消息，false表示中断处理（如登出）
     * @throws IOException 如果IO操作失败
     */
    boolean handle(encap_info message, Socket socket, encap_info response) throws IOException;
} 