package client.handler;

import client.controller.LoginController;
import client.model.ClientModel;
import info.Group_info;
import info.Login_info;
import info.encap_info;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * 客户端登录消息处理器
 */
public class LoginMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private LoginController loginController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param loginController 登录控制器
     */
    public LoginMessageHandler(ClientModel model, LoginController loginController) {
        this.model = model;
        this.loginController = loginController;
    }
    
    @Override
    public void handle(encap_info message) {
        Login_info loginInfo = message.get_login_info();
        if (loginInfo == null) return;
        if (loginInfo.isKicked()){
            JOptionPane.showMessageDialog(
                    null,                     // 父组件（null表示居中显示）
                    "您已在其他设备登录",      // 错误消息内容
                    "错误",                   // 窗口标题
                    JOptionPane.ERROR_MESSAGE // 消息类型（显示错误图标）
            );
            System.exit(0);
            return;
        }
        System.out.println("收到登录消息: " + 
            "用户名=" + loginInfo.getUserName() + 
            ", 成功标志=" + loginInfo.getLoginSuccessFlag() +
            ", 当前用户=" + model.getCurrentUser());
        
        // 更新在线用户列表 - 这个对所有登录消息都需要做
        // 添加空值检查，避免NullPointerException
        ArrayList<String> onlineUsers = loginInfo.getOnlineUsers();
        if (onlineUsers != null) {
            model.setOnlineUsers(onlineUsers);
        }
        
        // 更新所有注册用户列表 - 如果有的话
        ArrayList<String> allUsers = loginInfo.getAllUsers();
        if (allUsers != null) {
            model.setAllUsers(allUsers);
        }
        
        // 检查是初次登录消息还是更新消息   
        if (model.getCurrentUser() == null || !model.isLoggedIn()) {
            // 初次登录消息处理
            if (loginInfo.getLoginSuccessFlag()) {
                // 设置当前用户
                model.setCurrentUser(loginInfo.getUserName());
                model.setLoggedIn(true);
                
                // 处理群组信息 - 处理服务器发送的群组列表
                processGroupInfo(loginInfo);
                
                // 通知登录控制器登录成功
                loginController.onLoginSuccess();
            } else {
                // 登录失败，不设置当前用户，只通知登录失败
                loginController.onLoginFailure("用户名或密码错误");
            }
        } else if (model.getCurrentUser().equals(loginInfo.getUserName())) {
            // 这是对当前用户的确认消息，只需确认登录状态
            if (loginInfo.getLoginSuccessFlag()) {
                model.setLoggedIn(true);
                
                // 处理群组信息 - 也需要处理服务器可能发送的群组列表更新
                processGroupInfo(loginInfo);
            }
        } else {
            // 这是其他用户的登录通知，只更新在线用户列表，不改变当前用户
            // 已经在上面更新了在线用户列表，这里无需额外操作
        }
    }
    
    /**
     * 处理服务器发送的群组信息
     * @param loginInfo 登录信息对象
     */
    private void processGroupInfo(Login_info loginInfo) {
        // 获取群组ID列表
        ArrayList<Integer> groupIDList = loginInfo.getGroupIDList();
        Map<Integer, ArrayList<String>> groupMap = loginInfo.getGroupMap();
        Map<Integer, String> groupNameMap = loginInfo.getGroupNameMap();
        
        if (groupIDList != null && groupMap != null) {
            System.out.println("收到群组信息：" + groupIDList.size() + " 个群组");
            
            // 清除现有群组信息，准备更新
            model.clearGroups();
            
            // 处理每个群组信息
            for (Integer groupId : groupIDList) {
                ArrayList<String> members = groupMap.get(groupId);
                if (members != null) {
                    // 创建群组信息对象
                    Group_info groupInfo = new Group_info();
                    groupInfo.set_Group_id(groupId);
                    
                    // 使用服务器提供的群组名称
                    String groupName;
                    if (groupNameMap != null && groupNameMap.containsKey(groupId)) {
                        groupName = groupNameMap.get(groupId);
                    } else {
                        // 如果没有提供名称，使用默认名称
                        groupName = "群聊 " + groupId;
                        if (!members.isEmpty()) {
                            groupName = "用户 " + members.get(0) + " 的群组";
                        }
                    }
                    
                    groupInfo.set_Group_name(groupName);
                    groupInfo.setMembers(members);
                    groupInfo.setEstablish(false); // 非新建群组
                    groupInfo.setExist(true); // 群组存在
                    
                    // 更新到模型中
                    model.updateGroup(groupInfo);
                    System.out.println("添加群组: ID=" + groupId + ", 名称=" + groupName + ", 成员=" + members.size());
                }
            }
        } else {
            System.out.println("没有收到群组信息或群组信息为空");
        }
    }
} 