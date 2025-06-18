package Server;
import Server.view.ServerWindow;
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
    public ServerHandler(Socket socket, ChatServer server, ServerWindow serverframe) {
        this.socket = socket;
        this.server = server;
        this.ServerFrame = serverframe;
        controller = new ServerController(socket, server,serverframe,this);
    }
    //实例化Controller

    @Override
    public void run() {
        //默认重复拿
        while(running) {
            try {
                Object obj = IOStream.readMessage(socket);
                encap_info INFO = (encap_info)obj;
                encap_info RETURN = new encap_info();
                if(INFO.get_type()==3) {//处理login消息
                    if(!controller.Login_handler(INFO, RETURN)){//如果登录失败
                        shutdown();
                    }
                }else if(INFO.get_type()==4) {//收到一条消息
                    controller.Chat_handler(INFO,RETURN);
                }else if(INFO.get_type()==1) {//如果收到的消息为群聊控制消息
                    controller.Group_handler(INFO,RETURN);
                }else if(INFO.get_type()==5) {//收到了注册消息
                    controller.REG_handler(INFO,RETURN);
                }else if(INFO.get_type()==2) {//如果是登出类型消息
                    controller.LogoutHandler(INFO,RETURN);
                    break;
                }else if(INFO.get_type()==6){
                    controller.Org_handler(INFO,RETURN);
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void shutdown() throws IOException {
        //先关闭线程
        socket.close();
        running = false;
    }

}

