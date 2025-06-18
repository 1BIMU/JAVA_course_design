package client;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import client.controller.ChatController;
import client.controller.LoginController;
import client.handler.ClientMessageHandler;
import client.handler.ClientMessageHandlerFactory;
import client.model.ClientModel;
import info.encap_info;
import io.IOStream;

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
        使用策略模式重构：
            * 根据消息类型（info.get_type()）获取对应的处理器
            * 将消息分发给对应的处理器处理
            * 支持处理多种消息类型：登录(3)、聊天(4)、群组(1)、注册(5)、登出(2)
*/

public class MessageListener extends Thread {
    private Socket socket;   // 与服务器连接的Socket
    private ClientModel model; // 客户端数据模型
    private LoginController loginController; // 登录控制器
    private ChatController chatController; // 聊天控制器
    private boolean running; // 运行标志位
    private ClientMessageHandlerFactory handlerFactory; // 消息处理器工厂
    private MessageSender messageSender; // 消息发送器，用于重连

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
        this.handlerFactory = new ClientMessageHandlerFactory(model, loginController, chatController);
    }
    
    /**
     * 设置消息发送器，用于重连
     */
    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void run() {
        int reconnectAttempts = 0;
        final int maxReconnectAttempts = 3;
        
        while (running) {
            try {
                // 检查socket是否关闭
                if (socket == null || socket.isClosed()) {
                    handleClosedSocket(reconnectAttempts, maxReconnectAttempts);
                    reconnectAttempts++;
                    continue;
                }
                
                // 从Socket读取消息，这会阻塞直到收到消息
                Object obj = IOStream.readMessage(socket);
                if (obj == null) {
                    System.err.println("收到null消息，可能是连接已关闭");
                    // 如果连接已关闭，等待重连
                    Thread.sleep(1000);
                    continue;
                }
                
                // 重置重连计数
                reconnectAttempts = 0;
                
                // 将消息转换为封装信息对象
                encap_info info = (encap_info) obj;
                
                // 获取对应的消息处理器
                ClientMessageHandler handler = handlerFactory.getHandler(info.get_type());
                if (handler != null) {
                    // 如果找到了处理器，则调用它处理消息
                    handler.handle(info);
                } else {
                    System.out.println("Warning: 收到未知类型的消息: " + info.get_type());
                }
            } catch (ClassCastException e) {
                // 消息类型转换错误
                System.err.println("消息格式错误: " + e.getMessage());
            } catch (InterruptedException e) {
                // 线程被中断
                System.err.println("线程被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 其他异常
                System.err.println("接收消息时发生错误: " + e.getMessage());
                e.printStackTrace();
                
                // 如果是Socket相关的异常，尝试重连
                if (e instanceof SocketException || 
                    (e.getCause() != null && e.getCause() instanceof SocketException)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 线程结束时，释放 Socket 资源
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("关闭Socket时发生错误: " + e.getMessage());
            }
        }
        
        System.out.println("消息监听线程已停止");
    }
    
    /**
     * 处理Socket关闭的情况
     */
    private void handleClosedSocket(int reconnectAttempts, int maxReconnectAttempts) throws InterruptedException {
        if (messageSender != null) {
            // 尝试通过messageSender重连
            Socket newSocket = messageSender.getSocket();
            if (newSocket != null && !newSocket.isClosed()) {
                this.socket = newSocket;
                System.out.println("MessageListener: 使用MessageSender提供的新Socket");
            } else {
                // 如果重连次数超过上限，则停止监听
                if (reconnectAttempts >= maxReconnectAttempts) {
                    System.err.println("重连次数超过上限，停止监听");
                    stopListening();
                    return;
                }
                System.err.println("Socket已关闭，等待重连... 尝试次数: " + (reconnectAttempts + 1));
                Thread.sleep(2000); // 等待2秒后再次检查
            }
        } else {
            System.err.println("Socket已关闭且无法重连，停止监听");
            stopListening();
        }
    }
    
    /*
        停止监听线程
    */
    public void stopListening() {
        this.running = false;
        // 中断线程，如果它在阻塞状态
        this.interrupt();
    }
    
    /**
     * 更新Socket
     */
    public void updateSocket(Socket socket) {
        this.socket = socket;
    }
}