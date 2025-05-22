package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.controller.ChatController;
import client.model.ClientModel;
import client.model.ClientModel.ModelObserver;
import client.model.ClientModel.UpdateType;
import info.Group_info;

/*
    聊天视图，提供聊天界面和用户交互
*/
public class ChatView extends JFrame implements ModelObserver {
    private static final long serialVersionUID = 1L;
    
    // 控制器引用
    private ChatController controller;
    
    // 界面组件
    private JTextArea messageDisplay;
    private JTextField messageInput;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<GroupItem> groupList;
    private DefaultListModel<GroupItem> groupListModel;
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;
    
    // 当前聊天状态
    private boolean isGroupChat = false;
    private String currentTarget = null;  // 当前聊天对象(用户名或群组ID)
    private String currentTargetName = null;  // 当前聊天对象名称
    
    /*
        构造函数
    */
    public ChatView(ChatController controller) {
        this.controller = controller;
        
        // 设置窗口属性
        setTitle("多人聊天室");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        try {
            // 设置界面风格
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 初始化界面
        initComponents();
        
        // 添加窗口关闭事件处理
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 在这里添加退出前的清理工作
                controller.cleanup();
                System.exit(0);
            }
        });
    }
    
    /*
        初始化界面组件
    */
    private void initComponents() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setDividerSize(5);
        splitPane.setResizeWeight(0.7);
        
        // 创建聊天面板
        JPanel chatPanel = createChatPanel();
        
        // 创建用户/群组面板
        JPanel userGroupPanel = createUserGroupPanel();
        
        // 添加到分割面板
        splitPane.setLeftComponent(chatPanel);
        splitPane.setRightComponent(userGroupPanel);
        
        // 创建状态栏
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 组装主面板
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // 添加到窗口
        setContentPane(mainPanel);
    }
    
    /*
        创建聊天面板
    */
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 创建消息显示区域
        messageDisplay = new JTextArea();
        messageDisplay.setEditable(false);
        messageDisplay.setLineWrap(true);
        messageDisplay.setWrapStyleWord(true);
        messageDisplay.setFont(new Font("宋体", Font.PLAIN, 14));
        messageDisplay.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane messageScrollPane = new JScrollPane(messageDisplay);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // 创建消息输入区域
        messageInput = new JTextField();
        messageInput.setFont(new Font("宋体", Font.PLAIN, 14));
        messageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // 创建发送按钮
        sendButton = new JButton("发送");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // 创建聊天标题
        JLabel chatTitle = new JLabel("聊天消息");
        chatTitle.setFont(new Font("宋体", Font.BOLD, 16));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 组装面板
        panel.add(chatTitle, BorderLayout.NORTH);
        panel.add(messageScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /*
        创建用户/群组面板
    */
    private JPanel createUserGroupPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        
        // 创建在线用户面板
        JPanel userPanel = createUserListPanel();
        tabbedPane.addTab("在线用户", userPanel);
        
        // 创建群组面板
        JPanel groupPanel = createGroupListPanel();
        tabbedPane.addTab("我的群组", groupPanel);
        
        // 添加到面板
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /*
        创建用户列表面板
    */
    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 创建用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(new Font("宋体", Font.PLAIN, 14));
        
        // 添加选择监听器
        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null) {
                        // 切换到私聊模式
                        switchToPrivateChat(selectedUser);
                    }
                }
            }
        });
        
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // 组装面板
        panel.add(userScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /*
        创建群组列表面板
    */
    private JPanel createGroupListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 创建群组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setFont(new Font("宋体", Font.PLAIN, 14));
        
        // 设置自定义渲染器
        groupList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GroupItem) {
                    GroupItem item = (GroupItem) value;
                    setText(item.getName());
                }
                return this;
            }
        });
        
        // 添加选择监听器
        groupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    GroupItem selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        // 切换到群聊模式
                        switchToGroupChat(selectedGroup);
                    }
                }
            }
        });
        
        // 添加右键菜单
        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // 右键点击
                    int index = groupList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        groupList.setSelectedIndex(index);
                        GroupItem selectedGroup = groupList.getSelectedValue();
                        showGroupContextMenu(selectedGroup, e.getX(), e.getY());
                    }
                }
            }
        });
        
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton createGroupButton = new JButton("创建群组");
        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCreateGroupDialog();
            }
        });
        buttonPanel.add(createGroupButton);
        
        // 组装面板
        panel.add(groupScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /*
        显示创建群组对话框
    */
    private void showCreateGroupDialog() {
        // 创建对话框面板
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 群组名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("群组名称:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        JTextField groupNameField = new JTextField(20);
        panel.add(groupNameField, gbc);
        
        // 群组成员
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("选择成员:"), gbc);
        
        // 创建用户选择列表
        DefaultListModel<String> membersModel = new DefaultListModel<>();
        ArrayList<String> onlineUsers = controller.getOnlineUsers();
        for (String user : onlineUsers) {
            membersModel.addElement(user);
        }
        
        JList<String> membersList = new JList<>(membersModel);
        membersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane membersScrollPane = new JScrollPane(membersList);
        membersScrollPane.setPreferredSize(new Dimension(200, 150));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(membersScrollPane, gbc);
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(this, panel, "创建新群组", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String groupName = groupNameField.getText();
            List<String> selectedMembers = membersList.getSelectedValuesList();
            
            // 调用控制器创建群组
            controller.createGroup(groupName, selectedMembers);
        }
    }
    
    /*
        显示群组上下文菜单
    */
    private void showGroupContextMenu(GroupItem group, int x, int y) {
        // 创建菜单项
        String[] options = {"添加成员", "移除成员", "退出群组"};
        
        // 显示菜单
        int choice = JOptionPane.showOptionDialog(
            this,
            "请选择操作",
            "群组: " + group.getName(),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );
        
        // 处理选择
        switch (choice) {
            case 0: // 添加成员
                showAddMemberDialog(group);
                break;
            case 1: // 移除成员
                showRemoveMemberDialog(group);
                break;
            case 2: // 退出群组
                confirmLeaveGroup(group);
                break;
        }
    }
    
    /*
        显示添加成员对话框
    */
    private void showAddMemberDialog(GroupItem group) {
        // 获取当前群组成员
        Group_info groupInfo = group.getGroupInfo();
        List<String> currentMembers = groupInfo.getMembers();
        
        // 创建可选用户列表（排除已在群组中的用户）
        DefaultListModel<String> availableUsersModel = new DefaultListModel<>();
        ArrayList<String> onlineUsers = controller.getOnlineUsers();
        for (String user : onlineUsers) {
            if (!currentMembers.contains(user)) {
                availableUsersModel.addElement(user);
            }
        }
        
        // 如果没有可添加的用户
        if (availableUsersModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可添加的用户", "添加成员", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 创建用户选择列表
        JList<String> usersList = new JList<>(availableUsersModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(usersList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(this, scrollPane, "选择要添加的成员", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION && usersList.getSelectedValue() != null) {
            String selectedUser = usersList.getSelectedValue();
            // 调用控制器添加成员
            controller.addUserToGroup(group.getId(), selectedUser);
        }
    }
    
    /*
        显示移除成员对话框
    */
    private void showRemoveMemberDialog(GroupItem group) {
        // 获取当前群组成员
        Group_info groupInfo = group.getGroupInfo();
        List<String> currentMembers = groupInfo.getMembers();
        
        // 创建成员列表（排除当前用户）
        DefaultListModel<String> membersModel = new DefaultListModel<>();
        for (String member : currentMembers) {
            membersModel.addElement(member);
        }
        
        // 如果没有可移除的成员
        if (membersModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可移除的成员", "移除成员", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 创建成员选择列表
        JList<String> membersList = new JList<>(membersModel);
        membersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(membersList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(this, scrollPane, "选择要移除的成员", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION && membersList.getSelectedValue() != null) {
            String selectedMember = membersList.getSelectedValue();
            // 调用控制器移除成员
            controller.removeUserFromGroup(group.getId(), selectedMember);
        }
    }
    
    /*
        确认退出群组
    */
    private void confirmLeaveGroup(GroupItem group) {
        int result = JOptionPane.showConfirmDialog(this, 
                "确定要退出群组 \"" + group.getName() + "\" 吗？", 
                "退出群组", 
                JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // 调用控制器退出群组
            controller.leaveGroup(group.getId());
        }
    }
    
    /*
        切换到私聊模式
    */
    private void switchToPrivateChat(String username) {
        isGroupChat = false;
        currentTarget = username;
        currentTargetName = username;
        setTitle("多人聊天室 - 与 " + username + " 私聊中");
        statusLabel.setText("与 " + username + " 私聊中");
    }
    
    /*
        切换到群聊模式
    */
    private void switchToGroupChat(GroupItem group) {
        isGroupChat = true;
        currentTarget = String.valueOf(group.getId());
        currentTargetName = group.getName();
        setTitle("多人聊天室 - 群聊: " + group.getName());
        statusLabel.setText("群聊: " + group.getName());
    }
    
    /*
        发送消息
    */
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (currentTarget == null) {
            showError("请先选择聊天对象");
            return;
        }
        
        // 调用控制器发送消息
        controller.sendMessage(message, isGroupChat, currentTarget);
    }
    
    /*
        更新在线用户列表
    */
    public void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            ArrayList<String> onlineUsers = controller.getOnlineUsers();
            for (String user : onlineUsers) {
                userListModel.addElement(user);
            }
        });
    }
    
    /*
        更新群组列表
    */
    public void updateGroupList() {
        SwingUtilities.invokeLater(() -> {
            groupListModel.clear();
            List<Group_info> userGroups = controller.getUserGroups();
            for (Group_info group : userGroups) {
                groupListModel.addElement(new GroupItem(group));
            }
        });
    }
    
    /*
        显示消息
    */
    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageDisplay.append(message + "\n");
            // 滚动到底部
            messageDisplay.setCaretPosition(messageDisplay.getDocument().getLength());
        });
    }
    
    /*
        清空消息输入框
    */
    public void clearMessageInput() {
        SwingUtilities.invokeLater(() -> {
            messageInput.setText("");
            messageInput.requestFocus();
        });
    }
    
    /*
        统一显示错误消息
    */
    public void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, "错误", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /*
        显示普通消息
    */
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            // 5秒后恢复状态栏
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    SwingUtilities.invokeLater(() -> {
                        if (currentTargetName != null) {
                            if (isGroupChat) {
                                statusLabel.setText("群聊: " + currentTargetName);
                            } else {
                                statusLabel.setText("与 " + currentTargetName + " 私聊中");
                            }
                        } else {
                            statusLabel.setText("就绪");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
    
    /*
        模型更新回调
    */
    @Override
    public void onModelUpdate(UpdateType updateType) {
        switch (updateType) {
            case ONLINE_USERS:
                updateUserList();
                break;
            case GROUPS:
                updateGroupList();
                break;
            case MESSAGES:
                // 消息已通过控制器的onNewMessage方法处理
                break;
            case ALL:
                updateUserList();
                updateGroupList();
                break;
            default:
                break;
        }
    }
    
    /*
        群组项，用于在JList中显示群组
    */
    private class GroupItem {
        private Group_info groupInfo;
        
        public GroupItem(Group_info groupInfo) {
            this.groupInfo = groupInfo;
        }
        
        public int getId() {
            return groupInfo.get_Group_id();
        }
        
        public String getName() {
            return groupInfo.get_Group_name();
        }
        
        public Group_info getGroupInfo() {
            return groupInfo;
        }
        
        @Override
        public String toString() {
            return getName();
        }
    }
} 