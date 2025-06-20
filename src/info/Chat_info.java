package info;
import java.io.Serializable;
//聊天消息封装
public class Chat_info implements Serializable {
    private static final long serialVersionUID = -5761256693412260126L;
    private String text;//字符串消息主体
    private byte[] image;//二进制图片数据
    private String imageName;//图片名称

    private int message_type = 0;//消息类型 0为字符串型消息，1为图片类型消息，电话的方式不在这里做了，用UDP来做
    private boolean type;//标记是私聊消息还是群聊消息，如果为0，那么是私聊消息，如果为1，那么是群聊消息
    private boolean isOrgMessage = false; // 是否为小组消息
    private String from_username;//当前用户
    private String to_username;//给到那些用户，需要包含from_username
    private int group_id;//当前群聊的ID
    private int org_id = -1; // 小组ID，-1表示不属于任何小组
    private boolean transfer_status;
    
    public Chat_info() {
    }

    public void setMessageType(int message_type) {
        this.message_type = message_type;
    }

    public int getMessageType() {
        return message_type;
    }

    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getFrom_username() {
        return from_username;
    }
    
    public void setFrom_username(String from_username) {
        this.from_username = from_username;
    }
    
    public String getTo_username() {
        return to_username;
    }
    
    public void setTo_username(String to_username) {
        this.to_username = to_username;
    }
    
    public boolean getTransfer_status() {
        return transfer_status;
    }
    
    public void setTransfer_status(boolean transfer_status) {
        this.transfer_status = transfer_status;
    }
    
    public int getGroup_id() {
        return group_id;
    }
    
    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }
    
    public boolean isType() {
        return type;
    }
    
    public void setType(boolean type) {
        this.type = type;
    }

    public void setImageData(byte[] data, String fileName) {
        this.image = data;
        this.imageName = fileName;
        this.message_type = 1;//自动设置为类型1的消息
    }

    public byte[] getImageData() {
        return image;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    /**
     * 获取消息是否为小组消息
     * @return 是否为小组消息
     */
    public boolean isOrgMessage() {
        return isOrgMessage;
    }
    
    /**
     * 设置消息是否为小组消息
     * @param isOrgMessage 是否为小组消息
     */
    public void setOrgMessage(boolean isOrgMessage) {
        this.isOrgMessage = isOrgMessage;
    }
    
    /**
     * 获取小组ID
     * @return 小组ID，-1表示不属于任何小组
     */
    public int getOrg_id() {
        return org_id;
    }
    
    /**
     * 设置小组ID
     * @param org_id 小组ID
     */
    public void setOrg_id(int org_id) {
        this.org_id = org_id;
    }

}


