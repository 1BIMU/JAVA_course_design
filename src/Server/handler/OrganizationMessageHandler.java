package Server.handler;

import Server.ChatServer;
import Server.controller.ServerController;
import Server.view.ServerWindow;
import info.encap_info;

import java.io.IOException;
import java.net.Socket;

public class OrganizationMessageHandler implements MessageHandler {
    private ServerController controller;
    private ChatServer server;
    private ServerWindow serverWindow;
    private String currentUser;

    /**
     * 构造函数
     * @param controller 服务器控制器
     * @param server 聊天服务器
     * @param serverWindow 服务器窗口
     * @param currentUser 当前用户
     */
    public OrganizationMessageHandler(ServerController controller, ChatServer server, ServerWindow serverWindow,String currentUser) {
        this.controller = controller;
        this.server = server;
        this.serverWindow = serverWindow;
        this.currentUser = currentUser;
    }

    @Override
    public boolean handle(encap_info message, Socket socket, encap_info response) throws IOException {
        // 调用控制器处理聊天消息
        controller.Org_handler(message, response);
        // 聊天消息处理后继续处理后续消息
        return true;
    }

}
