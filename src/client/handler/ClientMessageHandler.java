package client.handler;

import info.encap_info;

/**
 * 客户端消息处理器接口
 * 用于处理从服务器接收到的不同类型的消息
 */
public interface ClientMessageHandler {
    /**
     * 处理消息
     * @param message 接收到的消息
     */
    void handle(encap_info message);
} 