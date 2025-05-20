package info;
//用于定时同步数据的消息类型，可以一遍用一边往里加
import java.io.Serializable;
import java.io.Serial;
import java.util.ArrayList;
public class Sync_info implements Serializable {
    @Serial
    private static final long serialVersionUID = -3657149350794892289L;
    ArrayList<String> onlineUsers;
    public ArrayList<String> get_onlineUsers(){
        return onlineUsers;
    }
    public void set_onlineUsers(ArrayList<String> onlineUsers){
        this.onlineUsers = onlineUsers;
    }
}
