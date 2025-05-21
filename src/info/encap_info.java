package info;
import java.io.Serializable;
import java.io.Serial;

public class encap_info implements Serializable {//对各种消息进行封装
    @Serial
    private static final long serialVersionUID = -2664356335109440843L;
    int type;// 这里是消息的类型，其中，1代表群管理消息，2代表登出，3代表登录，4代表聊天类的消息，5代表注册消息，如果要添加消息，直接在后面改就行
    Chat_info chat_info;
    Group_info group_info;
    Login_info login_info;
    Reg_info reg_info;
    public Chat_info get_chat_info(){
        return chat_info;
    }
    public Group_info get_group_info(){
        return group_info;
    }
    public Login_info get_login_info(){
        return login_info;
    }
    public void set_chat_info(Chat_info chat_info){
        this.chat_info = chat_info;
    }
    public void set_group_info(Group_info group_info){
        this.group_info = group_info;
    }
    public void set_login_info(Login_info login_info) {
        this.login_info = login_info;
    }
    public int get_type() {
        return type;
    }
    public void set_type(int type) {
        this.type = type;
    }
    public Reg_info get_reg_info() {
        return reg_info;
    }
    public void set_reg_info(Reg_info reg_info) {
        this.reg_info = reg_info;
    }
}
