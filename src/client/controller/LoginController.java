package client.controller;

import java.io.IOException;
import java.net.Socket;

import client.MessageSender;
import client.model.ClientModel;
import client.view.LoginView;
import info.Login_info;
import info.Reg_info;
import info.encap_info;
import io.IOStream;

/*
    登录控制器，处理用户登录和注册相关的业务逻辑
*/
public class LoginController {
    private ClientModel model;
    private LoginView loginView;
    private LoginCallback loginCallback;
    private MessageSender messageSender;

    /*
        构造函数
    */
    public LoginController(ClientModel model, MessageSender messageSender) {
        this.model = model;
        this.messageSender = messageSender;
    }

    /*
        设置登录视图
    */
    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }
    
    /*
        设置登录回调
    */
    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }

    /*
        显示登录视图
    */
    public void showLoginView() {
        if (loginView == null) {
            loginView = new LoginView(this);
        }
        loginView.setVisible(true);
    }

    /*
        处理用户登录请求
    */
    public void login(String username, String password) {
        // 输入验证
        if (username == null || username.trim().isEmpty()) {
            loginView.showError("用户名不能为空");
            return;
        }
        if (password == null || password.isEmpty()) {
            loginView.showError("密码不能为空");
            return;
        }

        // 预设用户名，但标记为未登录
        model.setCurrentUser(username);
        model.setLoggedIn(false);
        
        // 使用MessageSender发送登录请求
        boolean success = messageSender.sendLoginRequest(username, password);
        
        if (success) {
            loginView.showLoginInProgress();
        } else {
            loginView.showError("连接服务器失败");
        }
    }

    /*
        处理用户注册请求
    */
    public void register(String username, String password, String confirmPassword) {
        // 输入验证
        if (username == null || username.trim().isEmpty()) {
            loginView.showError("用户名不能为空");
            return;
        }
        if (password == null || password.isEmpty()) {
            loginView.showError("密码不能为空");
            return;
        }
        
        // 新加了重复输入密码功能
        if (!password.equals(confirmPassword)) {
            loginView.showError("两次输入的密码不一致");
            return;
        }
        
        // 使用MessageSender发送注册请求
        boolean success = messageSender.sendRegisterRequest(username, password);
        
        if (success) {
            loginView.showRegisterInProgress();
        } else {
            loginView.showError("连接服务器失败");
        }
    }

    /*
        登录成功回调
    */
    public void onLoginSuccess() {
        // 添加一个标志，防止重复处理登录成功事件
        if (loginView != null && loginView.isVisible()) {
            // 隐藏登录视图
            loginView.dispose();
            
            // 通知Client类处理登录成功事件
            if (loginCallback != null) {
                loginCallback.onLoginSuccess();
            }
        }
    }

    /*
        登录失败回调
    */
    public void onLoginFailure(String errorMessage) {
        if (loginView != null) {
            loginView.showError(errorMessage);
            loginView.resetLoginForm();
        }
    }

    /*
        注册成功回调
    */
    public void onRegisterSuccess(String username) {
        if (loginView != null) {
            loginView.showMessage("注册成功，请使用新账号登录");
            loginView.switchToLoginPanel();
            loginView.setUsername(username);
        }
    }

    /*
        注册失败回调
    */
    public void onRegisterFailure(String errorMessage) {
        if (loginView != null) {
            loginView.showError(errorMessage);
            loginView.resetRegisterForm();
        }
    }

    /*
        登出回调
    */
    public void onLogout() {
        // 清除模型数据（已在MessageListener中处理）
        
        // 显示登录视图
        showLoginView();
    }
    
    /*
        切换到注册面板
    */
    public void switchToRegister() {
        if (loginView != null) {
            loginView.switchToRegisterPanel();
        }
    }
    
    /*
        切换到登录面板
    */
    public void switchToLogin() {
        if (loginView != null) {
            loginView.switchToLoginPanel();
        }
    }

    /*
        登录回调接口，用于通知Client类处理登录相关事件
    */
    public interface LoginCallback {
        /*
            登录成功回调
        */
        void onLoginSuccess();
        
        /*
            登出回调
        */
        void onLogout();
    }
} 