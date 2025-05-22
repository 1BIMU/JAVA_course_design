package info;
import java.io.Serial;
import java.io.Serializable;
//聊天消息封装
public class Chat_info implements Serializable {
    @Serial
    private static final long serialVersionUID = -5761256693412260126L;
    private String message;//消息主体
    private boolean type;//标记是私聊消息还是群聊消息，如果为0，那么是私聊消息，如果为1，那么是群聊消息
    private String from_username;//当前用户
    private String to_username;//给到那些用户，需要包含from_username
    private int group_id;//当前群聊的ID
    private boolean transfer_status;
    
    public Chat_info() {
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
}


