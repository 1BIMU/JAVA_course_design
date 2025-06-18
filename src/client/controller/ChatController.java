package client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import client.MessageSender;
import client.model.ClientModel;
import client.view.ChatView;
import info.Chat_info;
import info.Group_info;
import io.FileIO;

/*
    一些说明：通过视图中的接口与视图组件进行交互
*/

/*
    聊天控制器，处理聊天相关的业务逻辑
*/
public class ChatController {
    private ClientModel model;
    private ChatView chatView;
    private MessageSender messageSender; // 替代原来的Socket
    
    /*
        构造函数
    */
    public ChatController(ClientModel model, MessageSender messageSender) {
        this.model = model;
        this.messageSender = messageSender;
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
            System.out.println("发送消息 - 当前用户: " + model.getCurrentUser());
            // 使用MessageSender发送聊天消息
            boolean success = messageSender.sendChatMessage(
                model.getCurrentUser(), message, isGroupChat, targetId);
            
            if (success) {
                // 创建聊天信息对象用于本地历史记录
                Chat_info chatInfo = new Chat_info();
                chatInfo.setType(isGroupChat);
                chatInfo.setFrom_username(model.getCurrentUser());
                chatInfo.setText(message);
                
                if (isGroupChat) {
                    chatInfo.setGroup_id(Integer.parseInt(targetId));
                } else {
                    chatInfo.setTo_username(targetId);
                }
                
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
        
        // 使用MessageSender发送创建群组请求
        boolean success = messageSender.sendCreateGroupRequest(groupName, members);
        
        if (!success && chatView != null) {
            chatView.showError("创建群组失败");
        }
    }
    
    /*
        退出群组
    */
    public void leaveGroup(int groupId) {
        // 使用MessageSender发送退出群组请求
        boolean success = messageSender.sendLeaveGroupRequest(groupId);
        
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
        
        // 使用MessageSender发送更新群组请求
        boolean success = messageSender.sendUpdateGroupRequest(
            groupId, currentGroup.get_Group_name(), newMembers);
        
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
        
        // 使用MessageSender发送更新群组请求
        boolean success = messageSender.sendUpdateGroupRequest(
            groupId, currentGroup.get_Group_name(), newMembers);
        
        if (!success && chatView != null) {
            chatView.showError("从群组移除用户失败");
        }
    }
    
    /*
        处理新接收到的消息
    */
    public void onNewMessage(Chat_info chatInfo) {
        // 检查这条消息是否是由当前用户发送的
        // 如果是当前用户发送的消息，不再添加到模型中，因为在sendMessage方法中已经添加过了
        boolean isFromCurrentUser = chatInfo.getFrom_username().equals(model.getCurrentUser());
        
        if (!isFromCurrentUser) {
            // 只有当消息不是由当前用户发送的时候才添加到模型
            // 这样可以避免消息被添加两次
            model.addMessage(chatInfo);
            saveMessageToFile(chatInfo);
        } else {
            System.out.println("跳过添加当前用户的消息，避免重复显示");
        }     
    }
    
    /**
     * 实时保存单条聊天消息到文件
     * @param chatInfo 聊天消息信息
     */
    private void saveMessageToFile(Chat_info chatInfo) {
        try {
            FileIO fileIO = new FileIO();
            fileIO.saveChatMessage(chatInfo);
            System.out.println("聊天记录已实时保存");
        } catch (IOException e) {
            System.err.println("保存聊天记录失败: " + e.getMessage());
            e.printStackTrace();
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
                    chatInfo.getText());
        } else { // 私聊消息
            return String.format("[%s] %s (私聊): %s", 
                    timeStamp, 
                    chatInfo.getFrom_username(), 
                    chatInfo.getText());
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
        // 使用MessageSender发送登出请求
        boolean success = messageSender.sendLogoutRequest();
        
        if (!success && chatView != null) {
            chatView.showError("登出失败");
        }
        
        // 清除模型数据
        model.clear();
    }
    
    /*
        通知服务器客户端将要断开连接
    */
    public void notifyServerDisconnect() {
        // 使用MessageSender发送断开连接通知
        boolean success = messageSender.sendDisconnectNotification();
        
        if (!success) {
            // 断开连接时出错，但我们即将关闭应用程序，所以只记录错误
            System.err.println("通知服务器断开连接失败");
        }
    }
    
    /*
        执行退出前的所有清理工作
    */
    public void cleanup() {
        // 通知服务器断开连接
        notifyServerDisconnect();
        
        // 发送登出消息
        logout();
    }
    
    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        return model.getCurrentUsername();
    }
    
    /**
     * 发送私聊消息
     * @param targetUser 目标用户
     * @param message 消息内容
     */
    public void sendPrivateMessage(String targetUser, String message) {
        sendMessage(message, false, targetUser);
    }
    
    /**
     * 发送群聊消息
     * @param groupId 群组ID
     * @param message 消息内容
     */
    public void sendGroupMessage(String groupId, String message) {
         sendMessage(message, true, groupId);
    }
    
    /**
     * 加载聊天历史记录
     * @param chatInfo 聊天信息
     * @param callback 历史记录回调
     */
    public void loadChatHistory(Chat_info chatInfo, HistoryCallback callback) {
        // 创建新线程加载历史记录
        new Thread(() -> {
            try {
                FileIO fileIO = new FileIO();
                List<String> history = fileIO.getChatHistory(chatInfo);
                callback.onHistoryLoaded(history);
            } catch (IOException e) {
                e.printStackTrace();
                callback.onHistoryLoaded(Collections.emptyList());
            }
        }).start();
    }
    
    /**
     * 历史记录回调接口
     */
    public interface HistoryCallback {
        void onHistoryLoaded(List<String> history);
    }
} 