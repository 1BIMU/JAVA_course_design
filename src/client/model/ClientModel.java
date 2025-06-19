package client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import info.Chat_info;
import info.Group_info;

/*
    客户端数据模型，管理客户端的状态和数据
*/

/*
    一些给服务端的说明：
        主要功能包括：管理当前用户信息、在线用户列表、群组信息和消息历史
        对于所有需要管理的数据提供了获取和修改数据的方法
        一些数据结构待定，比如管理群聊的
        我使用了观察者模式，当数据变化时通知观察者
        使用 CopyOnWriteArrayList 存储观察者是为了保证线程安全

        对客户端的数据进行了封装，所有数据都是私有的，只能通过公共方法访问
        返回给其他类的数据都是深拷贝的，避免外部类错误修改数据
*/

public class ClientModel {
    // 当前登录的用户名
    private String currentUser;
    // 在线用户列表
    private ArrayList<String> onlineUsers;
    // 所有注册用户列表
    private ArrayList<String> allUsers;
    // 群组信息映射表 <群组ID, 群组信息>
    private Map<Integer, Group_info> groups;   //TODO: 这里的具体逻辑待服务端实现
    // 聊天消息历史记录
    private List<Chat_info> messageHistory;   //TODO:这里的具体逻辑待服务端实现
    // 最新的聊天消息
    private Chat_info lastChatMessage;
    // 登录状态
    private boolean loggedIn;
    
    // 观察者列表
    private List<ModelObserver> observers;
    
    /*
        构造函数，初始化数据结构
    */
    public ClientModel() {
        this.onlineUsers = new ArrayList<>();
        this.allUsers = new ArrayList<>();
        this.groups = new HashMap<>();
        this.messageHistory = new ArrayList<>();
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
        TODO: 更新群组信息（待实现）
    */
    public void updateGroup(Group_info groupInfo) {
        groups.put(groupInfo.get_Group_id(), groupInfo);
        notifyObservers(UpdateType.GROUPS);
    }
    
    /*
        TODO: 移除群组（待实现）
    */
    public void removeGroup(int groupId) {
        groups.remove(groupId);
        notifyObservers(UpdateType.GROUPS);
    }
    
    /*
        TODO: 获取所有群组信息（待实现）
    */
    public Map<Integer, Group_info> getGroups() {
        return new HashMap<>(groups);
    }
    
    /*
        TODO: 添加聊天消息到历史记录（待实现）
    */
    public void addMessage(Chat_info message) {
        messageHistory.add(message);
        lastChatMessage = message;
        notifyObservers(UpdateType.CHAT);
    }
    
    /*
        TODO: 获取聊天消息历史记录（待实现）
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
    
    /*
        登出时，需要清除所有数据
    */
    public void clear() {
        currentUser = null;
        onlineUsers.clear();
        allUsers.clear();
        groups.clear();
        messageHistory.clear();
        lastChatMessage = null;
        loggedIn = false;
        notifyObservers(UpdateType.ALL);
    }
    
    /*
        模型的所有更新类型
    */
    public enum UpdateType {
        CURRENT_USER,  // 当前用户更新
        USERS,         // 在线用户列表更新
        ALL_USERS,     // 所有注册用户列表更新
        GROUPS,        // 群组信息更新
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