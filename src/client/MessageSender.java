package client;

import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import info.*;
import io.IOStream;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Graphics2D;

/*
    消息发送器，负责向服务器发送各类消息
*/
public class MessageSender {
    private Socket socket;
    private String host;
    private int port;
    private boolean reconnecting = false;
    // 存储已发送文件的缓存，用于后续下载
    private static Map<String, byte[]> fileDataCache = new HashMap<>();
    // 存储已发送图片的缓存，用于显示
    private static Map<String, ImageIcon> imageCache = new HashMap<>();
    
    // 图片最大尺寸（用于预览）
    private static final int MAX_IMAGE_WIDTH = 300;
    private static final int MAX_IMAGE_HEIGHT = 300;
    
    /*
        构造函数
    */
    public MessageSender(Socket socket) {
        this.socket = socket;
        if (socket != null) {
            try {
                this.host = socket.getInetAddress().getHostAddress();
                this.port = socket.getPort();
            } catch (Exception e) {
                System.err.println("获取Socket地址和端口失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取文件数据
     * @param fileId 文件ID
     * @return 文件数据
     */
    public static byte[] getFileData(String fileId) {
        return fileDataCache.get(fileId);
    }
    
    /**
     * 获取图片缩略图
     * @param fileId 文件ID
     * @return 图片缩略图
     */
    public static ImageIcon getImageIcon(String fileId) {
        return imageCache.get(fileId);
    }
    
    /**
     * 检查连接状态，如果连接已关闭则尝试重连
     * @return 连接是否可用
     */
    private boolean ensureConnected() {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            if (reconnecting) {
                return false;
            }
            
            reconnecting = true;
            try {
                System.out.println("连接已关闭，尝试重新连接到服务器: " + host + ":" + port);
                socket = new Socket(host, port);
                System.out.println("重新连接成功");
                reconnecting = false;
                return true;
            } catch (IOException e) {
                System.err.println("重新连接服务器失败: " + e.getMessage());
                reconnecting = false;
                return false;
            }
        }
        return true;
    }
    
    /*
        发送登录请求
    */
    public boolean sendLoginRequest(String username, String password) {
        if (!ensureConnected()) {
            return false;
        }
        
        Login_info loginInfo = new Login_info();
        loginInfo.setUserName(username);
        loginInfo.setPassword(password);
        loginInfo.setLoginSuccessFlag(false);
        
        encap_info info = new encap_info();
        info.set_type(3);
        info.set_login_info(loginInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 发送注册请求
     */
    public boolean sendRegisterRequest(String username, String password) {
        if (!ensureConnected()) {
            return false;
        }
        
        Reg_info regInfo = new Reg_info();
        regInfo.setUsername(username);
        regInfo.setPassword(password);
        regInfo.setReg_status(0);
        
        encap_info info = new encap_info();
        info.set_type(5);
        info.set_reg_info(regInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送聊天消息
    */
    public boolean sendChatMessage(String fromUser, String message, boolean isGroupChat, String targetId) {
        if (!ensureConnected()) {
            return false;
        }
        
        System.out.println("MessageSender - 发送消息 - 发送者: " + fromUser);
        Chat_info chatInfo = new Chat_info();
        chatInfo.setType(isGroupChat);
        chatInfo.setFrom_username(fromUser);
        chatInfo.setText(message);
        
        if (isGroupChat) {
            chatInfo.setGroup_id(Integer.parseInt(targetId));
        } else {
            chatInfo.setTo_username(targetId);
        }
        
        encap_info info = new encap_info();
        info.set_type(4);
        info.set_chat_info(chatInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送创建群组请求
    */
    public boolean sendCreateGroupRequest(String groupName, List<String> members) {
        if (!ensureConnected()) {
            return false;
        }
        
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        System.out.println(members);//让我看看怎么个事
        groupInfo.setEstablish(true);
        groupInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送更新群组请求
    */
    public boolean sendUpdateGroupRequest(int groupId, String groupName, List<String> members) {
        if (!ensureConnected()) {
            return false;
        }
        
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.set_Group_name(groupName);
        groupInfo.setMembers(new ArrayList<>(members));
        groupInfo.setEstablish(false);
        groupInfo.setExist(true);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送退出群组请求
    */
    public boolean sendLeaveGroupRequest(int groupId) {
        if (!ensureConnected()) {
            return false;
        }
        
        Group_info groupInfo = new Group_info();
        groupInfo.set_Group_id(groupId);
        groupInfo.setExist(false);
        
        encap_info info = new encap_info();
        info.set_type(1);
        info.set_group_info(groupInfo);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送登出请求
    */
    public boolean sendLogoutRequest() {
        if (!ensureConnected()) {
            return false;
        }
        
        encap_info info = new encap_info();
        info.set_type(2);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /*
        发送断开连接通知
    */
    public boolean sendDisconnectNotification() {
        if (socket == null || socket.isClosed()) {
            return false;
        }
        
        encap_info info = new encap_info();
        info.set_type(6);
        
        return IOStream.writeMessage(socket, info);
    }
    
    /**
     * 获取当前Socket
     */
    public Socket getSocket() {
        return socket;
    }
    
    /**
     * 设置新的Socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
        if (socket != null) {
            try {
                this.host = socket.getInetAddress().getHostAddress();
                this.port = socket.getPort();
            } catch (Exception e) {
                System.err.println("获取Socket地址和端口失败: " + e.getMessage());
            }
        } else {
            // 如果socket为null，清除host和port
            this.host = null;
            this.port = 0;
        }
    }
    
    /**
     * 发送文件（私聊）
     * @param fromUser 发送者用户名
     * @param toUser 接收者用户名
     * @param file 要发送的文件
     * @param description 文件描述
     * @return 是否发送成功
     */
    public boolean sendPrivateFile(String fromUser, String toUser, File file, String description) {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 读取文件数据
            byte[] fileData = readFileData(file);
            
            // 生成唯一文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 将文件数据缓存起来，以便后续下载
            fileDataCache.put(fileId, fileData);
            
            // 创建文件信息对象（发送给接收方的，包含文件数据）
            File_info fileInfo = new File_info();
            fileInfo.setFileName(file.getName());
            fileInfo.setFileData(fileData);
            fileInfo.setFileSize(file.length());
            fileInfo.setFromUsername(fromUser);
            fileInfo.setToUsername(toUser);
            fileInfo.setGroupFile(false);
            fileInfo.setFileDescription(description);
            fileInfo.setFileId(fileId);
            
            // 封装信息
            encap_info info = new encap_info();
            info.set_type(7); // 7代表文件传输消息
            info.set_file_info(fileInfo);
            
            // 发送给接收方
            boolean success = IOStream.writeMessage(socket, info);
            
            // 创建一个只包含文件信息的副本，发送给自己（用于显示在自己的聊天窗口中）
            if (success) {
                // 创建只包含文件信息的对象（不包含文件数据）
                File_info selfInfo = new File_info();
                selfInfo.setFileName(file.getName());
                selfInfo.setFileSize(file.length());
                selfInfo.setFromUsername(fromUser);
                selfInfo.setToUsername(toUser);
                selfInfo.setGroupFile(false);
                selfInfo.setFileDescription(description);
                selfInfo.setFileId(fileId);
                selfInfo.setInfoOnly(true); // 标记为只包含信息
                
                // 创建聊天信息对象
                Chat_info chatInfo = new Chat_info();
                chatInfo.setType(false); // 私聊
                chatInfo.setFrom_username(fromUser);
                chatInfo.setTo_username(toUser);
                chatInfo.setText("[文件] " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                chatInfo.setTransfer_status(true);
                
                // 通知客户端模型更新
                MessageListener.notifyFileMessage(selfInfo, chatInfo);
            }
            
            return success;
        } catch (IOException e) {
            System.err.println("发送文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送群文件
     * @param fromUser 发送者用户名
     * @param groupId 群组ID
     * @param file 要发送的文件
     * @param description 文件描述
     * @return 是否发送成功
     */
    public boolean sendGroupFile(String fromUser, int groupId, File file, String description) {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 读取文件数据
            byte[] fileData = readFileData(file);
            
            // 生成唯一文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 将文件数据缓存起来，以便后续下载
            fileDataCache.put(fileId, fileData);
            
            // 创建文件信息对象（发送给群组成员的，包含文件数据）
            File_info fileInfo = new File_info();
            fileInfo.setFileName(file.getName());
            fileInfo.setFileData(fileData);
            fileInfo.setFileSize(file.length());
            fileInfo.setFromUsername(fromUser);
            fileInfo.setGroupId(groupId);
            fileInfo.setGroupFile(true);
            fileInfo.setFileDescription(description);
            fileInfo.setFileId(fileId);
            
            // 封装信息
            encap_info info = new encap_info();
            info.set_type(7); // 7代表文件传输消息
            info.set_file_info(fileInfo);
            
            // 发送给群组
            boolean success = IOStream.writeMessage(socket, info);
            
            // 创建一个只包含文件信息的副本，发送给自己（用于显示在自己的聊天窗口中）
            if (success) {
                // 创建只包含文件信息的对象（不包含文件数据）
                File_info selfInfo = new File_info();
                selfInfo.setFileName(file.getName());
                selfInfo.setFileSize(file.length());
                selfInfo.setFromUsername(fromUser);
                selfInfo.setGroupId(groupId);
                selfInfo.setGroupFile(true);
                selfInfo.setFileDescription(description);
                selfInfo.setFileId(fileId);
                selfInfo.setInfoOnly(true); // 标记为只包含信息
                
                // 创建聊天信息对象
                Chat_info chatInfo = new Chat_info();
                chatInfo.setType(true); // 群聊
                chatInfo.setFrom_username(fromUser);
                chatInfo.setGroup_id(groupId);
                chatInfo.setText("[文件] " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                chatInfo.setTransfer_status(true);
                
                // 通知客户端模型更新
                MessageListener.notifyFileMessage(selfInfo, chatInfo);
            }
            
            return success;
        } catch (IOException e) {
            System.err.println("发送群文件失败: " + e.getMessage());
            return false;
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
     * 读取文件数据
     * @param file 要读取的文件
     * @return 文件字节数组
     * @throws IOException 如果读取文件出错
     */
    private byte[] readFileData(File file) throws IOException {
        byte[] fileData = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(fileData);
        }
        return fileData;
    }
    
    /**
     * 发送私聊图片
     * @param fromUser 发送者用户名
     * @param toUser 接收者用户名
     * @param imageFile 图片文件
     * @param description 图片描述
     * @return 是否发送成功
     */
    public boolean sendPrivateImage(String fromUser, String toUser, File imageFile, String description) {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 检查是否为图片文件
            if (!File_info.checkIsImage(imageFile.getName())) {
                System.err.println("不是有效的图片文件: " + imageFile.getName());
                return false;
            }
            
            // 读取文件数据
            byte[] fileData = readFileData(imageFile);
            
            // 生成唯一文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 将文件数据缓存起来，以便后续下载
            fileDataCache.put(fileId, fileData);
            
            // 创建图片缩略图并缓存
            try {
                BufferedImage originalImage = ImageIO.read(imageFile);
                if (originalImage != null) {
                    ImageIcon thumbnail = createThumbnail(originalImage);
                    imageCache.put(fileId, thumbnail);
                }
            } catch (Exception e) {
                System.err.println("创建图片缩略图失败: " + e.getMessage());
            }
            
            // 创建文件信息对象（发送给接收方的，包含文件数据）
            File_info fileInfo = new File_info();
            fileInfo.setFileName(imageFile.getName());
            fileInfo.setFileData(fileData);
            fileInfo.setFileSize(imageFile.length());
            fileInfo.setFromUsername(fromUser);
            fileInfo.setToUsername(toUser);
            fileInfo.setGroupFile(false);
            fileInfo.setFileDescription(description);
            fileInfo.setFileId(fileId);
            // 图片类型会在setFileName中自动设置
            
            // 封装信息
            encap_info info = new encap_info();
            info.set_type(7); // 7代表文件传输消息
            info.set_file_info(fileInfo);
            
            // 发送给接收方
            boolean success = IOStream.writeMessage(socket, info);
            
            // 创建一个只包含文件信息的副本，发送给自己（用于显示在自己的聊天窗口中）
            if (success) {
                // 创建只包含文件信息的对象（不包含文件数据）
                File_info selfInfo = new File_info();
                selfInfo.setFileName(imageFile.getName());
                selfInfo.setFileSize(imageFile.length());
                selfInfo.setFromUsername(fromUser);
                selfInfo.setToUsername(toUser);
                selfInfo.setGroupFile(false);
                selfInfo.setFileDescription(description);
                selfInfo.setFileId(fileId);
                selfInfo.setInfoOnly(true); // 标记为只包含信息
                
                // 创建聊天信息对象
                Chat_info chatInfo = new Chat_info();
                chatInfo.setType(false); // 私聊
                chatInfo.setFrom_username(fromUser);
                chatInfo.setTo_username(toUser);
                chatInfo.setText("[图片] " + imageFile.getName());
                chatInfo.setTransfer_status(true);
                
                // 通知客户端模型更新
                MessageListener.notifyFileMessage(selfInfo, chatInfo);
            }
            
            return success;
        } catch (IOException e) {
            System.err.println("发送图片失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送群聊图片
     * @param fromUser 发送者用户名
     * @param groupId 群组ID
     * @param imageFile 图片文件
     * @param description 图片描述
     * @return 是否发送成功
     */
    public boolean sendGroupImage(String fromUser, int groupId, File imageFile, String description) {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 检查是否为图片文件
            if (!File_info.checkIsImage(imageFile.getName())) {
                System.err.println("不是有效的图片文件: " + imageFile.getName());
                return false;
            }
            
            // 读取文件数据
            byte[] fileData = readFileData(imageFile);
            
            // 生成唯一文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 将文件数据缓存起来，以便后续下载
            fileDataCache.put(fileId, fileData);
            
            // 创建图片缩略图并缓存
            try {
                BufferedImage originalImage = ImageIO.read(imageFile);
                if (originalImage != null) {
                    ImageIcon thumbnail = createThumbnail(originalImage);
                    imageCache.put(fileId, thumbnail);
                }
            } catch (Exception e) {
                System.err.println("创建图片缩略图失败: " + e.getMessage());
            }
            
            // 创建文件信息对象（发送给群组成员的，包含文件数据）
            File_info fileInfo = new File_info();
            fileInfo.setFileName(imageFile.getName());
            fileInfo.setFileData(fileData);
            fileInfo.setFileSize(imageFile.length());
            fileInfo.setFromUsername(fromUser);
            fileInfo.setGroupId(groupId);
            fileInfo.setGroupFile(true);
            fileInfo.setFileDescription(description);
            fileInfo.setFileId(fileId);
            // 图片类型会在setFileName中自动设置
            
            // 封装信息
            encap_info info = new encap_info();
            info.set_type(7); // 7代表文件传输消息
            info.set_file_info(fileInfo);
            
            // 发送给群组
            boolean success = IOStream.writeMessage(socket, info);
            
            // 创建一个只包含文件信息的副本，发送给自己（用于显示在自己的聊天窗口中）
            if (success) {
                // 创建只包含文件信息的对象（不包含文件数据）
                File_info selfInfo = new File_info();
                selfInfo.setFileName(imageFile.getName());
                selfInfo.setFileSize(imageFile.length());
                selfInfo.setFromUsername(fromUser);
                selfInfo.setGroupId(groupId);
                selfInfo.setGroupFile(true);
                selfInfo.setFileDescription(description);
                selfInfo.setFileId(fileId);
                selfInfo.setInfoOnly(true); // 标记为只包含信息
                
                // 创建聊天信息对象
                Chat_info chatInfo = new Chat_info();
                chatInfo.setType(true); // 群聊
                chatInfo.setFrom_username(fromUser);
                chatInfo.setGroup_id(groupId);
                chatInfo.setText("[图片] " + imageFile.getName());
                chatInfo.setTransfer_status(true);
                
                // 通知客户端模型更新
                MessageListener.notifyFileMessage(selfInfo, chatInfo);
            }
            
            return success;
        } catch (IOException e) {
            System.err.println("发送群聊图片失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建图片缩略图
     * @param originalImage 原始图片
     * @return 缩略图
     */
    private ImageIcon createThumbnail(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 计算缩放比例
        double scale = 1.0;
        if (originalWidth > MAX_IMAGE_WIDTH || originalHeight > MAX_IMAGE_HEIGHT) {
            double scaleWidth = (double) MAX_IMAGE_WIDTH / originalWidth;
            double scaleHeight = (double) MAX_IMAGE_HEIGHT / originalHeight;
            scale = Math.min(scaleWidth, scaleHeight);
        }
        
        // 计算缩放后的尺寸
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);
        
        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        
        return new ImageIcon(thumbnail);
    }

    /**Add commentMore actions
     * 发送语音通话消息
     * @param encapInfo 封装的语音通话信息
     * @return 是否发送成功
     */
    public boolean sendVoiceCallMessage(encap_info encapInfo) {
        if (!ensureConnected()) {
            return false;
        }

        // 确保消息类型正确
        if (encapInfo.get_type() != 8) {
            encapInfo.set_type(8);
        }

        return IOStream.writeMessage(socket, encapInfo);
    }
}