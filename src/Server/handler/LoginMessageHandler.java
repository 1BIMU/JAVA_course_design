package Server.handler;

import java.io.IOException;
import java.net.Socket;
import Server.ChatServer;
import Server.controller.ServerController;
import Server.view.ServerWindow;
import info.encap_info;

/**
 * 登录消息处理器
 */
public class LoginMessageHandler implements MessageHandler {
    private ServerController controller;
    private ChatServer server;
    private ServerWindow serverWindow;
    
    /**
     * 构造函数
     * @param controller 服务器控制器
     * @param server 聊天服务器
     * @param serverWindow 服务器窗口
     */
    public LoginMessageHandler(ServerController controller, ChatServer server, ServerWindow serverWindow) {
        this.controller = controller;
        this.server = server;
        this.serverWindow = serverWindow;
    }
    
    @Override
    public boolean handle(encap_info message, Socket socket, encap_info response) throws IOException {
        // 调用控制器处理登录消息
        return controller.Login_handler(message, response);
    }
} 