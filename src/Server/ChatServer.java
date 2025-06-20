package Server;
import Server.view.ServerWindow;
import info.Conference_info;
import io.FileIO;
import io.UdpIO;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Server.model.*;
public class ChatServer {// 服务器启动入口
    public ArrayList<String> online_users = new ArrayList<String>();//维护在线用户列表
    public ArrayList<Socket> online_sockets = new ArrayList<Socket>();
    public Map<String,Socket> userSocketMap = new HashMap<>();
    public Map<Socket,ServerHandler> SocketHandlerMap = new HashMap<>();
    public Map<Integer, ArrayList<String>> ConferenceOnlineUsersMap = new HashMap<>();//映射关系：会议ID->加入了会议的用户列表

    //新增UDP Socket
    public DatagramSocket audioSocket;
    public ServerWindow ServerFrame;
    public int port = 6688;
    public String ip;

    // --- 新增数据结构结束 ---
    public ChatServer() {
        try {
            //建立服务器的Socket监听
            ServerSocket sso = new ServerSocket(port);
            audioSocket = new DatagramSocket(port + 1);//分配新的端口给语音通信模块
            new Thread(this::listenForAudioPackets).start(); // 启动 UDP 监听线程,绑定新的监听函数，防止和TCP冲突
            ServerFrame  = new ServerWindow();
            ServerFrame.appendLog("UDP Audio Server started on port: " + (port + 1));
            String ip = InetAddress.getLocalHost().getHostAddress();
            ServerFrame.setVisible(true);
            ServerFrame.setServerInfo("JAVA聊天",ip,port);
            //循环是为了解决多客户端使用
            while(true) {
                //等待连接，阻塞实现，会得到一个客户端的连接
                Socket socket = sso.accept();
                ServerHandler serverHandler = new ServerHandler(socket,this,ServerFrame);//开启一个新的线程，用于服务这个连接上的用户
                SocketHandlerMap.put(socket,serverHandler);
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

    // UDP 监听方法
    private void listenForAudioPackets() {
        final int MAX_UDP_PACKET_SIZE = 8192; // 根据 Conference_info 对象序列化后的大小调整
        // 一般 RTP 包大小小于 1500 字节，但 Java 序列化可能膨胀
        //创建一个buffer，如果收到的UDP包的顺序不对，那么就得把它存起来

        //创建一个HASHMAP，根据会议（群）号和用户，映射到对应的期望收到的序列号


        while (true) {
            try {
                // 调用 UdpIO 来接收对象
                Object receivedObj = UdpIO.receiveObject(audioSocket, MAX_UDP_PACKET_SIZE);

                if (receivedObj instanceof Conference_info audioInfo) {
                    handleReceivedAudioInfo(audioInfo);
                } else if (receivedObj != null) {
                    System.err.println("Received unexpected UDP object type: " + receivedObj.getClass().getName());
                }

            } catch (Exception e) { // 捕获所有可能异常，防止线程意外终止
                System.err.println("Error in UDP audio listening thread: " + e.getMessage());
                e.printStackTrace();
                // 考虑是否需要短暂暂停或重置 socket
            }
        }
    }
    private void handleReceivedAudioInfo(Conference_info INFO) throws IOException {//对包进行分析和管理，后续再封装吧，现在先这里测试
        int ACTION = INFO.getActionType();
        switch (ACTION) {
            case 1 -> {
                ArrayList<String> NewConferenceUser = new ArrayList<>();
                NewConferenceUser.add(INFO.getFromUser());
                ConferenceOnlineUsersMap.put(INFO.getConferenceId(), NewConferenceUser);
                //给发送方返回一个创建成功的消息
                INFO.setActionType(11);
                INFO.setSuccess(true);
                Send2Users_UDP(INFO,INFO.getFromUser());
            }
            case 2 -> {
                ArrayList<String> ConferenceUser = ConferenceOnlineUsersMap.get(INFO.getConferenceId());
                ConferenceUser.add(INFO.getFromUser());
                ConferenceOnlineUsersMap.put(INFO.getConferenceId(), ConferenceUser);
                //返回加入成功
                INFO.setActionType(12);
                INFO.setSuccess(true);
                Send2Users_UDP(INFO,INFO.getFromUser());
            }
            case 3 -> {
                ArrayList<String> ConferenceUser = ConferenceOnlineUsersMap.get(INFO.getConferenceId());
                ConferenceUser.remove(INFO.getFromUser());
                ConferenceOnlineUsersMap.put(INFO.getConferenceId(), ConferenceUser);
                //返回退出成功
                INFO.setActionType(13);
                INFO.setSuccess(true);
                Send2Users_UDP(INFO,INFO.getFromUser());
            }
            case 4 -> {//收到消息了要转发
                ArrayList<String> ConferenceUser = ConferenceOnlineUsersMap.get(INFO.getConferenceId());
                Send2Users_UDP(INFO,ConferenceUser);
                INFO.setActionType(14);
                INFO.setSuccess(true);
                Send2Users_UDP(INFO,INFO.getFromUser());
            }
            default -> {//出现异常了，收到了不存在的包
                ServerFrame.appendLog("UDP Audio Server unknown action: " + ACTION);
                //报错，鲁棒性处理
                INFO.setSuccess(false);
                Send2Users_UDP(INFO,INFO.getFromUser());
            }
        }
    }

    public void Send2Users_UDP(Conference_info INFO, ArrayList<String> to_user) throws IOException {//这个后续丢到model里面，先放这里
        DatagramSocket tempSocket = this.audioSocket;
        FileIO fileio = new FileIO();
        for(int i = 0;i<to_user.size();i++) {
            InetAddress ip = InetAddress.getByName(fileio.getUserIP(to_user.get(i)));
            UdpIO.sendObject(tempSocket,INFO,ip);
        }
    }
    public void Send2Users_UDP(Conference_info INFO, String to_user) throws IOException {//这个后续丢到model里面，先放这里
        DatagramSocket tempSocket = this.audioSocket;
        FileIO fileio = new FileIO();
        InetAddress ip = InetAddress.getByName(fileio.getUserIP(to_user));
        UdpIO.sendObject(tempSocket,INFO,ip);
    }
}
