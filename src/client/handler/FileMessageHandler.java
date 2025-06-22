package client.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import client.MessageSender;
import client.controller.ChatController;
import client.model.ClientModel;
import info.Chat_info;
import info.File_info;
import info.encap_info;

/**
 * 文件传输消息处理器
 * 负责处理文件传输相关的消息
 */
public class FileMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private ChatController chatController;
    // 存储文件信息，用于后续下载
    private static Map<String, File_info> fileInfoCache = new HashMap<>();
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param chatController 聊天控制器
     */
    public FileMessageHandler(ClientModel model, ChatController chatController) {
        this.model = model;
        this.chatController = chatController;
    }
    
    @Override
    public void handle(encap_info info) {
        if (info == null || info.get_file_info() == null) {
            System.err.println("文件信息为空");
            return;
        }
        
        File_info fileInfo = info.get_file_info();
        
        // 缓存文件信息，用于后续下载
        fileInfoCache.put(fileInfo.getFileId(), fileInfo);
        
        // 在UI线程中处理文件接收通知
        SwingUtilities.invokeLater(() -> {
            // 显示文件接收通知
            String sender = fileInfo.getFromUsername();
            String fileName = fileInfo.getFileName();
            long fileSize = fileInfo.getFileSize();
            String description = fileInfo.getFileDescription();
            
            // 如果是自己发送的文件（infoOnly=true），则不显示接收对话框
            if (fileInfo.isInfoOnly()) {
                // 在聊天窗口显示文件发送记录
                displayFileSentMessage(fileInfo);
                return;
            }
            
            // 构建通知消息
            StringBuilder message = new StringBuilder();
            message.append("收到来自 ").append(sender).append(" 的文件:\n");
            message.append("文件名: ").append(fileName).append("\n");
            message.append("大小: ").append(formatFileSize(fileSize)).append("\n");
            
            if (description != null && !description.isEmpty()) {
                message.append("描述: ").append(description).append("\n");
            }
            
            message.append("\n是否立即保存此文件?");
            message.append("\n(您也可以稍后在聊天窗口中下载)");
            
            // 创建自定义对话框
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            JLabel label = new JLabel(message.toString());
            panel.add(label, BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("立即保存");
            JButton laterButton = new JButton("稍后下载");
            JButton cancelButton = new JButton("取消");
            
            buttonPanel.add(saveButton);
            buttonPanel.add(laterButton);
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            // 创建非模态对话框
            final javax.swing.JDialog dialog = new javax.swing.JDialog();
            dialog.setTitle("文件接收");
            dialog.setContentPane(panel);
            dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
            dialog.setSize(400, 250);
            dialog.setLocationRelativeTo(null);
            
            // 设置按钮动作
            saveButton.addActionListener(e -> {
                dialog.dispose();
                saveFile(fileInfo);
            });
            
            laterButton.addActionListener(e -> {
                dialog.dispose();
            });
            
            cancelButton.addActionListener(e -> {
                dialog.dispose();
                // 从缓存中移除文件信息
                fileInfoCache.remove(fileInfo.getFileId());
            });
            
            // 显示对话框
            dialog.setVisible(true);
            
            // 在聊天窗口显示文件传输消息
            displayFileReceivedMessage(fileInfo);
        });
    }
    
    /**
     * 显示文件发送记录
     * @param fileInfo 文件信息
     */
    private void displayFileSentMessage(File_info fileInfo) {
        String fileName = fileInfo.getFileName();
        long fileSize = fileInfo.getFileSize();
        String fileId = fileInfo.getFileId();
        
        // 创建聊天信息对象
        Chat_info chatInfo = new Chat_info();
        if (fileInfo.isGroupFile()) {
            chatInfo.setType(true); // 群聊
            chatInfo.setGroup_id(fileInfo.getGroupId());
        } else {
            chatInfo.setType(false); // 私聊
            chatInfo.setTo_username(fileInfo.getToUsername());
        }
        chatInfo.setFrom_username(fileInfo.getFromUsername());
        chatInfo.setText("我发送了文件: " + fileName + " (" + formatFileSize(fileSize) + ")");
        chatInfo.setTransfer_status(true);
        
        // 通知聊天控制器添加消息
        chatController.onNewMessage(chatInfo);
    }
    
    /**
     * 显示文件接收消息
     * @param fileInfo 文件信息
     */
    private void displayFileReceivedMessage(File_info fileInfo) {
        String sender = fileInfo.getFromUsername();
        String fileName = fileInfo.getFileName();
        long fileSize = fileInfo.getFileSize();
        String fileId = fileInfo.getFileId();
        
        // 创建聊天信息对象
        Chat_info chatInfo = new Chat_info();
        if (fileInfo.isGroupFile()) {
            chatInfo.setType(true); // 群聊
            chatInfo.setGroup_id(fileInfo.getGroupId());
        } else {
            chatInfo.setType(false); // 私聊
            chatInfo.setTo_username(model.getCurrentUsername());
        }
        chatInfo.setFrom_username(sender);
        
        // 构建消息文本，包含下载链接
        String message = sender + " 发送了文件: " + fileName + " (" + formatFileSize(fileSize) + ")";
        message += "\n[点击此处下载文件: " + fileId + "]";
        chatInfo.setText(message);
        chatInfo.setTransfer_status(true);
        
        // 通知聊天控制器添加消息
        chatController.onNewMessage(chatInfo);
    }
    
    /**
     * 保存文件
     * @param fileInfo 文件信息
     */
    private void saveFile(File_info fileInfo) {
        // 如果是只包含信息的文件，需要从缓存中获取文件数据
        byte[] fileData;
        if (fileInfo.isInfoOnly()) {
            fileData = MessageSender.getFileData(fileInfo.getFileId());
            if (fileData == null) {
                JOptionPane.showMessageDialog(null, "文件数据已过期或不可用", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            fileData = fileInfo.getFileData();
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存文件");
        fileChooser.setSelectedFile(new File(fileInfo.getFileName()));
        
        int userSelection = fileChooser.showSaveDialog(null);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                fos.write(fileData);
                JOptionPane.showMessageDialog(null, "文件保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "保存文件时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 根据文件ID获取文件信息
     * @param fileId 文件ID
     * @return 文件信息
     */
    public static File_info getFileInfo(String fileId) {
        return fileInfoCache.get(fileId);
    }
    
    /**
     * 下载文件
     * @param fileId 文件ID
     * @return 是否下载成功
     */
    public static boolean downloadFile(String fileId) {
        File_info fileInfo = fileInfoCache.get(fileId);
        if (fileInfo == null) {
            JOptionPane.showMessageDialog(null, "文件信息不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 获取文件数据
        byte[] fileData;
        if (fileInfo.isInfoOnly()) {
            fileData = MessageSender.getFileData(fileId);
        } else {
            fileData = fileInfo.getFileData();
        }
        
        if (fileData == null) {
            JOptionPane.showMessageDialog(null, "文件数据已过期或不可用", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存文件");
        fileChooser.setSelectedFile(new File(fileInfo.getFileName()));
        
        int userSelection = fileChooser.showSaveDialog(null);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                fos.write(fileData);
                JOptionPane.showMessageDialog(null, "文件保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "保存文件时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        return false;
    }
} 