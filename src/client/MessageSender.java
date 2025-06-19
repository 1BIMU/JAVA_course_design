package client;

import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import info.*;
import io.IOStream;
import java.util.ArrayList;
import java.util.List;

/*
    消息发送器，负责向服务器发送各类消息
*/
public class MessageSender {
    private Socket socket;
    private String host;
    private int port;
    private boolean reconnecting = false;
    
    /*
        构造函数
    */
    public MessageSender(Socket socket) {
        this.socket = socket;
        try {
            this.host = socket.getInetAddress().getHostAddress();
            this.port = socket.getPort();
        } catch (Exception e) {
            System.err.println("获取Socket地址和端口失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查连接状态，如果连接已关闭则尝试重连
     * @return 连接是否可用
     */
    private boolean ensureConnected() {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            if (reconnecting) {
                return false;
            }
            
            reconnecting = true;
            try {
                System.out.println("连接已关闭，尝试重新连接到服务器: " + host + ":" + port);
                socket = new Socket(host, port);
                System.out.println("重新连接成功");
                reconnecting = false;
                return true;
            } catch (IOException e) {
                System.err.println("重新连接服务器失败: " + e.getMessage());
                reconnecting = false;
                return false;
            }
        }
        return true;
    }
    
    /*
        发送登录请求
    */
    public boolean sendLoginRequest(String username, String password) {
        if (!ensureConnected()) {
            return false;
        }
        
        Login_info loginInfo = new Login_info();
        loginInfo.setUserName(username);
        loginInfo.setPassword(password);
        loginInfo.setLoginSuccessFlag(false);
        
        encap_info info = new encap_info();
        info.set_type(3);
        info.set_login_info(loginInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送注册请求
     */
    public boolean sendRegisterRequest(String username, String password) {
        if (!ensureConnected()) {
            return false;
        }
        
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
        if (!ensureConnected()) {
            return false;
        }
        
        System.out.println("MessageSender - 发送消息 - 发送者: " + fromUser);
        Chat_info chatInfo = new Chat_info();
        chatInfo.setType(isGroupChat);
        chatInfo.setFrom_username(fromUser);
        chatInfo.setText(message);
        
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
        if (!ensureConnected()) {
            return false;
        }
        
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        System.out.println(members);//让我看看怎么个事
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
        if (!ensureConnected()) {
            return false;
        }
        
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
        if (!ensureConnected()) {
            return false;
        }
        
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
        if (!ensureConnected()) {
            return false;
        }
        
        encap_info info = new encap_info();
        info.set_type(2);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送断开连接通知
    */
    public boolean sendDisconnectNotification() {
        if (socket == null || socket.isClosed()) {
            return false;
        }
        
        encap_info info = new encap_info();
        info.set_type(6);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 获取当前Socket
     */
    public Socket getSocket() {
        return socket;
    }
    
    /**
     * 设置新的Socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
        try {
            this.host = socket.getInetAddress().getHostAddress();
            this.port = socket.getPort();
        } catch (Exception e) {
            System.err.println("获取Socket地址和端口失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送创建小组请求
     * @param groupId 所属群组ID
     * @param orgName 小组名称
     * @param members 小组成员列表
     * @return 是否发送成功
     */
    public boolean sendCreateOrgRequest(int groupId, String orgName, List<String> members) {
        if (!ensureConnected()) {
            return false;
        }
        
        Org_info orgInfo = new Org_info();
        orgInfo.setOrg_name(orgName);
        orgInfo.setGroup_id(groupId);
        orgInfo.setMembers(new ArrayList<>(members));
        orgInfo.setEstablish(true);
        orgInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(6); // 组消息类型
        info.set_org_info(orgInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送更新小组请求
     * @param orgId 小组ID
     * @param orgName 小组名称
     * @param members 小组成员列表
     * @return 是否发送成功
     */
    public boolean sendUpdateOrgRequest(int orgId, String orgName, ArrayList<String> members) {
        if (!ensureConnected()) {
            return false;
        }
        
        Org_info orgInfo = new Org_info();
        orgInfo.setOrg_id(orgId);
        orgInfo.setOrg_name(orgName);
        orgInfo.setMembers(new ArrayList<>(members));
        orgInfo.setEstablish(false);
        orgInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(6); // 组消息类型
        info.set_org_info(orgInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送退出小组请求
     * @param orgId 小组ID
     * @return 是否发送成功
     */
    public boolean sendLeaveOrgRequest(int orgId) {
        if (!ensureConnected()) {
            return false;
        }
        
        Org_info orgInfo = new Org_info();
        orgInfo.setOrg_id(orgId);
        orgInfo.setExist(false);
        
        encap_info info = new encap_info();
        info.set_type(6); // 组消息类型
        info.set_org_info(orgInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送小组聊天消息
     * @param username 用户名
     * @param message 消息内容
     * @param groupId 群组ID
     * @param orgId 小组ID
     * @return 是否发送成功
     */
    public boolean sendOrgChatMessage(String username, String message, int groupId, int orgId) {
        if (!ensureConnected()) {
            return false;
        }
        
        Chat_info chatInfo = new Chat_info();
        chatInfo.setFrom_username(username);
        chatInfo.setText(message);
        chatInfo.setType(true); // 设置为群聊类型
        chatInfo.setGroup_id(groupId);
        chatInfo.setOrgMessage(true);
        chatInfo.setOrg_id(orgId);
        
        encap_info info = new encap_info();
        info.set_type(4); // 聊天消息类型
        info.set_chat_info(chatInfo);
        
        return IOStream.writeMessage(socket, info);
    }
}