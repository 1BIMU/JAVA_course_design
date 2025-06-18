package client.handler;

import client.controller.LoginController;
import info.Reg_info;
import info.encap_info;

/**
 * 客户端注册消息处理器
 */
public class RegisterMessageHandler implements ClientMessageHandler {
    private LoginController loginController;
    
    /**
     * 构造函数
     * @param loginController 登录控制器
     */
    public RegisterMessageHandler(LoginController loginController) {
        this.loginController = loginController;
    }
    
    @Override
    public void handle(encap_info message) {
        Reg_info regInfo = message.get_reg_info();
        if (regInfo == null) return;
        
        switch (regInfo.getReg_status()) {
            case 1: // 注册成功
                loginController.onRegisterSuccess(regInfo.getUsername());
                break;
            case 2: // 注册失败
                loginController.onRegisterFailure("用户名已存在");
                break;
            default:
                loginController.onRegisterFailure("未知错误");
        }
    }
} 