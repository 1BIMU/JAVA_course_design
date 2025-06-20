package Server.model;

import Server.ChatServer;
import info.Conference_info;
import info.Login_info;
import info.encap_info;
import io.FileIO;
import io.IOStream;
import io.UdpIO;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class ServerModel {
    ChatServer server;
    public ServerModel(ChatServer server) {
        this.server = server;
    }
    public boolean checkUserLogin(Login_info tfi) throws IOException {
        String userName = tfi.getUserName();
        String password = tfi.getPassword();
        FileIO fileio = new FileIO();
        return fileio.validateUser(userName,password);//用户名密码正确
    }
    public boolean checkUserOnline(String User,ArrayList<String> online_users){
        boolean flag = false;
        for(int i = 0;i<online_users.size();i++){
            if(online_users.get(i).equals(User)){
                flag = true;
                break;
            }
        }
        return flag;
    }
    public void sendALL(encap_info INFO){
        for (int i = 0; i < server.online_sockets.size(); i++) {
            Socket tempSocket = server.online_sockets.get(i);
            IOStream.writeMessage(tempSocket , INFO);
        }
    }
    /*
    * 过滤转发列表中非在线的用户
    * */
    public void filterOnlineMembers(ArrayList<String> group_members, ArrayList<String> online_users) {
        // 将在线用户列表转为 HashSet 提高查找效率
        Set<String> onlineSet = new HashSet<>(online_users);

        // 使用 removeIf 方法过滤不在线成员（Java 8+ 特性）
        group_members.removeIf(member -> !onlineSet.contains(member));
    }
    /*
    * 转发消息到指定用户
    * */
    public void Send2Users(encap_info INFO,ArrayList<String> to_user){
        for(int i = 0;i<to_user.size();i++) {
            //先从hashmap中拿到对应用户的socket
            Socket tempSocket = server.userSocketMap.get(to_user.get(i));
            // 检查 socket 是否为 null，避免空指针异常
            if (tempSocket != null) {
                IOStream.writeMessage(tempSocket, INFO);
            } else {
                System.out.println("警告: 用户 " + to_user.get(i) + " 的 Socket 为 null，无法发送消息");
            }
        }
    }

    /*
    * 该方法用于将得到的ID表，构建一个对应到具体用户的映射
    * */
    public Map<Integer,ArrayList<String>> groupMap(ArrayList<Integer> group_members) throws IOException {
        Map<Integer,ArrayList<String>> groupMap = new HashMap<Integer,ArrayList<String>>();
        FileIO fileio = new FileIO();
        for(int i = 0;i<group_members.size();i++) {
            ArrayList<String> temp_members = new ArrayList<>();
            temp_members = fileio.getGroupMembers(group_members.get(i));//获取对应的所有成员
            groupMap.put(group_members.get(i),temp_members);//加入哈希表中
        }
        return groupMap;
    }
/*
* 用于检查，组中是否存在，不在群聊中的人，两个for，比较丑陋
* */
    public boolean IsInGroup(ArrayList<String> group_members, ArrayList<String> org_members) {
        boolean flag = false;//是否存在不在群聊中的人
        for (String orgMember : org_members) {
            boolean is_member = false;
            for (String groupMember : group_members) {
                if (orgMember.equals(groupMember)) {
                    is_member = true;//在群中
                    break;
                }
            }
            if (!is_member) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
