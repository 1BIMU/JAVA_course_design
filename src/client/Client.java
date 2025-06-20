package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import client.controller.ChatController;
import client.controller.LoginController;
import client.controller.VoiceCallController;
import client.controller.LoginController.LoginCallback;
import client.model.ClientModel;
import client.view.ContactListView;

/* 
    一些边写边学的记录：
        * 通知机制：
            * 在 LoginController 类中添加了 setLoginCallback 方法，用于设置回调对象；
            * 在 onLoginSuccess 方法中，检查回调是否存在，如果存在则调用其 onLoginSuccess 方法；
            * 作用：允许 LoginController 类在登录成功时通知 Client 类处理相关事件，而不需要直接创建 ChatView 或 ChatController 等对象，保持了类之间的松耦合；
*/

/*
    客户端主类，负责初始化客户端并管理控制器
*/
public class Client implements LoginCallback {
    private Socket socket;   // 与服务器连接的Socket
    private ClientModel model; // 客户端数据模型
    private LoginController loginController; // 登录控制器
    private ChatController chatController; // 聊天控制器
    private VoiceCallController voiceCallController; // 语音通话控制器
    private ContactListView contactListView; // 联系人列表视图
    private MessageListener messageListener;
    private MessageSender messageSender; // 新增
    
    /*
        初始化客户端
    */
    public Client() {
        this.model = new ClientModel();
    }
    
    /*
        连接服务器
    */
    public boolean connectToServer(String host, int port) {
        try {
            // 创建Socket连接
            this.socket = new Socket(host, port);
            System.out.println("连接到服务器: " + host + ":" + port);
            
            // 创建消息发送器
            this.messageSender = new MessageSender(socket);
            
            // 初始化控制器，传入消息发送器而不是Socket
            this.loginController = new LoginController(model, messageSender);
            this.loginController.setLoginCallback(this);
            
            this.chatController = new ChatController(model, messageSender);
            
            // 初始化语音通话控制器
            this.voiceCallController = new VoiceCallController(model, messageSender);
            
            // 启动消息监听线程
            this.messageListener = new MessageListener(socket, model, loginController, chatController, voiceCallController);
            this.messageListener.setMessageSender(messageSender);
            this.messageListener.start();
            
            return true;
        } catch (UnknownHostException e) {
            System.err.println("无法解析服务器地址: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }
    
    /*
        获取登录控制器
    */
    public LoginController getLoginController() {
        return loginController;
    }
    
    /*
        获取聊天控制器
    */
    public ChatController getChatController() {
        return chatController;
    }
    
    /*
        获取语音通话控制器
    */
    public VoiceCallController getVoiceCallController() {
        return voiceCallController;
    }
    
    /*
        关闭客户端连接
    */
    public void disconnect() {
        try {
            // 执行聊天控制器的清理工作，保存聊天记录
            if (chatController != null) {
                chatController.cleanup();
            }
            
            // 关闭语音通话控制器
            if (voiceCallController != null) {
                voiceCallController.shutdown();
            }
            
            // 停止消息监听线程
            if (messageListener != null) {
                messageListener.stopListening();
            }
            
            // 关闭Socket连接
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            System.out.println("已断开与服务器的连接");
        } catch (IOException e) {
            System.err.println("关闭连接失败: " + e.getMessage());
        }
    }
    
    /*
        客户端启动入口
    */
    public static void main(String[] args) {
        // 设置服务器地址和端口
        String host = "127.0.0.1";
        int port = 6688;
        
        // 从命令行参数获取服务器地址和端口
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("端口号格式错误，使用默认端口6688");
            }
        }
        
        // 创建并启动客户端
        Client client = new Client();
        
        // 连接服务器
        if (client.connectToServer(host, port)) {
            // 添加关闭钩子，确保程序退出时释放资源
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                client.disconnect();
            }));
            
            // 启动客户端
            client.start();
        } else {
            System.err.println("无法连接到服务器，程序退出");
            System.exit(1);
        }
    }
    
    /* 一些说明：
        以下的 2 个方法实现了 LoginCallback 接口，使得 LoginController 能够通知 Client 类处理这些事件；
    */

    /*
        实现LoginCallback接口的onLoginSuccess方法
        当用户登录成功时，创建联系人列表视图并显示
    */
    @Override
    public void onLoginSuccess() {
        // 创建联系人列表视图
        contactListView = new ContactListView(chatController, model, model.getCurrentUser());
        
        // 设置语音通话控制器的引用
        if (contactListView != null) {
            contactListView.setVoiceCallController(voiceCallController);
        }
        
        // 注册模型观察者
        model.addObserver(contactListView);
        
        // 初始化视图数据
        contactListView.updateUserList();
        contactListView.updateGroupList();
        contactListView.updateOrgList();
        
        // 显示联系人列表视图
        contactListView.setVisible(true);
        
        // 设置状态为在线
        contactListView.updateStatus("在线");
    }
    
    /*
        实现LoginCallback接口的onLogout方法
        当用户登出时，关闭联系人列表视图
    */
    @Override
    public void onLogout() {
        // 移除模型观察者
        if (contactListView != null) {
            model.removeObserver(contactListView);
        }
        
        // 关闭联系人列表视图
        if (contactListView != null) {
            contactListView.dispose();
            contactListView = null;
        }
        
        // 显示登录界面
        loginController.showLoginView();
    }
    
    /*
        启动客户端
    */
    public void start() {
        // 显示登录界面
        loginController.showLoginView();
    }
} 