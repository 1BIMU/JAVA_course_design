package client.handler;

import info.encap_info;

/**
 * 客户端消息处理器接口
 * 定义了处理不同类型消息的方法
 */
public interface ClientMessageHandler {
    /**
     * 处理消息
     * @param info 封装的消息信息
     */
    void handle(encap_info info);
} 