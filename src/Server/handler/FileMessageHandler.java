package Server.handler;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import Server.ChatServer;
import Server.controller.ServerController;
import Server.view.ServerWindow;
import info.File_info;
import info.encap_info;
import io.FileIO;
import io.IOStream;

/**
 * 文件传输消息处理器
 * 负责处理文件传输相关的消息
 */
public class FileMessageHandler implements MessageHandler {
    private ServerController controller;
    private ChatServer server;
    private ServerWindow serverWindow;
    private String currentUser;

    /**
     * 构造函数
     * @param controller 服务器控制器
     * @param server 聊天服务器
     * @param serverWindow 服务器窗口
     * @param currentUser 当前用户
     */
    public FileMessageHandler(ServerController controller, ChatServer server, ServerWindow serverWindow, String currentUser) {
        this.controller = controller;
        this.server = server;
        this.serverWindow = serverWindow;
        this.currentUser = currentUser;
    }

    @Override
    public boolean handle(encap_info info, Socket socket, encap_info returnInfo) {
        File_info fileInfo = info.get_file_info();
        if (fileInfo == null) {
            serverWindow.appendLog("文件信息为空");
            return true;
        }

        // 记录文件传输信息
        String sender = fileInfo.getFromUsername();
        String fileName = fileInfo.getFileName();
        long fileSize = fileInfo.getFileSize();
        boolean isGroupFile = fileInfo.isGroupFile();

        if (isGroupFile) {
            // 群文件处理
            int groupId = fileInfo.getGroupId();
            serverWindow.appendLog(sender + " 向群组 " + groupId + " 发送文件: " + fileName + " (" + formatFileSize(fileSize) + ")");
            
            // 获取群组成员并转发文件
            try {
                FileIO fileIO = new FileIO();
                ArrayList<String> groupMembers = fileIO.getGroupMembers(groupId);
                if (groupMembers != null && !groupMembers.isEmpty()) {
                    // 过滤出在线成员
                    ArrayList<String> onlineMembers = new ArrayList<>();
                    for (String member : groupMembers) {
                        if (server.online_users.contains(member) && !member.equals(sender)) {
                            onlineMembers.add(member);
                        }
                    }
                    
                    // 向在线成员转发文件
                    for (String member : onlineMembers) {
                        Socket memberSocket = server.userSocketMap.get(member);
                        if (memberSocket != null && !memberSocket.isClosed()) {
                            IOStream.writeMessage(memberSocket, info);
                        }
                    }
                }
            } catch (IOException e) {
                serverWindow.appendLog("获取群组成员失败: " + e.getMessage());
            }
        } else {
            // 私聊文件处理
            String receiver = fileInfo.getToUsername();
            serverWindow.appendLog(sender + " 向 " + receiver + " 发送文件: " + fileName + " (" + formatFileSize(fileSize) + ")");
            
            // 获取接收者的Socket并转发文件
            if (server.online_users.contains(receiver)) {
                Socket receiverSocket = server.userSocketMap.get(receiver);
                if (receiverSocket != null && !receiverSocket.isClosed()) {
                    IOStream.writeMessage(receiverSocket, info);
                }
            } else {
                serverWindow.appendLog("接收者 " + receiver + " 不在线，文件传输失败");
                // 通知发送者文件传输失败
                encap_info failureInfo = new encap_info();
                failureInfo.set_type(7);
                File_info failureFileInfo = new File_info();
                failureFileInfo.setFileName(fileName);
                failureFileInfo.setFromUsername("系统消息");
                failureFileInfo.setToUsername(sender);
                failureFileInfo.setGroupFile(false);
                failureFileInfo.setFileDescription("文件传输失败：接收者不在线");
                failureInfo.set_file_info(failureFileInfo);
                
                IOStream.writeMessage(socket, failureInfo);
            }
        }

        return true;
    }

    /**
     * 设置当前用户
     * @param currentUser 当前用户名
     */
    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
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
} 