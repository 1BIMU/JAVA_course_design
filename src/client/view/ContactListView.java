package client.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import client.controller.ChatController;
import client.model.ClientModel;
import client.model.ClientModel.ModelObserver;
import client.model.ClientModel.UpdateType;
import info.Chat_info;
import info.Group_info;
import info.Org_info;

/**
 * 联系人列表视图 - 类似QQ的主界面
 * 显示在线用户列表和群组列表，点击可打开聊天窗口
 */
public class ContactListView extends JFrame implements ModelObserver {
    private static final long serialVersionUID = 1L;

    // 控制器引用
    private ChatController controller;
    private ClientModel model;

    // 界面组件
    private JTabbedPane tabbedPane;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<GroupItem> groupList;
    private DefaultListModel<GroupItem> groupListModel;
    private JList<OrgItem> orgList;
    private DefaultListModel<OrgItem> orgListModel;
    private JLabel statusLabel;
    private JLabel currentUserLabel;
    
    // 打开的聊天窗口映射
    private Map<String, ChatView> privateChatWindows = new HashMap<>();
    private Map<Integer, ChatView> groupChatWindows = new HashMap<>();
    private Map<Integer, ChatView> orgChatWindows = new HashMap<>();
    
    // 当前用户名
    private String currentUsername;

    /**
     * 构造函数
     */
    public ContactListView(ChatController controller, ClientModel model, String username) {
        this.controller = controller;
        this.model = model;
        this.currentUsername = username;

        // 设置窗口属性
        setTitle("聊天室 - 联系人");
        setSize(280, 600);
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
                // 关闭所有聊天窗口
                closeAllChatWindows();
                // 执行登出操作
                controller.logout();
            }
        });
    }

    /**
     * 初始化界面组件
     */
    private void initComponents() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 创建顶部面板
        JPanel topPanel = createTopPanel();
        
        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // 创建在线用户面板
        JPanel userPanel = createUserListPanel();
        tabbedPane.addTab("联系人", new ImageIcon(), userPanel);

        // 创建群组面板
        JPanel groupPanel = createGroupListPanel();
        tabbedPane.addTab("群聊", new ImageIcon(), groupPanel);

        // 创建小组面板
        JPanel orgPanel = createOrgListPanel();
        tabbedPane.addTab("小组", new ImageIcon(), orgPanel);

        // 创建底部工具栏
        JPanel bottomPanel = createBottomPanel();

        // 创建状态栏
        statusLabel = new JLabel("在线");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

        // 组装主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(statusLabel, BorderLayout.PAGE_END);

        // 添加到窗口
        setContentPane(mainPanel);
    }

    /**
     * 创建顶部面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // 用户头像和名称
        JPanel userInfoPanel = new JPanel(new BorderLayout(5, 0));
        
        // 模拟头像
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(230, 230, 230));
                g.fillOval(0, 0, getWidth(), getHeight());
                g.setColor(Color.GRAY);
                g.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(40, 40));
        
        // 用户名称和状态
        JPanel namePanel = new JPanel(new GridLayout(2, 1));
        currentUserLabel = new JLabel(currentUsername);
        currentUserLabel.setFont(new Font("宋体", Font.BOLD, 14));
        JLabel statusText = new JLabel("在线");
        statusText.setFont(new Font("宋体", Font.PLAIN, 12));
        statusText.setForeground(new Color(0, 128, 0));
        
        namePanel.add(currentUserLabel);
        namePanel.add(statusText);
        
        userInfoPanel.add(avatarPanel, BorderLayout.WEST);
        userInfoPanel.add(namePanel, BorderLayout.CENTER);
        
        panel.add(userInfoPanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * 创建用户列表面板
     */
    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建搜索框
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        searchField.setPreferredSize(new Dimension(0, 30));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new ContactListCellRenderer());
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFixedCellHeight(50); // 设置固定高度
        
        // 添加双击事件
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(currentUsername)) {
                        openPrivateChat(selectedUser);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * 创建群组列表面板
     */
    private JPanel createGroupListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建群组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setCellRenderer(new GroupListCellRenderer());
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 添加双击事件
        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    GroupItem selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        openGroupChat(selectedGroup);
                    }
                }
            }
        });
        
        // 添加右键菜单
        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showGroupContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showGroupContextMenu(e);
                }
            }
            
            private void showGroupContextMenu(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    groupList.setSelectedIndex(index);
                    GroupItem selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        ContactListView.this.showGroupContextMenu(selectedGroup, e.getX(), e.getY());
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(groupList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createGroupButton = new JButton("创建群组");
        createGroupButton.addActionListener(e -> showCreateGroupDialog());
        buttonPanel.add(createGroupButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * 创建小组列表面板
     */
    private JPanel createOrgListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建小组列表
        orgListModel = new DefaultListModel<>();
        orgList = new JList<>(orgListModel);
        orgList.setCellRenderer(new OrgListCellRenderer());
        orgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 添加双击事件
        orgList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    OrgItem selectedOrg = orgList.getSelectedValue();
                    if (selectedOrg != null) {
                        openOrgChat(selectedOrg);
                    }
                }
            }
        });
        
        // 添加右键菜单
        orgList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            private void showPopupMenu(MouseEvent e) {
                int index = orgList.locationToIndex(e.getPoint());
                if (index != -1) {
                    orgList.setSelectedIndex(index);
                    OrgItem selectedOrg = orgList.getSelectedValue();
                    if (selectedOrg != null) {
                        showOrgContextMenu(selectedOrg, e.getX(), e.getY());
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(orgList);
        
        // 创建操作按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createOrgButton = new JButton("创建小组");
        createOrgButton.addActionListener(e -> showCreateOrgDialog());
        buttonPanel.add(createOrgButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * 创建底部工具栏
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        
        JButton settingsButton = new JButton("设置");
        JButton logoutButton = new JButton("退出");
        
        logoutButton.addActionListener(e -> {
            closeAllChatWindows();
            controller.logout();
        });
        
        panel.add(settingsButton);
        panel.add(logoutButton);
        
        return panel;
    }

    /**
     * 显示创建群组对话框
     */
    private void showCreateGroupDialog() {
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
        JTextField groupNameField = new JTextField(15);
        panel.add(groupNameField, gbc);
        
        // 群组成员
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("成员(用逗号分隔):"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        JTextField membersField = new JTextField(15);
        panel.add(membersField, gbc);
        
        int result = JOptionPane.showConfirmDialog(
                this, panel, "创建群组", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String groupName = groupNameField.getText().trim();
            String membersText = membersField.getText().trim();
            
            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "群组名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (membersText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "群组成员不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 解析成员列表
            String[] memberArray = membersText.split(",");
            ArrayList<String> members = new ArrayList<>();
            for (String member : memberArray) {
                String trimmed = member.trim();
                if (!trimmed.isEmpty()) {
                    members.add(trimmed);
                }
            }
            
            // 确保当前用户在群组中
            if (!members.contains(currentUsername)) {
                members.add(currentUsername);
            }
            
            // 调用控制器创建群组
            controller.createGroup(groupName, members);
        }
    }

    /**
     * 显示群组上下文菜单
     */
    private void showGroupContextMenu(GroupItem group, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem openItem = new JMenuItem("打开聊天");
        openItem.addActionListener(e -> openGroupChat(group));
        
        JMenuItem addMemberItem = new JMenuItem("添加成员");
        addMemberItem.addActionListener(e -> showAddMemberDialog(group));
        
        JMenuItem removeMemberItem = new JMenuItem("移除成员");
        removeMemberItem.addActionListener(e -> showRemoveMemberDialog(group));
        
        JMenuItem leaveItem = new JMenuItem("退出群组");
        leaveItem.addActionListener(e -> confirmLeaveGroup(group));
        
        menu.add(openItem);
        menu.add(addMemberItem);
        menu.add(removeMemberItem);
        menu.addSeparator();
        menu.add(leaveItem);
        
        menu.show(groupList, x, y);
    }

    /**
     * 显示添加成员对话框
     */
    private void showAddMemberDialog(GroupItem group) {
        String input = JOptionPane.showInputDialog(
                this, "请输入要添加的用户名:", "添加群组成员", JOptionPane.PLAIN_MESSAGE);
        
        if (input != null && !input.trim().isEmpty()) {
            controller.addUserToGroup(group.getId(), input.trim());
        }
    }

    /**
     * 显示移除成员对话框
     */
    private void showRemoveMemberDialog(GroupItem group) {
        ArrayList<String> members = group.getGroupInfo().getMembers();
        if (members == null || members.isEmpty()) {
            JOptionPane.showMessageDialog(this, "群组没有成员", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String[] memberArray = members.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
                this, "选择要移除的成员:", "移除群组成员",
                JOptionPane.PLAIN_MESSAGE, null, memberArray, memberArray[0]);
        
        if (selected != null) {
            controller.removeUserFromGroup(group.getId(), selected);
        }
    }

    /**
     * 确认退出群组
     */
    private void confirmLeaveGroup(GroupItem group) {
        int result = JOptionPane.showConfirmDialog(
                this, "确定要退出群组 \"" + group.getName() + "\" 吗?",
                "退出群组", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            controller.leaveGroup(group.getId());
        }
    }

    /**
     * 打开私聊窗口
     */
    public void openPrivateChat(String username) {
        if (privateChatWindows.containsKey(username)) {
            // 如果窗口已存在，则激活它
            ChatView chatView = privateChatWindows.get(username);
            chatView.setVisible(true);
            chatView.toFront();
            chatView.requestFocus();
            
            // 将该用户的未读消息标记为已读
            model.markMessagesAsRead(false, username);
        } else {
            // 创建新的聊天窗口
            ChatView chatView = new ChatView(controller, model, false, username, username);
            chatView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    privateChatWindows.remove(username);
                }
            });
            privateChatWindows.put(username, chatView);
            chatView.setVisible(true);
            
            // 将该用户的未读消息标记为已读
            model.markMessagesAsRead(false, username);
        }
    }

    /**
     * 打开群聊窗口
     */
    public void openGroupChat(GroupItem group) {
        int groupId = group.getId();
        if (groupChatWindows.containsKey(groupId)) {
            // 如果窗口已存在，则激活它
            ChatView chatView = groupChatWindows.get(groupId);
            chatView.setVisible(true);
            chatView.toFront();
            chatView.requestFocus();
            
            // 将该群组的未读消息标记为已读
            model.markMessagesAsRead(true, String.valueOf(groupId));
        } else {
            // 创建新的聊天窗口
            ChatView chatView = new ChatView(controller, model, true, String.valueOf(groupId), group.getName());
            chatView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    groupChatWindows.remove(groupId);
                }
            });
            groupChatWindows.put(groupId, chatView);
            chatView.setVisible(true);
            
            // 将该群组的未读消息标记为已读
            model.markMessagesAsRead(true, String.valueOf(groupId));
        }
    }

    /**
     * 关闭所有聊天窗口
     */
    private void closeAllChatWindows() {
        for (ChatView chatView : privateChatWindows.values()) {
            chatView.dispose();
        }
        privateChatWindows.clear();
        
        for (ChatView chatView : groupChatWindows.values()) {
            chatView.dispose();
        }
        groupChatWindows.clear();
        
        for (ChatView chatView : orgChatWindows.values()) {
            chatView.dispose();
        }
        orgChatWindows.clear();
    }

    /**
     * 更新用户列表
     */
    public void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            ArrayList<String> allUsers = model.getAllUsers();
            ArrayList<String> onlineUsers = model.getOnlineUsers();
            
            if (allUsers != null) {
                for (String user : allUsers) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    /**
     * 更新群组列表
     */
    public void updateGroupList() {
        SwingUtilities.invokeLater(() -> {
            groupListModel.clear();
            Map<Integer, Group_info> groups = model.getGroups();
            if (groups != null) {
                for (Group_info group : groups.values()) {
                    groupListModel.addElement(new GroupItem(group));
                }
            }
        });
    }

    /**
     * 显示错误消息
     */
    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error, "错误", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 显示提示消息
     */
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 更新状态标签
     */
    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * 显示创建小组对话框
     */
    private void showCreateOrgDialog() {
        // 获取当前用户所在的群组
        Map<Integer, Group_info> groups = model.getGroups();
        if (groups.isEmpty()) {
            showError("您需要先加入群组才能创建小组");
            return;
        }
        
        // 创建群组选择下拉菜单
        JComboBox<GroupItem> groupCombo = new JComboBox<>();
        for (Group_info group : groups.values()) {
            groupCombo.addItem(new GroupItem(group));
        }
        
        // 创建小组名称输入框
        JTextField nameField = new JTextField(20);
        
        // 创建面板
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("选择群组:"));
        panel.add(groupCombo);
        panel.add(new JLabel("小组名称:"));
        panel.add(nameField);
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(
            this, panel, "创建小组", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            // 获取选择的群组和输入的名称
            GroupItem selectedGroup = (GroupItem) groupCombo.getSelectedItem();
            String orgName = nameField.getText().trim();
            
            if (orgName.isEmpty()) {
                showError("小组名称不能为空");
                return;
            }
            
            if (selectedGroup != null) {
                // 显示成员选择对话框
                showSelectMembersForOrgDialog(selectedGroup, orgName);
            }
        }
    }
    
    /**
     * 显示选择小组成员对话框
     */
    private void showSelectMembersForOrgDialog(GroupItem group, String orgName) {
        Group_info groupInfo = group.getGroupInfo();
        ArrayList<String> groupMembers = groupInfo.getMembers();
        
        // 创建成员选择列表
        JPanel memberPanel = new JPanel();
        memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
        
        ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
        for (String member : groupMembers) {
            JCheckBox checkBox = new JCheckBox(member);
            // 当前用户默认选中
            if (member.equals(currentUsername)) {
                checkBox.setSelected(true);
                checkBox.setEnabled(false); // 不允许取消选择自己
            }
            checkBoxes.add(checkBox);
            memberPanel.add(checkBox);
        }
        
        JScrollPane scrollPane = new JScrollPane(memberPanel);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        
        // 显示对话框
        int result = JOptionPane.showConfirmDialog(
            this, scrollPane, "选择小组成员", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            // 收集选择的成员
            ArrayList<String> selectedMembers = new ArrayList<>();
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    selectedMembers.add(groupMembers.get(i));
                }
            }
            
            // 检查至少选择了一个成员
            if (selectedMembers.size() < 2) { // 至少要有自己和另一个人
                showError("请至少选择一个其他成员");
                return;
            }
            
            // 创建小组
            controller.createOrg(group.getId(), orgName, selectedMembers);
        }
    }
    
    /**
     * 显示小组右键菜单
     */
    private void showOrgContextMenu(OrgItem org, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        
        // 创建菜单项
        JMenuItem chatItem = new JMenuItem("打开聊天");
        chatItem.addActionListener(e -> openOrgChat(org));
        
        JMenuItem addItem = new JMenuItem("添加成员");
        addItem.addActionListener(e -> showAddMemberToOrgDialog(org));
        
        JMenuItem removeItem = new JMenuItem("移除成员");
        removeItem.addActionListener(e -> showRemoveMemberFromOrgDialog(org));
        
        JMenuItem leaveItem = new JMenuItem("退出小组");
        leaveItem.addActionListener(e -> confirmLeaveOrg(org));
        
        menu.add(chatItem);
        menu.addSeparator();
        menu.add(addItem);
        menu.add(removeItem);
        menu.addSeparator();
        menu.add(leaveItem);
        
        // 显示菜单
        menu.show(orgList, x, y);
    }
    
    /**
     * 显示添加小组成员对话框
     */
    private void showAddMemberToOrgDialog(OrgItem org) {
        Org_info orgInfo = org.getOrgInfo();
        int groupId = orgInfo.getGroup_id();
        
        // 获取所在群组
        Group_info parentGroup = model.getGroups().get(groupId);
        if (parentGroup == null) {
            showError("无法获取小组所属的群组信息");
            return;
        }
        
        // 获取群组成员列表
        ArrayList<String> groupMembers = parentGroup.getMembers();
        ArrayList<String> orgMembers = orgInfo.getMembers();
        
        // 过滤出未加入小组的群组成员
        ArrayList<String> nonOrgMembers = new ArrayList<>();
        for (String member : groupMembers) {
            if (!orgMembers.contains(member)) {
                nonOrgMembers.add(member);
            }
        }
        
        if (nonOrgMembers.isEmpty()) {
            showError("群组中所有成员已加入小组");
            return;
        }
        
        // 显示成员选择对话框
        String[] members = nonOrgMembers.toArray(new String[0]);
        String selectedMember = (String) JOptionPane.showInputDialog(
            this, "选择要添加的成员", "添加小组成员",
            JOptionPane.PLAIN_MESSAGE, null, members, members[0]
        );
        
        if (selectedMember != null) {
            controller.addUserToOrg(org.getId(), selectedMember);
        }
    }
    
    /**
     * 显示移除小组成员对话框
     */
    private void showRemoveMemberFromOrgDialog(OrgItem org) {
        ArrayList<String> members = org.getOrgInfo().getMembers();
        
        // 从列表中移除当前用户
        ArrayList<String> otherMembers = new ArrayList<>(members);
        otherMembers.remove(currentUsername);
        
        if (otherMembers.isEmpty()) {
            showError("小组中没有其他成员可移除");
            return;
        }
        
        // 显示成员选择对话框
        String[] memberArray = otherMembers.toArray(new String[0]);
        String selectedMember = (String) JOptionPane.showInputDialog(
            this, "选择要移除的成员", "移除小组成员",
            JOptionPane.PLAIN_MESSAGE, null, memberArray, memberArray[0]
        );
        
        if (selectedMember != null) {
            controller.removeUserFromOrg(org.getId(), selectedMember);
        }
    }
    
    /**
     * 确认退出小组
     */
    private void confirmLeaveOrg(OrgItem org) {
        int option = JOptionPane.showConfirmDialog(
            this, "确定要退出 \"" + org.getName() + "\" 小组吗?", 
            "退出小组", JOptionPane.YES_NO_OPTION
        );
        
        if (option == JOptionPane.YES_OPTION) {
            controller.leaveOrg(org.getId());
        }
    }
    
    /**
     * 打开小组聊天窗口
     */
    public void openOrgChat(OrgItem org) {
        int orgId = org.getId();
        int groupId = org.getOrgInfo().getGroup_id();
        
        if (orgChatWindows.containsKey(orgId)) {
            // 如果窗口已存在，则激活它
            ChatView chatView = orgChatWindows.get(orgId);
            chatView.setVisible(true);
            chatView.toFront();
            chatView.requestFocus();
        } else {
            // 创建新的小组聊天窗口 (使用ChatView类的小组构造函数)
            ChatView chatView = new ChatView(controller, model, groupId, orgId, org.getName());
            chatView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    orgChatWindows.remove(orgId);
                }
            });
            orgChatWindows.put(orgId, chatView);
            chatView.setVisible(true);
        }
    }

    /**
     * 更新小组列表
     */
    public void updateOrgList() {
        SwingUtilities.invokeLater(() -> {
            orgListModel.clear();
            Map<Integer, Org_info> orgs = model.getOrgs();
            if (orgs != null) {
                for (Org_info org : orgs.values()) {
                    orgListModel.addElement(new OrgItem(org));
                }
            }
        });
    }

    /**
     * 模型更新回调
     */
    @Override
    public void onModelUpdate(UpdateType updateType) {
        SwingUtilities.invokeLater(() -> {
            switch (updateType) {
                case USERS:
                    updateUserList();
                    break;
                case GROUPS:
                    updateGroupList();
                    break;
                case ORGS:
                    updateOrgList();
                    break;
                case ALL_USERS:
                    updateUserList();
                    break;
                case ALL:
                    updateUserList();
                    updateGroupList();
                    updateOrgList();
                    break;
            }
        });
    }

    /**
     * 联系人列表单元格渲染器
     */
    private class ContactListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            if (isSelected) {
                panel.setBackground(new Color(230, 240, 250));
                panel.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0, 120, 215)));
            } else {
                panel.setBackground(list.getBackground());
            }
            
            String username = (String) value;
            boolean isOnline = model.getOnlineUsers().contains(username);
            
            // 创建头像面板
            JPanel avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(new Color(200, 200, 200));
                    g.fillOval(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                    
                    // 绘制用户名首字母
                    if (username != null && !username.isEmpty()) {
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("宋体", Font.BOLD, 16));
                        String initial = username.substring(0, 1).toUpperCase();
                        FontMetrics fm = g.getFontMetrics();
                        int x = (getWidth() - fm.stringWidth(initial)) / 2;
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        g.drawString(initial, x, y);
                    }
                }
            };
            avatarPanel.setPreferredSize(new Dimension(40, 40));
            
            // 创建用户信息面板
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);
            
            // 检查是否有未读消息
            boolean hasUnread = false;
            final int unreadCount;
            if (!username.equals(currentUsername)) {
                java.util.List<Chat_info> unreadMessages = model.getUnreadMessages(false, username);
                unreadCount = unreadMessages.size();
                hasUnread = unreadCount > 0;
            } else {
                unreadCount = 0;
            }
            
            // 创建用户名标签，如果有未读消息，添加提示
            JLabel nameLabel = new JLabel(username);
            nameLabel.setFont(new Font("宋体", Font.BOLD, 14));
            
            if (hasUnread) {
                nameLabel.setText(username + " [" + unreadCount + "条新消息]");
                nameLabel.setForeground(new Color(255, 0, 0)); // 红色显示
            }
            
            // 如果是当前用户，特殊标记
            if (username.equals(currentUsername)) {
                nameLabel.setText(username + " (我)");
                nameLabel.setForeground(new Color(0, 102, 204));
            }
            
            // 创建状态标签
            JLabel statusLabel = new JLabel(isOnline ? "在线" : "离线");
            statusLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            statusLabel.setForeground(isOnline ? new Color(0, 128, 0) : Color.GRAY);
            
            // 如果有未读消息，修改状态标签
            if (hasUnread) {
                statusLabel.setText(isOnline ? "在线 - 有新消息" : "离线 - 有新消息");
                statusLabel.setForeground(new Color(255, 0, 0));
            }
            
            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(statusLabel, BorderLayout.SOUTH);
            
            panel.add(avatarPanel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            
            // 如果有未读消息，添加提示图标
            if (hasUnread) {
                JPanel notificationPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(new Color(255, 0, 0));
                        g.fillOval(0, 0, 16, 16);
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("宋体", Font.BOLD, 10));
                        String count = String.valueOf(unreadCount);
                        FontMetrics fm = g.getFontMetrics();
                        int x = (getWidth() - fm.stringWidth(count)) / 2;
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        g.drawString(count, x, y);
                    }
                };
                notificationPanel.setPreferredSize(new Dimension(16, 16));
                panel.add(notificationPanel, BorderLayout.EAST);
            }
            
            return panel;
        }
    }

    /**
     * 群组列表单元格渲染器
     */
    private class GroupListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            if (isSelected) {
                panel.setBackground(new Color(230, 240, 250));
                panel.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0, 120, 215)));
            } else {
                panel.setBackground(list.getBackground());
            }
            
            GroupItem group = (GroupItem) value;
            
            // 检查是否有未读消息
            boolean hasUnread = false;
            final int unreadCount;
            java.util.List<Chat_info> unreadMessages = model.getUnreadMessages(true, String.valueOf(group.getId()));
            unreadCount = unreadMessages.size();
            hasUnread = unreadCount > 0;
            
            // 创建群组图标面板
            JPanel iconPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(new Color(102, 153, 255));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g.setColor(Color.WHITE);
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    
                    // 绘制群组名首字母
                    String name = group.getName();
                    if (name != null && !name.isEmpty()) {
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("宋体", Font.BOLD, 16));
                        String initial = name.substring(0, 1).toUpperCase();
                        FontMetrics fm = g.getFontMetrics();
                        int x = (getWidth() - fm.stringWidth(initial)) / 2;
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        g.drawString(initial, x, y);
                    }
                }
            };
            iconPanel.setPreferredSize(new Dimension(40, 40));
            
            // 创建群组信息面板
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);
            
            JLabel nameLabel = new JLabel(group.getName());
            nameLabel.setFont(new Font("宋体", Font.BOLD, 14));
            
            // 如果有未读消息，修改名称标签
            if (hasUnread) {
                nameLabel.setText(group.getName() + " [" + unreadCount + "条新消息]");
                nameLabel.setForeground(new Color(255, 0, 0)); // 红色显示
            }
            
            JLabel memberCountLabel = new JLabel(group.getGroupInfo().getMembers().size() + "人");
            memberCountLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            memberCountLabel.setForeground(Color.GRAY);
            
            // 如果有未读消息，修改成员数量标签
            if (hasUnread) {
                memberCountLabel.setText(group.getGroupInfo().getMembers().size() + "人 - 有新消息");
                memberCountLabel.setForeground(new Color(255, 0, 0));
            }
            
            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(memberCountLabel, BorderLayout.SOUTH);
            
            panel.add(iconPanel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            
            // 如果有未读消息，添加提示图标
            if (hasUnread) {
                JPanel notificationPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(new Color(255, 0, 0));
                        g.fillOval(0, 0, 16, 16);
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("宋体", Font.BOLD, 10));
                        String count = String.valueOf(unreadCount);
                        FontMetrics fm = g.getFontMetrics();
                        int x = (getWidth() - fm.stringWidth(count)) / 2;
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        g.drawString(count, x, y);
                    }
                };
                notificationPanel.setPreferredSize(new Dimension(16, 16));
                panel.add(notificationPanel, BorderLayout.EAST);
            }
            
            return panel;
        }
    }

    /**
     * 群组项包装类
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

    /**
     * 小组列表单元格渲染器
     */
    private class OrgListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof OrgItem)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            
            OrgItem orgItem = (OrgItem) value;
            Org_info orgInfo = orgItem.getOrgInfo();
            
            // 检查是否有未读消息
            boolean hasUnread = model.hasUnreadMessages(true, String.valueOf(orgItem.getId()));
            int unreadCount = 0;
            if (hasUnread) {
                java.util.List<Chat_info> unreadMessages = model.getUnreadMessages(true, String.valueOf(orgItem.getId()));
                unreadCount = unreadMessages != null ? unreadMessages.size() : 0;
            }
            
            // 创建自定义面板
            JPanel panel = new JPanel(new BorderLayout(5, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            // 设置选中状态的背景
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
            } else {
                panel.setBackground(list.getBackground());
            }
            
            // 创建左侧头像
            JPanel avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(new Color(220, 220, 240));
                    g.fillOval(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(180, 180, 200));
                    g.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                    
                    // 绘制小组名称首字母
                    String name = orgItem.getName();
                    String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                    g.setColor(new Color(100, 100, 180));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    FontMetrics fm = g.getFontMetrics();
                    int width = fm.stringWidth(initial);
                    int height = fm.getAscent();
                    g.drawString(initial, (getWidth() - width) / 2, (getHeight() + height) / 2 - 2);
                }
            };
            avatarPanel.setPreferredSize(new Dimension(40, 40));
            
            // 创建中间信息面板
            JPanel infoPanel = new JPanel(new GridLayout(2, 1));
            infoPanel.setOpaque(false);
            
            // 显示小组名称，如果有未读消息，显示未读消息数量并加粗红色显示
            JLabel nameLabel = new JLabel(orgItem.getName() + (hasUnread ? " (" + unreadCount + ")" : ""));
            nameLabel.setFont(new Font("宋体", Font.BOLD, 14));
            if (hasUnread) {
                nameLabel.setForeground(Color.RED);
            } else if (isSelected) {
                nameLabel.setForeground(list.getSelectionForeground());
            }
            
            // 显示所属群组ID
            JLabel groupLabel = new JLabel("群组ID: " + orgInfo.getGroup_id());
            groupLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            groupLabel.setForeground(Color.GRAY);
            
            infoPanel.add(nameLabel);
            infoPanel.add(groupLabel);
            
            // 创建右侧面板，显示成员数量和未读消息提示
            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setOpaque(false);
            
            // 成员数量标签
            JLabel countLabel = new JLabel(orgInfo.getMembers().size() + "人");
            countLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            countLabel.setForeground(new Color(100, 100, 180));
            
            rightPanel.add(countLabel, BorderLayout.NORTH);
            
            // 如果有未读消息，添加红点提示
            if (hasUnread) {
                JPanel notificationPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(Color.RED);
                        g.fillOval(0, 0, 8, 8);
                    }
                };
                notificationPanel.setPreferredSize(new Dimension(10, 10));
                rightPanel.add(notificationPanel, BorderLayout.CENTER);
            }
            
            // 组装面板
            panel.add(avatarPanel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            panel.add(rightPanel, BorderLayout.EAST);
            
            return panel;
        }
    }
    
    /**
     * 小组项类，封装小组信息
     */
    private class OrgItem {
        private Org_info orgInfo;
        
        public OrgItem(Org_info orgInfo) {
            this.orgInfo = orgInfo;
        }
        
        public int getId() {
            return orgInfo.getOrg_id();
        }
        
        public String getName() {
            return orgInfo.getOrg_name();
        }
        
        public Org_info getOrgInfo() {
            return orgInfo;
        }
        
        @Override
        public String toString() {
            return orgInfo.getOrg_name();
        }
    }
}