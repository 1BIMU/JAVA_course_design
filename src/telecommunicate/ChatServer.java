package telecommunicate;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import Frame.ServerWindow;
import java.util.ArrayList;
public class ChatServer {// 服务器启动入口
    ArrayList<String> online_users;//维护在线用户列表
    public ChatServer() {
        try {
            //建立服务器的Socket监听
            ServerSocket sso = new ServerSocket(6688);
            new ServerWindow();
            //循环是为了解决多客户端使用
            while(true) {
                //等待连接，阻塞实现，会得到一个客户端的连接
                Socket socket = sso.accept();
                ServerHandler serverHandler = new ServerHandler(socket,this);//开启一个新的线程，用于服务这个连接上的用户
                serverHandler.start();
                System.out.println("服务器接受到客户端的连接：" + socket);
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
}
