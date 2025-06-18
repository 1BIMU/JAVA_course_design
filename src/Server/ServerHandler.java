package Server;
import Server.view.ServerWindow;
import Server.handler.MessageHandler;
import Server.handler.MessageHandlerFactory;
import info.*;
import io.FileIO;

import java.io.IOException;
import java.net.Socket;

import java.util.*;

import io.IOStream;
import Server.controller.ServerController;
public class ServerHandler extends Thread {

    Socket socket;
    ChatServer server;
    ServerWindow ServerFrame;
    ServerController controller;
    boolean running = true;
    String current_user;//标记当前线程服务的用户
    private MessageHandlerFactory handlerFactory;

    public ServerHandler(Socket socket, ChatServer server, ServerWindow serverframe) {
        this.socket = socket;
        this.server = server;
        this.ServerFrame = serverframe;
        controller = new ServerController(socket, server,serverframe,this);
        this.handlerFactory = new MessageHandlerFactory(controller, server, serverframe, current_user);
    }
    //实例化Controller

    @Override
    public void run() {
        //默认重复拿
        while(running) {
            try {
                Object obj = IOStream.readMessage(socket);
                // 检查是否收到了null，这可能意味着连接已关闭
                if (obj == null) {
                    ServerFrame.appendLog("连接已关闭，停止处理消息");
                    break;
                }

                encap_info INFO = (encap_info)obj;
                encap_info RETURN = new encap_info();

                // 获取对应的消息处理器
                MessageHandler handler = handlerFactory.getHandler(INFO.get_type());
                if (handler != null) {
                    // 如果找到了处理器，则调用它处理消息
                    boolean continueProcessing = handler.handle(INFO, socket, RETURN);

                    // 如果是登录消息，可能需要更新当前用户
                    if (INFO.get_type() == 3) {
                        if (INFO.get_login_info().getLoginSuccessFlag()) {
                            this.current_user = INFO.get_login_info().getUserName();
                            handlerFactory.updateCurrentUser(current_user);
                        }
                        // 登录失败不中断循环，继续等待下一次登录请求
                    }

                    // 只有在登出消息时才中断循环
                    if (!continueProcessing && INFO.get_type() == 2) {
                        break;
                    }
                } else {
                    ServerFrame.appendLog("收到未知类型的消息: " + INFO.get_type());
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // 处理IO异常，可能是客户端断开连接
                ServerFrame.appendLog("IO异常，可能是客户端断开连接: " + e.getMessage());
                // 如果当前用户已设置，执行清理操作
                if (current_user != null && !current_user.isEmpty()) {
                    try {
                        // 创建一个登出消息并处理
                        encap_info logoutInfo = new encap_info();
                        logoutInfo.set_type(2);
                        MessageHandler logoutHandler = handlerFactory.getHandler(2);
                        if (logoutHandler != null) {
                            logoutHandler.handle(logoutInfo, socket, new encap_info());
                        }
                    } catch (Exception ex) {
                        ServerFrame.appendLog("处理客户端断开连接时出错: " + ex.getMessage());
                    }
                }
                break;
            }
        }

        // 确保资源被释放
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
            ServerFrame.appendLog("Socket已关闭，线程结束");
        } catch (IOException e) {
            ServerFrame.appendLog("关闭Socket时出错: " + e.getMessage());
        }
    }

    public void shutdown() throws IOException {
        //先关闭线程
        socket.close();
        running = false;
    }

}

