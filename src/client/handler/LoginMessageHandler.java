package client.handler;

import client.controller.LoginController;
import client.model.ClientModel;
import info.Login_info;
import info.encap_info;

import javax.swing.*;
import java.util.ArrayList;

/**
 * 客户端登录消息处理器
 */
public class LoginMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private LoginController loginController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param loginController 登录控制器
     */
    public LoginMessageHandler(ClientModel model, LoginController loginController) {
        this.model = model;
        this.loginController = loginController;
    }
    
    @Override
    public void handle(encap_info message) {
        Login_info loginInfo = message.get_login_info();
        if (loginInfo == null) return;
        if (loginInfo.isKicked()){
            JOptionPane.showMessageDialog(
                    null,                     // 父组件（null表示居中显示）
                    "您已在其他设备登录",      // 错误消息内容
                    "错误",                   // 窗口标题
                    JOptionPane.ERROR_MESSAGE // 消息类型（显示错误图标）
            );
            System.exit(0);
            return;
        }
        System.out.println("收到登录消息: " + 
            "用户名=" + loginInfo.getUserName() + 
            ", 成功标志=" + loginInfo.getLoginSuccessFlag() +
            ", 当前用户=" + model.getCurrentUser());
        
        // 更新在线用户列表 - 这个对所有登录消息都需要做
        // 添加空值检查，避免NullPointerException
        ArrayList<String> onlineUsers = loginInfo.getOnlineUsers();
        if (onlineUsers != null) {
            model.setOnlineUsers(onlineUsers);
        } else {
            // 如果在线用户列表为null，则使用空列表
            model.setOnlineUsers(new ArrayList<>());
        }
        
        // 更新所有注册用户列表
        ArrayList<String> allUsers = loginInfo.getAllUsers();
        if (allUsers != null) {
            model.setAllUsers(allUsers);
        } else {
            // 如果所有用户列表为null，则使用空列表
            model.setAllUsers(new ArrayList<>());
        }
        
        // 如果当前客户端还没有登录用户，这可能是我们的登录响应
        if (model.getCurrentUser() == null || !model.isLoggedIn()) {
            // 只有登录成功才设置当前用户
            if (loginInfo.getLoginSuccessFlag()) {
                model.setCurrentUser(loginInfo.getUserName());
                model.setLoggedIn(true);
                loginController.onLoginSuccess();
            } else {
                // 登录失败，不设置当前用户，只通知登录失败
                loginController.onLoginFailure("用户名或密码错误");
            }
        } else if (model.getCurrentUser().equals(loginInfo.getUserName())) {
            // 这是对当前用户的确认消息，只需确认登录状态
            if (loginInfo.getLoginSuccessFlag()) {
                model.setLoggedIn(true);
            }
        } else {
            // 这是其他用户的登录通知，只更新在线用户列表，不改变当前用户
            // 已经在上面更新了在线用户列表，这里无需额外操作
        }
    }
} 