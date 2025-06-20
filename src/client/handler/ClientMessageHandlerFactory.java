package client.handler;

import java.util.HashMap;
import java.util.Map;
import client.controller.ChatController;
import client.controller.LoginController;
import client.controller.VoiceCallController;
import client.model.ClientModel;
import info.encap_info;

/**
 * 客户端消息处理器工厂
 * 用于创建和管理不同类型的消息处理器
 */
public class ClientMessageHandlerFactory {
    private Map<Integer, ClientMessageHandler> handlers = new HashMap<>();
    
    /**
     * 初始化消息处理器工厂
     * @param model 客户端数据模型
     * @param loginController 登录控制器
     * @param chatController 聊天控制器
     * @param voiceCallController 语音通话控制器
     */
    public ClientMessageHandlerFactory(ClientModel model, LoginController loginController, 
                                      ChatController chatController, VoiceCallController voiceCallController) {
        // 注册各种消息处理器
        handlers.put(1, new GroupMessageHandler(model, chatController));
        handlers.put(2, new LogoutMessageHandler(model, loginController));
        handlers.put(3, new LoginMessageHandler(model, loginController));
        handlers.put(4, new ChatMessageHandler(model, chatController));
        handlers.put(5, new RegisterMessageHandler(loginController));
        handlers.put(6, new OrganizationMessageHandler(model, chatController));
        handlers.put(7, new VoiceCallMessageHandler(model, voiceCallController));
    }
    
    /**
     * 根据消息类型获取对应的处理器
     * @param messageType 消息类型
     * @return 消息处理器，如果类型不支持则返回null
     */
    public ClientMessageHandler getHandler(int messageType) {
        return handlers.get(messageType);
    }
} 