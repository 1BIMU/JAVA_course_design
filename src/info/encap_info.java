package info;
import java.io.Serializable;

public class encap_info implements Serializable {//对各种消息进行封装
    private static final long serialVersionUID = -2664356335109440843L;
    
    // 消息类型常量
    public static final int TYPE_GROUP_MESSAGE = 1;     // 群管理消息
    public static final int TYPE_LOGOUT = 2;            // 登出
    public static final int TYPE_LOGIN = 3;             // 登录
    public static final int TYPE_CHAT = 4;              // 聊天类消息
    public static final int TYPE_REGISTER = 5;          // 注册消息
    public static final int TYPE_ORG = 6;               // 组管理消息
    public static final int TYPE_VOICE = 7;             // 语音通话消息
    
    private int type;// 这里是消息的类型，其中，1代表群管理消息，2代表登出，3代表登录，4代表聊天类的消息，5代表注册消息，6代表组管理消息，7代表语音通话消息
    private Chat_info chat_info;
    private Group_info group_info;
    private Login_info login_info;
    private Reg_info reg_info;
    private Org_info org_info;
    private Voice_info voice_info; // 新增语音通话信息
    
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

    public Org_info get_org_info() {
        return org_info;
    }
    
    public void set_org_info(Org_info org_info) {
        this.org_info = org_info;
    }
    
    public Voice_info get_voice_info() {
        return voice_info;
    }
    
    public void set_voice_info(Voice_info voice_info) {
        this.voice_info = voice_info;
    }
}
