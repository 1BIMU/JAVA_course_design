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
import info.Group_info;

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
    }

    /**
     * 更新用户列表
     */
    public void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            ArrayList<String> users = model.getOnlineUsers();
            if (users != null) {
                for (String user : users) {
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
                updateUserList();
                break;
            case GROUPS:
                updateGroupList();
                break;
            case CHAT:
                // 聊天消息由各聊天窗口处理
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
            
            JLabel nameLabel = new JLabel(username);
            nameLabel.setFont(new Font("宋体", Font.BOLD, 14));
            
            JLabel statusLabel = new JLabel("在线");
            statusLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            statusLabel.setForeground(new Color(0, 128, 0));
            
            // 如果是当前用户，特殊标记
            if (username.equals(currentUsername)) {
                nameLabel.setText(username + " (我)");
                nameLabel.setForeground(new Color(0, 102, 204));
            }
            
            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(statusLabel, BorderLayout.SOUTH);
            
            panel.add(avatarPanel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            
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
            
            JLabel memberCountLabel = new JLabel(group.getGroupInfo().getMembers().size() + "人");
            memberCountLabel.setFont(new Font("宋体", Font.PLAIN, 12));
            memberCountLabel.setForeground(Color.GRAY);
            
            infoPanel.add(nameLabel, BorderLayout.CENTER);
            infoPanel.add(memberCountLabel, BorderLayout.SOUTH);
            
            panel.add(iconPanel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            
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
}