package client;

import java.net.Socket;
import info.*;
import io.IOStream;
import java.util.ArrayList;
import java.util.List;

/*
    消息发送器，负责向服务器发送各类消息
*/
public class MessageSender {
    private Socket socket;
    
    /*
        构造函数
    */
    public MessageSender(Socket socket) {
        this.socket = socket;
    }
    
    /*
        发送登录请求
    */
    public boolean sendLoginRequest(String username, String password) {
        Login_info loginInfo = new Login_info();
        loginInfo.setUserName(username);
        loginInfo.setPassword(password);
        loginInfo.setLoginSucceessFlag(false);
        
        encap_info info = new encap_info();
        info.set_type(3);
        info.set_login_info(loginInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送注册请求
     */
    public boolean sendRegisterRequest(String username, String password) {
        Reg_info regInfo = new Reg_info();
        regInfo.setUsername(username);
        regInfo.setPassword(password);
        regInfo.setReg_status(0);
        
        encap_info info = new encap_info();
        info.set_type(5);
        info.set_reg_info(regInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送聊天消息
    */
    public boolean sendChatMessage(String fromUser, String message, boolean isGroupChat, String targetId) {
        System.out.println("MessageSender - 发送消息 - 发送者: " + fromUser);
        Chat_info chatInfo = new Chat_info();
        chatInfo.setType(isGroupChat);
        chatInfo.setFrom_username(fromUser);
        chatInfo.setMessage(message);
        
        if (isGroupChat) {
            chatInfo.setGroup_id(Integer.parseInt(targetId));
        } else {
            chatInfo.setTo_username(targetId);
        }
        
        encap_info info = new encap_info();
        info.set_type(4);
        info.set_chat_info(chatInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送创建群组请求
    */
    public boolean sendCreateGroupRequest(String groupName, List<String> members) {
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        groupInfo.setEstablish(true);
        groupInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送更新群组请求
    */
    public boolean sendUpdateGroupRequest(int groupId, String groupName, List<String> members) {
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        groupInfo.setEstablish(false);
        groupInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送退出群组请求
    */
    public boolean sendLeaveGroupRequest(int groupId) {
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.setExist(false);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送登出请求
    */
    public boolean sendLogoutRequest() {
        encap_info info = new encap_info();
        info.set_type(2);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送断开连接通知
    */
    public boolean sendDisconnectNotification() {
        encap_info info = new encap_info();
        info.set_type(6);
        
        return IOStream.writeMessage(socket, info);
    }
}