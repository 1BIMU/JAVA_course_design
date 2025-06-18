package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Chat_info;
import info.encap_info;

/**
 * 客户端聊天消息处理器
 */
public class ChatMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private ChatController chatController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param chatController 聊天控制器
     */
    public ChatMessageHandler(ClientModel model, ChatController chatController) {
        this.model = model;
        this.chatController = chatController;
    }
    
    @Override
    public void handle(encap_info message) {
        Chat_info chatInfo = message.get_chat_info();
        if (chatInfo == null) return;
        
        // 不需要在这里添加消息到历史记录，因为onNewMessage方法会做这件事
        // model.addMessage(chatInfo);
        
        // 通知聊天控制器处理新消息
        chatController.onNewMessage(chatInfo);
    }
} 