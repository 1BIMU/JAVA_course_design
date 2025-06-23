package client.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
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
    private JLabel statusLabel;
    private JLabel currentUserLabel;
    
    // 打开的聊天窗口映射
    private Map<String, ChatView> privateChatWindows = new HashMap<>();
    private Map<Integer, ChatView> groupChatWindows = new HashMap<>();
    private Map<Integer, ChatView> teamChatWindows = new HashMap<>();
    // 当前用户名
    private String currentUsername;

    private JList<TeamItem> teamList; // 新增：小组列表
    private DefaultListModel<TeamItem> teamListModel; // 新增：小组列表模型
    private JButton viewInvitationsButton; // 新增：查看邀请按钮
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

        // 新增：创建小组面板
        JPanel teamPanel = createTeamListPanel();
        tabbedPane.addTab("小组", null, teamPanel);

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
     * 创建小组列表面板 (新增方法)
     */
    private JPanel createTeamListPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        teamListModel = new DefaultListModel<>();
        teamList = new JList<>(teamListModel);
        teamList.setCellRenderer(new TeamListCellRenderer());
        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 这里可以为小组列表添加双击事件等
        teamList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TeamItem selectedTeam = teamList.getSelectedValue();
                    if (selectedTeam != null) {
                        openTeamChat(selectedTeam);
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(teamList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createTeamButton = new JButton("创建小组");
        createTeamButton.addActionListener(e -> controller.showCreateTeamDialog());

        viewInvitationsButton = new JButton("查看邀请");
        viewInvitationsButton.addActionListener(e -> controller.showTeamInvitationsView());

        buttonPanel.add(createTeamButton);
        buttonPanel.add(viewInvitationsButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 打开小组聊天窗口 (新增方法)
     * @param team 要打开聊天的目标小组
     */
    public void openTeamChat(TeamItem team) {
        int teamId = team.getId();
        if (teamChatWindows.containsKey(teamId)) {
            // 如果窗口已存在，则激活它
            ChatView chatView = teamChatWindows.get(teamId);
            chatView.setVisible(true);
            chatView.toFront();
            chatView.requestFocus();
        } else {
            // 创建新的聊天窗口
            // 注意：我们复用 ChatView，并将小组聊天视为一种特殊的“群聊” (isGroupChat = true)
            ChatView chatView = new ChatView(controller, model, true, String.valueOf(teamId), team.getName(),true);
            chatView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    teamChatWindows.remove(teamId);
                }
            });
            teamChatWindows.put(teamId, chatView);
            chatView.setVisible(true);
        }
    }
    /**
     * 更新小组列表 (新增方法)
     */
    public void updateTeamList() {
        SwingUtilities.invokeLater(() -> {
            teamListModel.clear();
            Map<Integer, Org_info> teams = model.getOrgs();
            if (teams != null) {
                for (Org_info team : teams.values()) {
                    teamListModel.addElement(new TeamItem(team));
                }
            }
        });
    }
    /**
     * 更新邀请按钮的状态 (新增方法)
     */
    private void updateInvitationStatus() {
        SwingUtilities.invokeLater(() -> {
            List<Org_info> invitations = model.getPendingTeamInvitations();
            if (invitations != null && !invitations.isEmpty()) {
                viewInvitationsButton.setText("查看邀请 (" + invitations.size() + ")");
                viewInvitationsButton.setForeground(Color.RED);
            } else {
                viewInvitationsButton.setText("查看邀请");
                viewInvitationsButton.setForeground(null);
            }
        });
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
            ChatView chatView = new ChatView(controller, model, false, username, username,false);
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
            ChatView chatView = new ChatView(controller, model, true, String.valueOf(groupId), group.getName(),false);
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

        for (ChatView chatView : teamChatWindows.values()) {
            chatView.dispose();
        }
        teamChatWindows.clear();
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
     * 模型更新回调
     */
    @Override
    public void onModelUpdate(UpdateType updateType) {
        switch (updateType) {
            case USERS:
            case ALL_USERS:
                updateUserList();
                break;
            case GROUPS:
                updateGroupList();
                break;
            case CHAT:
                // 聊天消息由各聊天窗口处理
                // 但我们需要更新联系人列表以显示未读消息状态
                updateUserList();
                // 同时更新群组列表，显示群聊未读消息
                updateGroupList();
                break;
            case ORGANIZATIONS: // 小组列表更新
                updateTeamList();
                break;
            case TEAM_INVITATIONS: // 小组邀请更新
                updateInvitationStatus();
                break;
        }
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
    private class TeamListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            if (isSelected) {
                panel.setBackground(new Color(230, 240, 250));
            } else {
                panel.setBackground(list.getBackground());
            }

            TeamItem team = (TeamItem) value;

            JLabel iconLabel = new JLabel("组", SwingConstants.CENTER);
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new Color(255, 165, 0));
            iconLabel.setForeground(Color.WHITE);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            iconLabel.setPreferredSize(new Dimension(40, 40));

            JLabel nameLabel = new JLabel(team.getName());
            nameLabel.setFont(new Font("宋体", Font.BOLD, 14));

            JLabel memberCountLabel = new JLabel(team.getTeamInfo().getMembers().size() + "人");
            memberCountLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            memberCountLabel.setForeground(Color.GRAY);

            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);
            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(memberCountLabel, BorderLayout.SOUTH);

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);

            return panel;
        }
    }
    /**
     * 群组项包装类
     */
    public static class GroupItem {
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
    public static class TeamItem {
        private Org_info teamInfo;
        public TeamItem(Org_info teamInfo) { this.teamInfo = teamInfo; }
        public int getId() { return teamInfo.getOrg_id(); }
        public String getName() { return teamInfo.getOrg_name(); }
        public Org_info getTeamInfo() { return teamInfo; }
        public int getGroupId() { return teamInfo.getGroup_id(); }
        @Override
        public String toString() { return getName(); }
    }

}