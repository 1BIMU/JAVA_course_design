package client;

import java.io.IOException;
import java.net.Socket;

import client.controller.ChatController;
import client.controller.LoginController;
import client.model.ClientModel;
import info.Chat_info;
import info.Group_info;
import info.Login_info;
import info.Reg_info;
import info.encap_info;
import io.IOStream;

import javax.swing.*;

/*
    消息监听器，负责接收和处理后端，即服务器发送的消息
*/

/*
    给服务端的说明：
        该监听器其实就是之前的的ClientHandler
        作为独立线程运行，持续监听Socket输入流
        重构是为了更好的把 UI 和消息处理逻辑分离
        之前是 ClientHandler 在处理消息的同时，还负责UI的更新，
        UI 现在只负责前端显示，不负责消息处理
*/

/* 
    消息分发机制：
        和原来的 ClientHandler 一样：
            * 根据消息类型（info.get_type()）将消息分发给不同的处理方法
            * 支持处理多种消息类型：登录(3)、聊天(4)、群组(1)、注册(5)、登出(2)
*/




/* 
    一些边写边学的记录：
        1. 使用 Thread 类创建一个独立线程，持续监听 Socket 输入流：
            * 其中 Thread 类是 Java 中的一个类，用于创建和管理线程
                * 通过继承 Thread 类并重写 run() 方法，可以创建一个自定义的线程类
            * Thread 类提供了一个 run() 方法，用于定义线程的执行逻辑
                * 在 run() 方法中，可以实现持续监听 Socket 输入流的功能
                * 当有消息到达时，会调用 handleMessage() 方法进行处理
            * 通过调用 interrupt() 方法中断线程
        2. MVC 架构的说明：
            * 监听器持有对模型和控制器的引用，监听器更新模型数据并通知控制器处理业务逻辑
            * 模型负责存储和管理数据
            * 控制器负责处理用户交互逻辑
            * 视图负责显示数据和接收用户输入
*/

public class MessageListener extends Thread {
    private Socket socket;   // 与服务器连接的Socket
    private ClientModel model; // 客户端数据模型
    private LoginController loginController; // 登录控制器
    private ChatController chatController; // 聊天控制器
    private boolean running; // 运行标志位

    /*
        构造函数
    */
    public MessageListener(Socket socket, ClientModel model, 
                          LoginController loginController, 
                          ChatController chatController) {
        this.socket = socket;   // 与服务器连接的Socket
        this.model = model;     // 客户端数据模型
        this.loginController = loginController; // 登录控制器
        this.chatController = chatController; // 聊天控制器
        this.running = true;   // 运行标志位
    }

    @Override
    public void run() {
        while (running && !socket.isClosed()) {
            try {
                // 从Socket读取消息，这会阻塞直到收到消息
                Object obj = IOStream.readMessage(socket);
                if (obj == null) continue;
                
                // 将消息转换为封装信息对象
                encap_info info = (encap_info) obj;
                
                // 根据消息类型分发处理
                switch (info.get_type()) {
                    case 3: // 登录消息
                        handleLoginMessage(info.get_login_info());
                        break;
                    case 4: // 聊天消息
                        handleChatMessage(info.get_chat_info());
                        break;
                    case 1: // 群组消息
                        handleGroupMessage(info.get_group_info());
                        break;
                    case 5: // 注册消息
                        handleRegisterMessage(info.get_reg_info());
                        break;
                    case 2: // 登出消息
                        handleLogoutMessage();
                        break;
                    default:
                        System.out.println("Warning: 收到未知类型的消息: " + info.get_type());
                }
            } catch (ClassCastException e) {
                // 消息类型转换错误
                System.err.println("消息格式错误: " + e.getMessage());
            } catch (Exception e) {
                // 其他异常，包括网络异常
                System.err.println("接收消息时发生错误: " + e.getMessage());
                e.printStackTrace();
                
                // 如果是网络相关问题，停止监听
                if (e instanceof java.net.SocketException || 
                    e.getCause() instanceof java.net.SocketException) {
                    System.err.println("网络连接异常，停止监听");
                    stopListening();
                }
            }
        }
        
        // 线程结束时，释放 Socket 资源
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("关闭Socket时发生错误: " + e.getMessage());
            }
        }
        
        System.out.println("消息监听线程已停止");
    }
    
    /*
        处理登录结果，更新用户状态
    */
    private void handleLoginMessage(Login_info loginInfo) {
        if (loginInfo == null) return;
        if (loginInfo.isKicked()){
            JOptionPane.showMessageDialog(
                    null,                     // 父组件（null表示居中显示）
                    "您已在其他设备登录",      // 错误消息内容
                    "错误",                   // 窗口标题
                    JOptionPane.ERROR_MESSAGE // 消息类型（显示错误图标）
            );
            chatController.cleanup();
            running = false;
            System.exit(0);
            return;
        }
        System.out.println("收到登录消息: " + 
            "用户名=" + loginInfo.getUserName() + 
            ", 成功标志=" + loginInfo.getLoginSuccessFlag() +
            ", 当前用户=" + model.getCurrentUser());
        
        // 更新在线用户列表 - 这个对所有登录消息都需要做
        model.setOnlineUsers(loginInfo.getOnlineUsers());
        
        // 如果当前客户端还没有登录用户，这可能是我们的登录响应
        if (model.getCurrentUser() == null || !model.isLoggedIn()) {
            // 第一次登录的情况
            if (loginInfo.getLoginSuccessFlag()) {
                model.setCurrentUser(loginInfo.getUserName());
                model.setLoggedIn(true);
                loginController.onLoginSuccess();
            } else {
                // 登录失败
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
    
    /*
        处理聊天消息，添加到历史记录
    */
    private void handleChatMessage(Chat_info chatInfo) {
        if (chatInfo == null) return;
        
        // 添加消息到历史记录
        model.addMessage(chatInfo);
        
        // 通知聊天控制器处理新消息
        chatController.onNewMessage(chatInfo);
    }
    
    /*
        TODO: 处理群组操作
    */
    private void handleGroupMessage(Group_info groupInfo) {
        if (groupInfo == null) return;

        if (groupInfo.isEstablish()) {
            // 新建群组
            model.updateGroup(groupInfo);
            chatController.onGroupCreated(groupInfo);
        } else if (!groupInfo.isExist()) {
            // 被移出群组
            model.removeGroup(groupInfo.get_Group_id());
            chatController.onRemovedFromGroup(groupInfo);
        } else {
            // 群组更新
            model.updateGroup(groupInfo);
            chatController.onGroupUpdated(groupInfo);
        }
    }
    
    /*
        处理注册消息
    */
    private void handleRegisterMessage(Reg_info regInfo) {
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
    
    /*
        处理登出消息
    */
    private void handleLogoutMessage() {
        // 清除模型数据
        model.clear();
        // 通知登录控制器处理登出
        loginController.onLogout();
    }
    
    /*
        停止监听线程
    */
    public void stopListening() {
        this.running = false;
        // 中断线程，如果它在阻塞状态
        this.interrupt();
    }
} 