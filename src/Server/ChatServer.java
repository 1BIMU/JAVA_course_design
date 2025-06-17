package Server;
import Server.view.ServerWindow;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import Frame.ServerWindow;
import info.encap_info;
import io.IOStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {// 服务器启动入口
    public ArrayList<String> online_users = new ArrayList<String>();//维护在线用户列表
    public ArrayList<Socket> online_sockets = new ArrayList<Socket>();
    public Map<String, Socket> userSocketMap = new HashMap<>();
    public Map<Integer, ArrayList<String>> groupMap = new ConcurrentHashMap<>(); // 群组ID到成员列表的映射
    private int nextGroupId = 1; // 用于生成唯一的群组ID
    private ServerWindow serverWindow;
    
    public ChatServer() {
        try {
            // 建立服务器的Socket监听
            ServerSocket sso = new ServerSocket(6688);
            serverWindow = new ServerWindow();
            serverWindow.setVisible(true);
            serverWindow.appendMessage("服务器已启动，监听端口: 6688");
            
            // 循环是为了解决多客户端使用
            while(true) {
                // 等待连接，阻塞实现，会得到一个客户端的连接
                Socket socket = sso.accept();
                serverWindow.appendMessage("收到新的客户端连接: " + socket.getInetAddress().getHostAddress());
                
                // 开启一个新的线程，用于服务这个连接上的用户
                ServerHandler serverHandler = new ServerHandler(socket, this, serverWindow);
                serverHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            serverWindow.appendMessage("服务器启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加在线用户
     * @param user 用户名
     */
    public synchronized void add_online_user(String user) {
        this.online_users.add(user);
        serverWindow.appendMessage("用户 " + user + " 已登录");
        serverWindow.updateOnlineUsers(online_users);
    }
    
    /**
     * 添加在线用户的Socket
     * @param socket 用户Socket
     */
    public synchronized void add_online_socket(Socket socket) {
        this.online_sockets.add(socket);
    }
    
    /**
     * 移除在线用户
     * @param user 用户名
     */
    public synchronized void remove_online_user(String user) {
        this.online_users.remove(user);
        serverWindow.appendMessage("用户 " + user + " 已登出");
        serverWindow.updateOnlineUsers(online_users);
    }
    
    /**
     * 移除在线用户的Socket
     * @param socket 用户Socket
     */
    public synchronized void remove_online_socket(Socket socket) {
        this.online_sockets.remove(socket);
    }
    
    /**
     * 广播消息给所有在线用户
     * @param message 要广播的消息
     */
    public synchronized void broadcastMessage(encap_info message) {
        for (Socket socket : online_sockets) {
            IOStream.writeMessage(socket, message);
        }
    }
    
    /**
     * 发送消息给特定用户
     * @param username 目标用户名
     * @param message 要发送的消息
     * @return 是否发送成功
     */
    public synchronized boolean sendMessageToUser(String username, encap_info message) {
        Socket socket = userSocketMap.get(username);
        if (socket != null && !socket.isClosed()) {
            return IOStream.writeMessage(socket, message);
        }
        return false;
    }
    
    /**
     * 创建新群组
     * @param groupName 群组名称
     * @param members 群组成员列表
     * @return 新群组的ID
     */
    public synchronized int createGroup(String groupName, ArrayList<String> members) {
        int groupId = nextGroupId++;
        groupMap.put(groupId, members);
        serverWindow.appendMessage("创建新群组: " + groupName + " (ID: " + groupId + ")");
        return groupId;
    }
    
    /**
     * 更新群组成员
     * @param groupId 群组ID
     * @param members 新的成员列表
     * @return 是否更新成功
     */
    public synchronized boolean updateGroup(int groupId, ArrayList<String> members) {
        if (groupMap.containsKey(groupId)) {
            groupMap.put(groupId, members);
            serverWindow.appendMessage("更新群组 ID: " + groupId + " 的成员");
            return true;
        }
        return false;
    }
    
    /**
     * 获取群组成员
     * @param groupId 群组ID
     * @return 成员列表，如果群组不存在则返回null
     */
    public synchronized ArrayList<String> getGroupMembers(int groupId) {
        return groupMap.get(groupId);
    }
    
    /**
     * 删除群组
     * @param groupId 群组ID
     */
    public synchronized void removeGroup(int groupId) {
        groupMap.remove(groupId);
        serverWindow.appendMessage("删除群组 ID: " + groupId);
    }
    
    /**
     * 检查用户是否在群组中
     * @param username 用户名
     * @param groupId 群组ID
     * @return 是否在群组中
     */
    public synchronized boolean isUserInGroup(String username, int groupId) {
        ArrayList<String> members = groupMap.get(groupId);
        return members != null && members.contains(username);
    }
    
    public static void main(String[] args) {
        new ChatServer();
    }
}
