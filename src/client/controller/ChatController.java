package client.controller;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import client.model.ClientModel;
import client.view.ChatView;
import info.Chat_info;
import info.Group_info;
import info.encap_info;
import io.IOStream;

/*
    一些说明：通过视图中的接口与视图组件进行交互
*/

/*
    聊天控制器，处理聊天相关的业务逻辑
*/
public class ChatController {
    private Socket socket;    // 与服务器连接的Socket
    private ClientModel model; // 客户端数据模型
    private ChatView chatView; // 聊天视图
    
    /*
        构造函数
    */
    public ChatController(Socket socket, ClientModel model) {
        this.socket = socket;
        this.model = model;
    }
    
    /*
        设置聊天视图
    */
    public void setChatView(ChatView chatView) {
        this.chatView = chatView;
    }
    
    /*
        发送聊天消息
    */
    public void sendMessage(String message, boolean isGroupChat, String targetId) {
        // 输入验证
        if (message == null || message.trim().isEmpty()) {
            chatView.showError("消息不能为空");
            return;
        }
        
        try {
            // 创建聊天信息对象
            Chat_info chatInfo = new Chat_info();
            chatInfo.setType(isGroupChat); // true表示群聊，false表示私聊
            chatInfo.setFrom_username(model.getCurrentUser());
            chatInfo.setMessage(message);
            
            if (isGroupChat) {
                // 群聊消息
                chatInfo.setGroup_id(Integer.parseInt(targetId));
            } else {
                // 私聊消息
                chatInfo.setTo_username(targetId);
            }
            
            // 创建封装信息对象
            encap_info info = new encap_info();
            info.set_type(4); // 聊天消息类型
            info.set_chat_info(chatInfo);
            
            // 发送消息
            boolean success = IOStream.writeMessage(socket, info);
            
            if (success) {
                // 添加到本地消息历史
                model.addMessage(chatInfo);
                
                // 清空输入框
                if (chatView != null) {
                    chatView.clearMessageInput();
                }
            } else {
                if (chatView != null) {
                    chatView.showError("发送消息失败");
                }
            }
        } catch (NumberFormatException e) {
            if (chatView != null) {
                chatView.showError("群组ID格式错误");
            }
        }
    }
    
    /*
        创建新群组
    */
    public void createGroup(String groupName, List<String> members) {
        // 输入验证
        if (groupName == null || groupName.trim().isEmpty()) {
            chatView.showError("群组名称不能为空");
            return;
        }
        if (members == null || members.isEmpty()) {
            chatView.showError("群组成员不能为空");
            return;
        }
        
        // 确保当前用户在群组中
        if (!members.contains(model.getCurrentUser())) {
            members.add(model.getCurrentUser());
        }
        
        // 创建群组信息对象
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        groupInfo.setEstablish(true); // 新建群组
        groupInfo.setExist(true);
        
        // 创建封装信息对象
        encap_info info = new encap_info();
        info.set_type(1); // 群组消息类型
        info.set_group_info(groupInfo);
        
        // 发送创建群组请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success && chatView != null) {
            chatView.showError("创建群组失败");
        }
    }
    
    /*
        退出群组
    */
    public void leaveGroup(int groupId) {
        // 创建群组信息对象
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.setExist(false); // 退出群组
        
        // 创建封装信息对象
        encap_info info = new encap_info();
        info.set_type(1); // 群组消息类型
        info.set_group_info(groupInfo);
        
        // 发送退出群组请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success && chatView != null) {
            chatView.showError("退出群组失败");
        }
    }
    
    /*
        添加用户到群组
    */
    public void addUserToGroup(int groupId, String username) {
        // 获取当前群组信息
        Group_info currentGroup = model.getGroups().get(groupId);
        if (currentGroup == null) {
            chatView.showError("群组不存在");
            return;
        }
        
        // 创建新的成员列表
        ArrayList<String> newMembers = new ArrayList<>(currentGroup.getMembers());
        
        // 检查用户是否已在群组中
        if (newMembers.contains(username)) {
            chatView.showError("用户已在群组中");
            return;
        }
        
        // 添加新成员
        newMembers.add(username);
        
        // 创建群组信息对象
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.set_Group_name(currentGroup.get_Group_name());
        groupInfo.setMembers(newMembers);
        groupInfo.setEstablish(false); // 不是新建群组
        groupInfo.setExist(true);
        
        // 创建封装信息对象
        encap_info info = new encap_info();
        info.set_type(1); // 群组消息类型
        info.set_group_info(groupInfo);
        
        // 发送更新群组请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success && chatView != null) {
            chatView.showError("添加用户到群组失败");
        }
    }
    
    /*
        从群组移除用户
    */
    public void removeUserFromGroup(int groupId, String username) {
        // 获取当前群组信息
        Group_info currentGroup = model.getGroups().get(groupId);
        if (currentGroup == null) {
            chatView.showError("群组不存在");
            return;
        }
        
        // 创建新的成员列表
        ArrayList<String> newMembers = new ArrayList<>(currentGroup.getMembers());
        
        // 检查用户是否在群组中
        if (!newMembers.contains(username)) {
            chatView.showError("用户不在群组中");
            return;
        }
        
        // 移除成员
        newMembers.remove(username);
        
        // 创建群组信息对象
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.set_Group_name(currentGroup.get_Group_name());
        groupInfo.setMembers(newMembers);
        groupInfo.setEstablish(false); // 不是新建群组
        groupInfo.setExist(true);
        
        // 创建封装信息对象
        encap_info info = new encap_info();
        info.set_type(1); // 群组消息类型
        info.set_group_info(groupInfo);
        
        // 发送更新群组请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success && chatView != null) {
            chatView.showError("从群组移除用户失败");
        }
    }
    
    /*
        处理新接收到的消息
    */
    public void onNewMessage(Chat_info chatInfo) {
        if (chatView != null) {
            chatView.displayMessage(formatChatMessage(chatInfo));
        }
    }
    
    /*
        处理群组创建成功事件
    */
    public void onGroupCreated(Group_info groupInfo) {
        if (chatView != null) {
            chatView.updateGroupList();
            chatView.showMessage("群组 \"" + groupInfo.get_Group_name() + "\" 创建成功");
        }
    }
    
    /*
        处理群组更新事件
    */
    public void onGroupUpdated(Group_info groupInfo) {
        if (chatView != null) {
            chatView.updateGroupList();
            chatView.showMessage("群组 \"" + groupInfo.get_Group_name() + "\" 已更新");
        }
    }
    
    /*
        处理被移出群组事件
    */
    public void onRemovedFromGroup(Group_info groupInfo) {
        if (chatView != null) {
            chatView.updateGroupList();
            chatView.showMessage("您已被移出群组 \"" + groupInfo.get_Group_name() + "\"");
        }
    }
    
    /*
        格式化聊天消息，添加时间戳和发送者信息
    */
    private String formatChatMessage(Chat_info chatInfo) {
        // 获取当前时间
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        
        // 根据消息类型构建不同的格式
        if (chatInfo.isType()) { // 群聊消息
            return String.format("[%s] %s (群聊): %s", 
                    timeStamp, 
                    chatInfo.getFrom_username(), 
                    chatInfo.getMessage());
        } else { // 私聊消息
            return String.format("[%s] %s (私聊): %s", 
                    timeStamp, 
                    chatInfo.getFrom_username(), 
                    chatInfo.getMessage());
        }
    }
    
    /*
        获取在线用户列表
    */
    public ArrayList<String> getOnlineUsers() {
        return model.getOnlineUsers();
    }
    
    /*
        获取当前用户的群组列表
    */
    public List<Group_info> getUserGroups() {
        // 从模型获取所有群组
        List<Group_info> userGroups = new ArrayList<>();
        for (Group_info group : model.getGroups().values()) {
            if (group.getMembers().contains(model.getCurrentUser())) {
                userGroups.add(group);
            }
        }
        return userGroups;
    }
    
    /*
        处理客户端退出逻辑
    */
    public void logout() {
        // 创建登出消息
        encap_info info = new encap_info();
        info.set_type(2); // 登出消息类型
        
        // 发送登出请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success && chatView != null) {
            chatView.showError("登出失败");
        }
        
        // 清除模型数据
        model.clear();
    }
    
    /*
        保存聊天记录
    */
    public void saveMessageHistory() {
        try {
            // 获取聊天历史
            List<Chat_info> messageHistory = model.getMessageHistory();
            
            // 如果没有消息，则不需要保存
            if (messageHistory.isEmpty()) {
                return;
            }
            
            // 创建保存目录
            java.io.File dir = new java.io.File("chat_history");
            if (!dir.exists()) {
                dir.mkdir();
            }
            
            // 创建文件名（使用当前用户名和时间戳）
            String fileName = "chat_history/" + model.getCurrentUser() + "_" 
                    + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".txt";
            
            // 创建文件输出流
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));
            
            // 写入聊天记录
            for (Chat_info message : messageHistory) {
                writer.println(formatChatMessage(message));
            }
            
            // 关闭文件
            writer.close();
            
            if (chatView != null) {
                chatView.showMessage("聊天记录已保存至 " + fileName);
            }
            
        } catch (IOException e) {
            if (chatView != null) {
                chatView.showError("保存聊天记录失败: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    /*
        通知服务器客户端将要断开连接
    */
    public void notifyServerDisconnect() {
        // 创建断开连接消息
        encap_info info = new encap_info();
        info.set_type(6); // 假设6为断开连接消息类型
        
        // 发送断开连接请求
        boolean success = IOStream.writeMessage(socket, info);
        
        if (!success) {
            // 断开连接时出错，但我们即将关闭应用程序，所以只记录错误
            System.err.println("通知服务器断开连接失败");
        }
    }
    
    /*
        执行退出前的所有清理工作
    */
    public void cleanup() {
        // 保存聊天记录
        saveMessageHistory();
        
        // 通知服务器断开连接
        notifyServerDisconnect();
        
        // 发送登出消息
        logout();
    }
} 