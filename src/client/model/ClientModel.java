package client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import info.Chat_info;
import info.Group_info;
import info.Org_info;

/*
    客户端数据模型，管理客户端的状态和数据
*/

public class ClientModel {
    // 当前登录的用户名
    private String currentUser;
    // 在线用户列表
    private ArrayList<String> onlineUsers;
    // 所有注册用户列表
    private ArrayList<String> allUsers;
    // 群组信息映射表 <群组ID, 群组信息>
    private Map<Integer, Group_info> groups;
    // 小组信息映射表 <群组ID, 群组信息>
    private Map<Integer, Org_info> orgs;
    private List<Org_info> pendingTeamInvitations;//待处理邀请
    // 聊天消息历史记录
    private List<Chat_info> messageHistory;
    // 最新的聊天消息
    private Chat_info lastChatMessage;
    // 登录状态
    private boolean loggedIn;
    
    // 未读消息映射表 <聊天对象ID, 未读消息列表>
    // 聊天对象ID格式：private:username 或 group:groupId
    private Map<String, List<Chat_info>> unreadMessages;
    
    // 观察者列表
    private List<ModelObserver> observers;
    
    /*
        构造函数，初始化数据结构
    */
    public ClientModel() {
        this.onlineUsers = new ArrayList<>();
        this.allUsers = new ArrayList<>();
        this.groups = new HashMap<>();
        this.orgs = new HashMap<>(); // 确保小组 map 被初始化
        this.pendingTeamInvitations = new ArrayList<>(); // 确保邀请 list 被初始化
        this.messageHistory = new ArrayList<>();
        this.unreadMessages = new HashMap<>();
        this.loggedIn = false;
        // 使用线程安全的列表存储观察者
        this.observers = new CopyOnWriteArrayList<>();
    }
    
    /*
        添加模型观察者
    */
    public void addObserver(ModelObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /*
        移除模型观察者
    */
    public void removeObserver(ModelObserver observer) {
        observers.remove(observer);
    }
    
    /*
        通知所有观察者数据已更新
    */
    private void notifyObservers(UpdateType updateType) {
        for (ModelObserver observer : observers) {
            observer.onModelUpdate(updateType);
        }
    }
    
    /*
        设置当前用户
    */
    public void setCurrentUser(String username) {
        System.out.println("设置当前用户: " + username + " (原用户: " + this.currentUser + ")");
        this.currentUser = username;
        notifyObservers(UpdateType.CURRENT_USER);
    }
    
    /*
        获取当前用户
    */
    public String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 获取当前用户名（别名方法，与ChatController保持一致）
     */
    public String getCurrentUsername() {
        return currentUser;
    }
    
    /*
        更新在线用户列表
    */
    public void setOnlineUsers(ArrayList<String> users) {
        // 添加空值检查，避免NullPointerException
        if (users != null) {
            this.onlineUsers = new ArrayList<>(users);
        } else {
            this.onlineUsers = new ArrayList<>();
        }
        notifyObservers(UpdateType.USERS);
    }
    
    /*
        获取在线用户列表
    */
    public ArrayList<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers);
    }
    
    /*
        更新群组信息
    */
    public void updateGroup(Group_info groupInfo) {
        groups.put(groupInfo.get_Group_id(), groupInfo);
        notifyObservers(UpdateType.GROUPS);
    }
    
    /*
        获取所有群组信息
    */
    public Map<Integer, Group_info> getGroups() {
        return new HashMap<>(groups);
    }

    public void updateGOrg(Org_info orgInfo) {//更新小组信息
        orgs.put(orgInfo.getOrg_id(), orgInfo);
        notifyObservers(UpdateType.ORGANIZATIONS);
    }

    public void removeOrg(int orgId) {
        groups.remove(orgId);
        notifyObservers(UpdateType.ORGANIZATIONS);
    }
    public Map<Integer, Org_info> getOrgs() {
        return new HashMap<>(orgs);
    }
    /*
        添加聊天消息到历史记录
    */
    public void addMessage(Chat_info message) {
        // 添加到历史记录
        messageHistory.add(message);
        lastChatMessage = message;
        
        // 如果消息不是当前用户发送的，则添加到未读消息列表
        if (!message.getFrom_username().equals(currentUser)) {
            String chatId;
            if (message.isType()) {
                // 群聊消息
                chatId = "group:" + message.getGroup_id();
            } else {
                // 私聊消息
                chatId = "private:" + message.getFrom_username();
            }
            
            // 获取或创建未读消息列表
            List<Chat_info> unread = unreadMessages.getOrDefault(chatId, new ArrayList<>());
            unread.add(message);
            unreadMessages.put(chatId, unread);
        }
        
        notifyObservers(UpdateType.CHAT);
    }
    
    /*
        获取聊天消息历史记录
    */
    public List<Chat_info> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * 获取最新的聊天消息
     */
    public Chat_info getLastChatMessage() {
        return lastChatMessage;
    }
    
    /**
     * 获取指定聊天对象的未读消息列表
     * @param isGroupChat 是否为群聊
     * @param targetId 目标ID（用户名或群组ID）
     * @return 未读消息列表
     */
    public List<Chat_info> getUnreadMessages(boolean isGroupChat, String targetId) {
        String chatId = (isGroupChat ? "group:" : "private:") + targetId;
        List<Chat_info> unread = unreadMessages.get(chatId);
        return unread != null ? new ArrayList<>(unread) : new ArrayList<>();
    }
    
    /**
     * 将指定聊天对象的消息标记为已读
     * @param isGroupChat 是否为群聊
     * @param targetId 目标ID（用户名或群组ID）
     */
    public void markMessagesAsRead(boolean isGroupChat, String targetId) {
        String chatId = (isGroupChat ? "group:" : "private:") + targetId;
        if (unreadMessages.containsKey(chatId) && !unreadMessages.get(chatId).isEmpty()) {
            unreadMessages.remove(chatId);
            // 通知观察者更新界面
            notifyObservers(isGroupChat ? UpdateType.GROUPS : UpdateType.USERS);
        }
    }
    
    /**
     * 检查指定聊天对象是否有未读消息
     * @param isGroupChat 是否为群聊
     * @param targetId 目标ID（用户名或群组ID）
     * @return 是否有未读消息
     */
    public boolean hasUnreadMessages(boolean isGroupChat, String targetId) {
        String chatId = (isGroupChat ? "group:" : "private:") + targetId;
        List<Chat_info> unread = unreadMessages.get(chatId);
        return unread != null && !unread.isEmpty();
    }
    
    /*
        设置登录状态
    */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
        notifyObservers(UpdateType.LOGIN_STATUS);
    }
    
    /*
        获取登录状态
    */
    public boolean isLoggedIn() {
        return loggedIn;
    }
    
    /*
        更新所有注册用户列表
    */
    public void setAllUsers(ArrayList<String> users) {
        // 添加空值检查，避免NullPointerException
        if (users != null) {
            this.allUsers = new ArrayList<>(users);
        } else {
            this.allUsers = new ArrayList<>();
        }
        notifyObservers(UpdateType.ALL_USERS);
    }
    
    /*
        获取所有注册用户列表
    */
    public ArrayList<String> getAllUsers() {
        return new ArrayList<>(allUsers);
    }
    /**
     * 获取待处理的小组邀请
     */
    public List<Org_info> getPendingTeamInvitations() {
        return new ArrayList<>(pendingTeamInvitations);
    }

    /**
     * 添加一个新的待处理小组邀请
     * @param invitation 邀请信息
     */
    public void addPendingTeamInvitation(Org_info invitation) {
        // 防止重复添加
        for (Org_info existing : pendingTeamInvitations) {
            if (existing.getOrg_id() == invitation.getOrg_id()) {
                return;
            }
        }
        this.pendingTeamInvitations.add(invitation);
        notifyObservers(UpdateType.TEAM_INVITATIONS);
    }

    /**
     * 根据小组ID移除一个待处理的邀请
     * @param orgId 小组ID
     */
    public void removePendingTeamInvitation(int orgId) {
        this.pendingTeamInvitations.removeIf(inv -> inv.getOrg_id() == orgId);
        notifyObservers(UpdateType.TEAM_INVITATIONS);
    }
    /*
        登出时，需要清除所有数据
    */
    public void clear() {
        currentUser = null;
        onlineUsers.clear();
        allUsers.clear();
        clearGroups();
        clearOrgs();
        orgs.clear();
        pendingTeamInvitations.clear();
        messageHistory.clear();
        lastChatMessage = null;
        loggedIn = false;
        unreadMessages.clear();
        notifyObservers(UpdateType.ALL);
    }
    
    /**
     * 清除所有群组信息
     */
    public void clearGroups() {
        groups.clear();
        notifyObservers(UpdateType.GROUPS);
    }
    public void clearOrgs() {
        if (orgs != null) {
            orgs.clear();
        }
        notifyObservers(UpdateType.ORGANIZATIONS);
    }
    /*
        模型的所有更新类型
    */
    public enum UpdateType {
        CURRENT_USER,  // 当前用户更新
        USERS,         // 在线用户列表更新
        ALL_USERS,     // 所有注册用户列表更新
        GROUPS,        // 群组信息更新
        ORGANIZATIONS, // 小组类型更新
        TEAM_INVITATIONS, // 小组邀请更新
        CHAT,          // 聊天消息更新
        LOGIN_STATUS,  // 登录状态更新
        ALL           // 全部更新
    }
    
    /*
        模型观察者接口
    */
    public interface ModelObserver {
        // 模型数据更新时调用
        void onModelUpdate(UpdateType updateType);
    }
} 