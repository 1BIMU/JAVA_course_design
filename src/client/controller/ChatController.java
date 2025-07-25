package client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import client.MessageSender;
import client.model.ClientModel;
import client.view.ChatView;
import client.view.ContactListView;
import client.view.CreateTeamDialog;
import client.view.TeamInvitationsView;
import info.Chat_info;
import info.Group_info;
import info.Org_info;
import io.FileIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
    聊天控制器，处理聊天相关的业务逻辑
*/
public class ChatController {
    private ClientModel model;
    // 修改为Map，用于管理多个聊天窗口
    private Map<String, ChatView> chatViews = new HashMap<>();
    private MessageSender messageSender; // 替代原来的Socket
    private VoiceCallController voiceCallController; // 语音通话控制器
    private ContactListView contactListView; // 新增一个对主窗口的引用
    /*
        构造函数
    */
    public ChatController(ClientModel model, MessageSender messageSender) {
        this.model = model;
        this.messageSender = messageSender;
    }
    //添加引用
    public void setContactListView(ContactListView contactListView) {
        this.contactListView = contactListView;
    }

    /**
     * 显示创建小组对话框
     */
    public void showCreateTeamDialog() {
        if (contactListView != null) {
            CreateTeamDialog dialog = new CreateTeamDialog(contactListView, this, model);
            dialog.setVisible(true);
        }
    }

    /**
     * 显示小组邀请视图
     */
    public void showTeamInvitationsView() {
        if (contactListView != null) {
            TeamInvitationsView view = new TeamInvitationsView(contactListView, this, model);
            view.setVisible(true);
        }
    }

    /**
     * 创建小组的逻辑
     * @param parentGroupId 父级群聊ID
     * @param teamName 小组名称
     * @param members 成员列表
     */
    public void createTeam(int parentGroupId, String teamName, List<String> members) {
        // 调用你已有的 messageSender 方法
        messageSender.sendEstablishOrgMessage(new ArrayList<>(members), model.getCurrentUser(), parentGroupId, teamName);
    }

    /**
     * 响应小组邀请
     * @param orgId 小组ID
     * @param accepted 是否接受
     */
    public void respondToTeamInvitation(int orgId, boolean accepted) {
        // 从待处理列表中找到邀请信息
        Org_info invitation = model.getPendingTeamInvitations().stream()
                .filter(inv -> inv.getOrg_id() == orgId)
                .findFirst().orElse(null);

        if (invitation == null) return;

        // 从模型中移除该邀请，UI会自动更新
        model.removePendingTeamInvitation(orgId);

        if (accepted) {
            // 调用你已有的 messageSender 方法
            messageSender.sendOrgAckMessage(model.getCurrentUser(), invitation.getGroup_id(), orgId);
        }
    }
    /*
        设置聊天视图
    */
    public void setChatView(ChatView chatView) {
        if (chatView == null) {
            return; // 忽略null值
        }
        
        // 根据聊天类型和目标ID生成唯一键
        String key = getChatViewKey(chatView.isGroupChat(), chatView.getTargetId());
        chatViews.put(key, chatView);
    }
    
    /*
        移除聊天视图
    */
    public void removeChatView(ChatView chatView) {
        if (chatView == null) {
            return;
        }
        
        String key = getChatViewKey(chatView.isGroupChat(), chatView.getTargetId());
        chatViews.remove(key);
    }
    
    /*
        生成聊天窗口的唯一键
    */
    private String getChatViewKey(boolean isGroupChat, String targetId) {
        return (isGroupChat ? "group:" : "private:") + targetId;
    }
    
    /*
        发送聊天消息
    */
    public void sendMessage(String message, boolean isGroupChat, String targetId) {
        // 输入验证
        if (message == null || message.trim().isEmpty()) {
            String key = getChatViewKey(isGroupChat, targetId);
            ChatView chatView = chatViews.get(key);
            if (chatView != null) {
                chatView.showError("消息不能为空");
            }
            return;
        }
        
        try {
            System.out.println("发送消息 - 当前用户: " + model.getCurrentUser());
            // 使用MessageSender发送聊天消息
            boolean success = messageSender.sendChatMessage(
                model.getCurrentUser(), message, isGroupChat, targetId);
            
            if (success) {
                // 消息已发送到服务器，等待服务器回复
                // 不再在本地添加消息到模型，而是等待服务器的响应
                
                // 清空输入框
                String key = getChatViewKey(isGroupChat, targetId);
                ChatView chatView = chatViews.get(key);
                if (chatView != null) {
                    chatView.clearMessageInput();
                }
            } else {
                String key = getChatViewKey(isGroupChat, targetId);
                ChatView chatView = chatViews.get(key);
                if (chatView != null) {
                    chatView.showError("发送消息失败，请检查网络连接");
                }
            }
        } catch (NumberFormatException e) {
            String key = getChatViewKey(isGroupChat, targetId);
            ChatView chatView = chatViews.get(key);
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
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("群组名称不能为空");
            }
            return;
        }
        if (members == null || members.isEmpty()) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("群组成员不能为空");
            }
            return;
        }
        
        // 确保当前用户在群组中
        if (!members.contains(model.getCurrentUser())) {
            members.add(model.getCurrentUser());
        }
        
        // 使用MessageSender发送创建群组请求
        boolean success = messageSender.sendCreateGroupRequest(groupName, members);
        
        if (!success) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("创建群组失败");
            }
        }
    }
    
    /*
        处理新接收到的消息
    */
    public void onNewMessage(Chat_info chatInfo) {
        // 检查这条消息是否是由当前用户发送的
        boolean isFromCurrentUser = chatInfo.getFrom_username().equals(model.getCurrentUser());
        
        // 检查消息是否发送失败
        if (isFromCurrentUser && !chatInfo.getTransfer_status()) {
            // 消息发送失败，显示提示框
            String targetUser = chatInfo.getTo_username();
            // 获取对应的聊天窗口
            String key = getChatViewKey(false, targetUser);
            ChatView chatView = chatViews.get(key);
            
            if (chatView != null) {
                chatView.showError("用户 " + targetUser + " 当前不在线，消息发送失败");
            } else {
                // 如果没有找到对应的聊天窗口，可以考虑在所有打开的窗口中显示错误
                for (ChatView view : chatViews.values()) {
                    view.showError("用户 " + targetUser + " 当前不在线，消息发送失败");
                    break; // 只在一个窗口显示即可
                }
            }
            // 不将失败的消息添加到模型中
            return;
        }
        
        if (!isFromCurrentUser) {
            // 只有当消息不是由当前用户发送的时候才添加到模型并保存到文件
            model.addMessage(chatInfo);
            saveMessageToFile(chatInfo);
        } else if (chatInfo.getTransfer_status()) {
            // 如果是当前用户发送的消息且发送成功，只添加到模型，不重复保存到文件
            // 因为服务器会将消息转发回来，此时会再次触发onNewMessage
            model.addMessage(chatInfo);
            System.out.println("消息发送成功，添加到模型");
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
        for (ChatView chatView : chatViews.values()) {
            chatView.updateGroupList();
            chatView.showMessage("群组 \"" + groupInfo.get_Group_name() + "\" 创建成功");
        }
    }
    
    /*
        处理群组更新事件
    */
    public void onGroupUpdated(Group_info groupInfo) {
        for (ChatView chatView : chatViews.values()) {
            chatView.updateGroupList();
            chatView.showMessage("群组 \"" + groupInfo.get_Group_name() + "\" 已更新");
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
        
        if (!success) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("登出失败");
            }
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
     * 新增：发送小组聊天消息
     * @param teamId 小组ID
     * @param message 消息内容
     */
    public void sendTeamMessage(String teamId, String message) {
        // 直接调用 MessageSender 的新方法
        boolean success = messageSender.sendTeamChatMessage(model.getCurrentUser(), message, teamId);
        if (success) {
            // 清空输入框等后续处理
            String key = "team:" + teamId; // 使用一个唯一的key
            ChatView chatView = chatViews.get(key);
            if (chatView != null) {
                chatView.clearMessageInput();
            }
        }
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

    public void InviteAgreement(Org_info org_info) {
        int org_id = org_info.getOrg_id();
        int group_id = org_info.getGroup_id();
        messageSender.sendOrgAckMessage(getCurrentUsername(), org_id, group_id);
    }

    /**
     * 历史记录回调接口
     */
    public interface HistoryCallback {
        void onHistoryLoaded(List<String> history);
    }
    
    /**
     * 发送私聊文件
     * @param fromUser 发送者用户名
     * @param toUser 接收者用户名
     * @param file 要发送的文件
     * @param description 文件描述
     * @return 是否发送成功
     */
    public boolean sendPrivateFile(String fromUser, String toUser, File file, String description) {
        // 输入验证
        if (file == null || !file.exists() || !file.isFile()) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("文件不存在或无效");
            }
            return false;
        }
        
        // 使用MessageSender发送文件
        boolean success = messageSender.sendPrivateFile(fromUser, toUser, file, description);
        
        if (!success) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("发送文件失败，请检查网络连接");
            }
        }
        
        return success;
    }
    
    /**
     * 发送群聊文件
     * @param fromUser 发送者用户名
     * @param groupId 群组ID
     * @param file 要发送的文件
     * @param description 文件描述
     * @return 是否发送成功
     */
    public boolean sendGroupFile(String fromUser, int groupId, File file, String description) {
        // 输入验证
        if (file == null || !file.exists() || !file.isFile()) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("文件不存在或无效");
            }
            return false;
        }
        
        // 使用MessageSender发送文件
        boolean success = messageSender.sendGroupFile(fromUser, groupId, file, description);
        
        if (!success) {
            for (ChatView chatView : chatViews.values()) {
                chatView.showError("发送群文件失败，请检查网络连接");
            }
        }
        
        return success;
    }
    
    /**
     * 发送私聊图片
     * @param username 接收者用户名
     */
    public void sendPrivateImage(String username) {
        if (username == null || username.isEmpty()) {
            // 获取对应的聊天窗口
            String key = getChatViewKey(false, username);
            ChatView chatView = chatViews.get(key);
            
            if (chatView != null) {
                chatView.showError("请先选择聊天对象");
            }
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的图片");
        // 设置文件过滤器，只显示图片文件
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "图片文件", "jpg", "jpeg", "png", "gif", "bmp");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 检查文件是否为图片
            if (!selectedFile.exists() || !selectedFile.isFile()) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(false, username);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("所选文件不存在或不是有效文件");
                }
                return;
            }
            
            // 检查文件扩展名
            String fileName = selectedFile.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
                !fileName.endsWith(".png") && !fileName.endsWith(".gif") && 
                !fileName.endsWith(".bmp")) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(false, username);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("请选择有效的图片文件 (jpg, jpeg, png, gif, bmp)");
                }
                return;
            }
            
            // 询问图片描述
            String description = JOptionPane.showInputDialog(null, 
                "请输入图片描述（可选）:", "图片描述", JOptionPane.QUESTION_MESSAGE);
            
            // 发送图片
            boolean success = messageSender.sendPrivateImage(
                model.getCurrentUsername(), username, selectedFile, description);
            
            if (!success) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(false, username);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("发送图片失败，请重试");
                }
            }
        }
    }
    
    /**
     * 发送群聊图片
     * @param groupId 群组ID
     */
    public void sendGroupImage(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            // 获取对应的聊天窗口
            String key = getChatViewKey(true, groupId);
            ChatView chatView = chatViews.get(key);
            
            if (chatView != null) {
                chatView.showError("请先选择聊天群组");
            }
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的图片");
        // 设置文件过滤器，只显示图片文件
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "图片文件", "jpg", "jpeg", "png", "gif", "bmp");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 检查文件是否为图片
            if (!selectedFile.exists() || !selectedFile.isFile()) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(true, groupId);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("所选文件不存在或不是有效文件");
                }
                return;
            }
            
            // 检查文件扩展名
            String fileName = selectedFile.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
                !fileName.endsWith(".png") && !fileName.endsWith(".gif") && 
                !fileName.endsWith(".bmp")) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(true, groupId);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("请选择有效的图片文件 (jpg, jpeg, png, gif, bmp)");
                }
                return;
            }
            
            // 询问图片描述
            String description = JOptionPane.showInputDialog(null, 
                "请输入图片描述（可选）:", "图片描述", JOptionPane.QUESTION_MESSAGE);
            
            try {
                int groupIdInt = Integer.parseInt(groupId);
                // 发送图片
                boolean success = messageSender.sendGroupImage(
                    model.getCurrentUsername(), groupIdInt, selectedFile, description);
                
                if (!success) {
                    // 获取对应的聊天窗口
                    String key = getChatViewKey(true, groupId);
                    ChatView chatView = chatViews.get(key);
                    
                    if (chatView != null) {
                        chatView.showError("发送图片失败，请重试");
                    }
                }
            } catch (NumberFormatException e) {
                // 获取对应的聊天窗口
                String key = getChatViewKey(true, groupId);
                ChatView chatView = chatViews.get(key);
                
                if (chatView != null) {
                    chatView.showError("群组ID格式错误");
                }
            }
        }
    }
    
    /**
     * 获取语音通话控制器
     * @return 语音通话控制器
     */
    public VoiceCallController getVoiceCallController() {
        if (voiceCallController == null) {
            voiceCallController = new VoiceCallController(model, messageSender);
        }
        return voiceCallController;
    }
    
    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 群组成员列表
     */
    public List<String> getGroupMembers(String groupId) {
        try {
            // 尝试解析群组ID
            int id = Integer.parseInt(groupId);
            
            // 从模型中获取群组信息
            Group_info group = model.getGroups().get(id);
            if (group != null) {
                return new ArrayList<>(group.getMembers());
            } else {
                // 如果模型中没有群组信息，尝试从文件中读取
                try {
                    FileIO fileIO = new FileIO();
                    return fileIO.getGroupMembers(id);
                } catch (IOException e) {
                    System.err.println("获取群组成员失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("群组ID格式错误: " + groupId);
        }
        
        return Collections.emptyList();
    }
} 