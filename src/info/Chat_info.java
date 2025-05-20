package info;
import java.io.Serial;
import java.io.Serializable;
//聊天消息封装
public class Chat_info implements Serializable {
    @Serial
            private static final long serialVersionUID = -5761256693412260126L;
    String message;//消息主体
    String from_username;//当前用户
    String[] to_username;//给到那些用户，需要包含from_username
    int chat_id;//当前群聊的ID
    boolean transfer_status;
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
    public String[] getTo_username() {
        return to_username;
    }
    public void setTo_username(String[] to_username) {
        this.to_username = to_username;
    }
    public boolean getTransfer_status() {
        return transfer_status;
    }
    public void setTransfer_status(boolean transfer_status) {
        this.transfer_status = transfer_status;
    }
    public int getChat_id() {
        return chat_id;
    }
    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }
}


