package Server;
import Server.view.ServerWindow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {// 服务器启动入口
    public ArrayList<String> online_users = new ArrayList<String>();//维护在线用户列表
    public ArrayList<Socket> online_sockets = new ArrayList<Socket>();
    public Map<String,Socket> userSocketMap = new HashMap<>();
    public int port = 6688;
    public String ip;
    public ChatServer() {
        try {
            //建立服务器的Socket监听
            ServerSocket sso = new ServerSocket(port);
            ServerWindow ServerFrame  = new ServerWindow();
            String ip = InetAddress.getLocalHost().getHostAddress();

            ServerFrame.setVisible(true);
            ServerFrame.setServerInfo("JAVA聊天",ip,port);
            //循环是为了解决多客户端使用
            while(true) {
                //等待连接，阻塞实现，会得到一个客户端的连接
                Socket socket = sso.accept();
                ServerHandler serverHandler = new ServerHandler(socket,this,ServerFrame);//开启一个新的线程，用于服务这个连接上的用户
                serverHandler.start();
                ServerFrame.appendLog("服务器接受到客户端的连接：" + socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void add_online_user(String user){
        this.online_users.add(user);
    }
    public void remove_online_user(String user){
        this.online_users.remove(user);
    }
    public static void main(String[] args) {
        new ChatServer();
    }
    public void add_online_socket(Socket socket){
        this.online_sockets.add(socket);
    }
    public void remove_online_socket(Socket socket){
        this.online_sockets.remove(socket);
    }
}
