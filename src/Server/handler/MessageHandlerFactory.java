package Server.handler;

import java.util.HashMap;
import java.util.Map;
import Server.ChatServer;
import Server.controller.ServerController;
import Server.view.ServerWindow;

/**
 * 消息处理器工厂
 * 用于创建和管理不同类型的消息处理器
 */
public class MessageHandlerFactory {
    private Map<Integer, MessageHandler> handlers = new HashMap<>();
    
    /**
     * 初始化消息处理器工厂
     * @param controller 服务器控制器
     * @param server 聊天服务器
     * @param serverWindow 服务器窗口
     * @param currentUser 当前用户
     */
    public MessageHandlerFactory(ServerController controller, ChatServer server, ServerWindow serverWindow, String currentUser) {
        // 注册各种消息处理器
        handlers.put(1, new GroupMessageHandler(controller, server, serverWindow, currentUser));
        handlers.put(2, new LogoutMessageHandler(controller, server, serverWindow, currentUser));
        handlers.put(3, new LoginMessageHandler(controller, server, serverWindow));
        handlers.put(4, new ChatMessageHandler(controller, server, serverWindow, currentUser));
        handlers.put(5, new RegisterMessageHandler(controller, server, serverWindow));
        handlers.put(6, new OrganizationMessageHandler(controller,server,serverWindow,currentUser));
        handlers.put(7, new VoiceCallMessageHandler(controller, server, serverWindow, currentUser));
    }
    
    /**
     * 根据消息类型获取对应的处理器
     * @param messageType 消息类型
     * @return 消息处理器，如果类型不支持则返回null
     */
    public MessageHandler getHandler(int messageType) {
        return handlers.get(messageType);
    }
    
    /**
     * 更新当前用户
     * @param currentUser 当前用户名
     */
    public void updateCurrentUser(String currentUser) {
        // 更新需要用户信息的处理器
        if (handlers.get(1) instanceof GroupMessageHandler) {
            ((GroupMessageHandler) handlers.get(1)).setCurrentUser(currentUser);
        }
        if (handlers.get(2) instanceof LogoutMessageHandler) {
            ((LogoutMessageHandler) handlers.get(2)).setCurrentUser(currentUser);
        }
        if (handlers.get(4) instanceof ChatMessageHandler) {
            ((ChatMessageHandler) handlers.get(4)).setCurrentUser(currentUser);
        }
        if (handlers.get(7) instanceof VoiceCallMessageHandler) {
            ((VoiceCallMessageHandler) handlers.get(7)).setCurrentUser(currentUser);
        }
    }
} 