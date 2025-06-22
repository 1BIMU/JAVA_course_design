package info;

import java.io.Serializable;

/**
 * 文件传输信息类，用于封装文件传输相关的信息
 */
public class File_info implements Serializable {
    private static final long serialVersionUID = -3664356335109440844L;
    
    private String fileName;        // 文件名
    private byte[] fileData;        // 文件数据
    private long fileSize;          // 文件大小
    private String fromUsername;    // 发送者用户名
    private String toUsername;      // 接收者用户名（私聊时使用）
    private int groupId;            // 群组ID（群聊时使用）
    private boolean isGroupFile;    // 是否为群文件
    private String fileDescription; // 文件描述
    private String fileId;          // 文件唯一标识符
    private boolean infoOnly = false; // 是否只包含文件信息，不包含文件数据
    private boolean isImage = false;  // 是否为图片文件
    private String mimeType;          // 文件MIME类型
    
    /**
     * 检查文件是否为图片
     * @return 是否为图片
     */
    public boolean isImage() {
        return isImage;
    }
    
    /**
     * 设置文件是否为图片
     * @param isImage 是否为图片
     */
    public void setImage(boolean isImage) {
        this.isImage = isImage;
    }
    
    /**
     * 获取文件MIME类型
     * @return 文件MIME类型
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * 设置文件MIME类型
     * @param mimeType 文件MIME类型
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    /**
     * 根据文件名判断是否为图片
     * @param fileName 文件名
     * @return 是否为图片
     */
    public static boolean checkIsImage(String fileName) {
        if (fileName == null) return false;
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
               lowerName.endsWith(".png") || lowerName.endsWith(".gif") || 
               lowerName.endsWith(".bmp");
    }
    
    /**
     * 根据文件名获取MIME类型
     * @param fileName 文件名
     * @return MIME类型
     */
    public static String getMimeTypeFromFileName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "application/octet-stream";
        }
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
        // 自动检测是否为图片
        if (fileName != null) {
            this.isImage = checkIsImage(fileName);
            this.mimeType = getMimeTypeFromFileName(fileName);
        }
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFromUsername() {
        return fromUsername;
    }
    
    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }
    
    public String getToUsername() {
        return toUsername;
    }
    
    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }
    
    public int getGroupId() {
        return groupId;
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    public boolean isGroupFile() {
        return isGroupFile;
    }
    
    public void setGroupFile(boolean groupFile) {
        isGroupFile = groupFile;
    }
    
    public String getFileDescription() {
        return fileDescription;
    }
    
    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public boolean isInfoOnly() {
        return infoOnly;
    }
    
    public void setInfoOnly(boolean infoOnly) {
        this.infoOnly = infoOnly;
    }
} 