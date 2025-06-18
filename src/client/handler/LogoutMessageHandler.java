package client.handler;

import client.controller.LoginController;
import client.model.ClientModel;
import info.encap_info;

/**
 * 客户端登出消息处理器
 */
public class LogoutMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private LoginController loginController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param loginController 登录控制器
     */
    public LogoutMessageHandler(ClientModel model, LoginController loginController) {
        this.model = model;
        this.loginController = loginController;
    }
    
    @Override
    public void handle(encap_info message) {
        // 清除模型数据
        model.clear();
        // 通知登录控制器处理登出
        loginController.onLogout();
    }
} 