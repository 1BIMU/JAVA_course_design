package info;
//聊天消息封装
public class Chat_info {
    String message;
    String from_username;
    String[] to_username;
    String chat_name;//当前聊天室的名字，用于同步聊天室的ID
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
    public String getChat_name() {
        return chat_name;
    }
    public void setChat_name(String chat_name) {
        this.chat_name = chat_name;
    }
}


