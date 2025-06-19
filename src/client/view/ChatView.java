package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import client.controller.ChatController;
import client.model.ClientModel;
import client.model.ClientModel.ModelObserver;
import client.model.ClientModel.UpdateType;
import info.Chat_info;

/**
 * 聊天窗口，提供单独的聊天界面
*/
public class ChatView extends JFrame implements ModelObserver {
    private static final long serialVersionUID = 1L;

    // 控制器和模型引用
    private ChatController controller;
    private ClientModel model;

    // 界面组件
    private JTextArea messageDisplay;
    private JTextField messageInput;
    private JButton sendButton;
    private JLabel statusLabel;

    // 聊天信息
    private boolean isGroupChat;
    private String targetId;  // 用户名或群组ID
    private String targetName; // 显示名称

    // 日期格式化
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数 - 用于单独的聊天窗口
     * @param controller 聊天控制器
     * @param model 客户端模型
     * @param isGroupChat 是否为群聊
     * @param targetId 目标ID（用户名或群组ID）
     * @param targetName 目标名称（用于显示）
     */
    public ChatView(ChatController controller, ClientModel model, boolean isGroupChat, String targetId, String targetName) {
        this.controller = controller;
        this.model = model;
        this.isGroupChat = isGroupChat;
        this.targetId = targetId;
        this.targetName = targetName;

        // 设置窗口属性
        setTitle(isGroupChat ? "群聊: " + targetName : "与 " + targetName + " 聊天");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            // 设置界面风格
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 初始化界面
        initComponents();

        // 注册为模型观察者
        model.addObserver(this);

        // 将此视图设置到控制器中
        controller.setChatView(this);

        // 添加窗口关闭事件处理
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                model.removeObserver(ChatView.this);
                // 从控制器中移除此视图
                controller.removeChatView(ChatView.this);
            }
        });

        // 加载历史聊天记录
        loadChatHistory();
    }

    /**
     * 初始化界面组件
    */
    private void initComponents() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建聊天面板
        JPanel chatPanel = createChatPanel();

        // 创建状态栏
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 组装主面板
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // 添加到窗口
        setContentPane(mainPanel);
    }

    /**
     * 创建聊天面板
    */
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 创建聊天标题
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel chatTitle = new JLabel(isGroupChat ? "群聊: " + targetName : "与 " + targetName + " 聊天");
        chatTitle.setFont(new Font("宋体", Font.BOLD, 16));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        titlePanel.add(chatTitle, BorderLayout.CENTER);

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
        messageInput.addActionListener(e -> sendMessage());

        // 创建发送按钮
        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());

        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // 组装面板
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(messageScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 发送消息
    */
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            showError("消息不能为空");
            return;
        }

        // 发送消息到服务器，不在本地显示，等待服务器回传
        if (isGroupChat) {
            controller.sendGroupMessage(targetId, message);
        } else {
            controller.sendPrivateMessage(targetId, message);
        }
        
        // 只清空输入框，不显示消息
        clearMessageInput();
    }

    /**
     * 加载历史聊天记录
     */
    private void loadChatHistory() {
        Chat_info chatInfo = new Chat_info();
        chatInfo.setType(isGroupChat);
        
        if (isGroupChat) {
            try {
                int groupId = Integer.parseInt(targetId);
                chatInfo.setGroup_id(groupId);
            } catch (NumberFormatException e) {
                showError("群组ID格式错误");
                return;
            }
        } else {
            chatInfo.setFrom_username(controller.getCurrentUsername());
            chatInfo.setTo_username(targetId);
        }
        
        // 显示加载提示
        messageDisplay.setText("正在加载消息...\n");
        
        // 获取未读消息
        List<Chat_info> unreadMessages = model.getUnreadMessages(isGroupChat, targetId);
        
        controller.loadChatHistory(chatInfo, history -> {
            SwingUtilities.invokeLater(() -> {
                messageDisplay.setText("");
                
                // 使用Set去重，避免重复显示相同的消息
                Set<String> uniqueMessages = new HashSet<>();
                
                // 显示历史消息
                if (history != null && !history.isEmpty()) {
                    for (String line : history) {
                        // 解析保存的消息格式: 时间戳|发送者|消息内容
                        String[] parts = line.split("\\|", 3);
                        if (parts.length >= 3) {
                            String timestamp = parts[0];
                            String sender = parts[1];
                            String text = parts[2];
                            
                            // 尝试解析并格式化时间戳
                            try {
                                // 如果时间戳只包含时分秒，添加当前日期
                                if (timestamp.matches("\\d{2}:\\d{2}:\\d{2}")) {
                                    LocalDateTime now = LocalDateTime.now();
                                    timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + timestamp;
                                }
                                // 如果时间戳格式不符合预期，保持原样
                            } catch (Exception e) {
                                // 保持原始时间戳
                            }
                            
                            // 构建格式化的消息
                            String formattedMessage = "[" + timestamp + "] " + sender + ": " + text;
                            
                            // 如果是新消息，则添加到显示中
                            if (uniqueMessages.add(formattedMessage)) {
                                messageDisplay.append(formattedMessage + "\n");
                            }
                        } else {
                            // 如果格式不正确，直接显示原始行
                            messageDisplay.append(line + "\n");
                        }
                    }
                }
                
                // 显示未读消息，如果有的话
                if (!unreadMessages.isEmpty()) {
                    // 添加分隔线，标记未读消息的开始
                    messageDisplay.append("\n----- 以下是未读消息 -----\n\n");
                    
                    // 遍历未读消息并显示
                    for (Chat_info msg : unreadMessages) {
                        String timestamp = LocalDateTime.now().format(timeFormatter);
                        String sender = msg.getFrom_username();
                        String text = msg.getText();
                        
                        // 显示未读消息
                        messageDisplay.append("[" + timestamp + "] " + sender + ": " + text + "\n");
                    }
                }
                
                // 滚动到最底部
                messageDisplay.setCaretPosition(messageDisplay.getDocument().getLength());
                
                // 将未读消息标记为已读
                if (!unreadMessages.isEmpty()) {
                    model.markMessagesAsRead(isGroupChat, targetId);
                }
            });
        });
    }

    /**
     * 显示消息
    */
    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            // 添加时间戳
            String timestamp = LocalDateTime.now().format(timeFormatter);
            messageDisplay.append("[" + timestamp + "] " + message + "\n");
            // 滚动到最底部
            messageDisplay.setCaretPosition(messageDisplay.getDocument().getLength());
        });
    }

    /**
     * 清空消息输入框
    */
    public void clearMessageInput() {
        SwingUtilities.invokeLater(() -> {
            messageInput.setText("");
            messageInput.requestFocus();
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
        if (updateType == UpdateType.CHAT) {
            Chat_info lastChat = model.getLastChatMessage();
            if (lastChat != null) {
                // 检查消息是否与当前聊天相关
                boolean relevant = false;
                
                if (isGroupChat && lastChat.isType()) {
                    // 群聊消息
                    try {
                        int groupId = Integer.parseInt(targetId);
                        if (lastChat.getGroup_id() == groupId) {
                            relevant = true;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略错误
                    }
                } else if (!isGroupChat && !lastChat.isType()) {
                    // 私聊消息
                    String from = lastChat.getFrom_username();
                    String to = lastChat.getTo_username();
                    String currentUser = controller.getCurrentUsername();
                    
                    // 如果消息是当前用户与目标用户之间的
                    if ((from.equals(currentUser) && to.equals(targetId)) ||
                        (from.equals(targetId) && to.equals(currentUser))) {
                        relevant = true;
                    }
                }
                
                // 如果消息与当前聊天相关，则显示
                if (relevant) {
                    String from = lastChat.getFrom_username();
                    String text = lastChat.getText();
                    displayMessage(from + ": " + text);
                    
                    // 如果消息不是由当前用户发送的，则标记为已读
                    if (!lastChat.getFrom_username().equals(controller.getCurrentUsername())) {
                        // 将当前聊天的所有未读消息标记为已读
                        model.markMessagesAsRead(isGroupChat, targetId);
                    }
                }
            }
        }
    }
    
    // 为了兼容旧代码添加的方法
    public void updateUserList() {}
    public void updateGroupList() {}

    /**
     * 返回聊天是否为群聊
     * @return 是否为群聊
     */
    public boolean isGroupChat() {
        return isGroupChat;
    }
    
    /**
     * 获取目标ID（用户名或群组ID）
     * @return 目标ID
     */
    public String getTargetId() {
        return targetId;
    }
}