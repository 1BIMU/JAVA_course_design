package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;

import client.controller.ChatController;
import client.controller.VoiceCallController;
import client.handler.FileMessageHandler;
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
    private VoiceCallController voiceCallController; // 添加语音通话控制器引用

    // 界面组件
    private JTextPane messageDisplay;
    private JTextField messageInput;
    private JButton sendButton;
    private JButton sendFileButton; // 发送文件按钮
    private JButton sendImageButton; // 发送图片按钮
    private JButton voiceCallButton; // 语音通话按钮
    private JLabel statusLabel;
    private StyledDocument document;
    private Style defaultStyle;
    private Style imageStyle;

    // 聊天信息
    private boolean isGroupChat;
    private boolean isTeamChat; // 是否为小组的字段
    private String targetId;  // 用户名或群组ID
    private String targetName; // 显示名称

    // 日期格式化
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 文件下载链接正则表达式
    private final Pattern FILE_DOWNLOAD_PATTERN = Pattern.compile("\\[点击此处下载文件: ([0-9a-f-]+)\\]");
    // 图片标识符的正则表达式
    private final Pattern IMAGE_PATTERN = Pattern.compile("\\[图片ID:([0-9a-f-]+)\\]");

    /**
     * 构造函数 - 用于单独的聊天窗口
     * @param controller 聊天控制器
     * @param model 客户端模型
     * @param isGroupChat 是否为群聊
     * @param targetId 目标ID（用户名或群组ID）
     * @param targetName 目标名称（用于显示）
     */
    public ChatView(ChatController controller, ClientModel model, boolean isGroupChat, String targetId, String targetName,boolean isTeamChat) {
        this.controller = controller;
        this.model = model;
        this.isGroupChat = isGroupChat;
        this.targetId = targetId;
        this.targetName = targetName;
        this.voiceCallController = controller.getVoiceCallController(); // 获取语音通话控制器
        this.isTeamChat = isTeamChat;
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
        document = new DefaultStyledDocument();
        messageDisplay = new JTextPane(document);
        messageDisplay.setEditable(false);
        messageDisplay.setFont(new Font("宋体", Font.PLAIN, 14));
        
        // 创建默认样式
        defaultStyle = messageDisplay.addStyle("defaultStyle", null);
        
        // 创建图片样式
        imageStyle = messageDisplay.addStyle("imageStyle", null);
        
        // 添加鼠标点击事件监听器
        messageDisplay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleFileDownloadClick(e);
            }
        });
        
        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(messageDisplay);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 创建输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageInput = new JTextField();
        messageInput.setFont(new Font("宋体", Font.PLAIN, 14));
        messageInput.addActionListener(e -> sendMessage());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        
        sendFileButton = new JButton("发送文件");
        sendFileButton.addActionListener(e -> sendFile());
        
        sendImageButton = new JButton("发送图片");
        sendImageButton.addActionListener(e -> sendImage());
        
        voiceCallButton = new JButton("语音通话");
        voiceCallButton.addActionListener(e -> initiateVoiceCall());
        
        buttonPanel.add(voiceCallButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(sendButton);
        
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // 组装面板
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    /**
     * 处理文件下载点击事件
     * @param e 鼠标事件
     */
    private void handleFileDownloadClick(MouseEvent e) {
        try {
            int offset = messageDisplay.viewToModel2D(e.getPoint());
            String text = document.getText(0, document.getLength());
            
            // 查找点击位置所在的行
            int lineStart = text.lastIndexOf('\n', offset) + 1;
            int lineEnd = text.indexOf('\n', offset);
            if (lineEnd == -1) {
                lineEnd = text.length();
            }
            
            String line = text.substring(lineStart, lineEnd);
            
            // 检查是否点击了文件下载链接
            Matcher fileMatcher = FILE_DOWNLOAD_PATTERN.matcher(line);
            if (fileMatcher.find()) {
                String fileId = fileMatcher.group(1);
                FileMessageHandler.downloadFile(fileId);
                return;
            }
            
            // 检查是否点击了图片标识符
            Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
            if (imageMatcher.find()) {
                String imageId = imageMatcher.group(1);
                displayImage(imageId);
            }
        } catch (BadLocationException ex) {
            System.err.println("处理点击事件时出错: " + ex.getMessage());
        }
    }
    
    /**
     * 显示图片
     * @param imageId 图片ID
     */
    private void displayImage(String imageId) {
        ImageIcon image = FileMessageHandler.getImage(imageId);
        if (image != null) {
            // 创建一个新窗口显示原始大小的图片
            JFrame imageFrame = new JFrame("图片查看");
            JLabel imageLabel = new JLabel(image);
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            
            imageFrame.getContentPane().add(scrollPane);
            imageFrame.setSize(800, 600);
            imageFrame.setLocationRelativeTo(null);
            imageFrame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(null, "无法加载图片，可能已过期或不可用", "错误", JOptionPane.ERROR_MESSAGE);
        }
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
        if (isGroupChat&&!isTeamChat) {
            controller.sendGroupMessage(targetId, message);
        } else if(isTeamChat){
            controller.sendTeamMessage(targetId, message);
        }else{
            controller.sendPrivateMessage(targetId, message);
        }
        
        // 只清空输入框，不显示消息
        clearMessageInput();
    }
    
    /**
     * 发送文件
     */
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 检查文件大小
            if (selectedFile.length() > 10 * 1024 * 1024) { // 限制文件大小为10MB
                showError("文件过大，请选择小于10MB的文件");
                return;
            }
            
            // 询问文件描述
            String description = JOptionPane.showInputDialog(this, "请输入文件描述（可选）:", "文件描述", JOptionPane.QUESTION_MESSAGE);
            
            // 发送文件
            boolean success;
            String currentUser = controller.getCurrentUsername();
            
            if (isGroupChat) {
                try {
                    int groupId = Integer.parseInt(targetId);
                    success = controller.sendGroupFile(currentUser, groupId, selectedFile, description);
                } catch (NumberFormatException e) {
                    showError("群组ID格式错误");
                    return;
                }
            } else {
                success = controller.sendPrivateFile(currentUser, targetId, selectedFile, description);
            }
            
            if (success) {
                showMessage("文件发送请求已提交");
            } else {
                showError("文件发送失败，请检查网络连接");
            }
        }
    }

    /**
     * 发送图片
     */
    private void sendImage() {
        if (isGroupChat) {
            controller.sendGroupImage(targetId);
        } else {
            controller.sendPrivateImage(targetId);
        }
    }

    /**
     * 发起语音通话
     */
    private void initiateVoiceCall() {
        if (voiceCallController == null) {
            showError("语音通话功能不可用");
            return;
        }
        
        try {
            if (isGroupChat) {
                // 获取群组成员列表
                List<String> participants = controller.getGroupMembers(targetId);
                if (participants == null || participants.isEmpty()) {
                    showError("无法获取群组成员信息");
                    return;
                }
                
                // 过滤掉当前用户
                String currentUser = controller.getCurrentUsername();
                participants.removeIf(username -> username.equals(currentUser));
                
                if (participants.isEmpty()) {
                    showError("群组中没有其他成员");
                    return;
                }
                
                // 发起群组语音会议
                voiceCallController.initiateConference(participants);
                
                // 显示提示消息
                displayMessage("系统消息: 已发起群组语音会议");
            } else {
                // 发起一对一语音通话
                voiceCallController.initiateCall(targetId);
                
                // 显示提示消息
                displayMessage("系统消息: 正在呼叫 " + targetName);
            }
        } catch (Exception e) {
            showError("发起语音通话失败: " + e.getMessage());
            e.printStackTrace();
        }
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
        try {
            document.remove(0, document.getLength());
            document.insertString(0, "正在加载消息...\n", defaultStyle);
        } catch (BadLocationException e) {
            System.err.println("显示加载提示时出错: " + e.getMessage());
        }
        
        // 获取未读消息
        List<Chat_info> unreadMessages = model.getUnreadMessages(isGroupChat, targetId);
        
        controller.loadChatHistory(chatInfo, history -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    document.remove(0, document.getLength());
                    
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
                                    document.insertString(document.getLength(), formattedMessage + "\n", defaultStyle);
                                }
                            } else {
                                // 如果格式不正确，直接显示原始行
                                document.insertString(document.getLength(), line + "\n", defaultStyle);
                            }
                        }
                    }
                    
                    // 显示未读消息，如果有的话
                    if (!unreadMessages.isEmpty()) {
                        // 添加分隔线，标记未读消息的开始
                        document.insertString(document.getLength(), "\n----- 以下是未读消息 -----\n\n", defaultStyle);
                        
                        // 遍历未读消息并显示
                        for (Chat_info msg : unreadMessages) {
                            String timestamp = LocalDateTime.now().format(timeFormatter);
                            String sender = msg.getFrom_username();
                            String text = msg.getText();
                            
                            // 显示未读消息
                            document.insertString(document.getLength(), "[" + timestamp + "] " + sender + ": " + text + "\n", defaultStyle);
                        }
                    }
                    
                    // 滚动到最底部
                    messageDisplay.setCaretPosition(document.getLength());
                    
                    // 将未读消息标记为已读
                    if (!unreadMessages.isEmpty()) {
                        model.markMessagesAsRead(isGroupChat, targetId);
                    }
                } catch (BadLocationException e) {
                    System.err.println("加载历史记录时出错: " + e.getMessage());
                }
            });
        });
    }

    /**
     * 显示消息
    */
    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 添加时间戳
                String timestamp = LocalDateTime.now().format(timeFormatter);
                String formattedMessage = "[" + timestamp + "] " + message;
                document.insertString(document.getLength(), formattedMessage + "\n", defaultStyle);
                // 滚动到底部
                messageDisplay.setCaretPosition(document.getLength());
            } catch (BadLocationException e) {
                System.err.println("添加消息时出错: " + e.getMessage());
            }
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

    /**
     * 添加消息到聊天窗口
     * @param message 消息文本
     */
    public void addMessage(String message) {
        try {
            // 检查消息中是否包含图片标识符
            Matcher matcher = IMAGE_PATTERN.matcher(message);
            
            if (matcher.find()) {
                // 如果包含图片标识符，分割消息并插入图片
                int start = matcher.start();
                int end = matcher.end();
                String imageId = matcher.group(1);
                
                // 添加消息前半部分
                document.insertString(document.getLength(), message.substring(0, start), defaultStyle);
                
                // 获取并添加图片
                ImageIcon image = FileMessageHandler.getImage(imageId);
                if (image != null) {
                    StyleConstants.setIcon(imageStyle, image);
                    document.insertString(document.getLength(), " ", imageStyle);
                    document.insertString(document.getLength(), "\n点击图片查看原图", defaultStyle);
                } else {
                    // 如果图片加载失败，显示文本
                    document.insertString(document.getLength(), "[图片]", defaultStyle);
                }
                
                // 添加消息后半部分（如果有）
                if (end < message.length()) {
                    document.insertString(document.getLength(), message.substring(end), defaultStyle);
                }
            } else {
                // 如果不包含图片标识符，直接添加消息
                document.insertString(document.getLength(), message, defaultStyle);
            }
            
            // 添加换行
            document.insertString(document.getLength(), "\n", defaultStyle);
            
            // 滚动到底部
            messageDisplay.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            System.err.println("添加消息时出错: " + e.getMessage());
        }
    }
}