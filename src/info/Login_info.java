package info;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
//登录消息封装，服务端和客户端
public class Login_info implements Serializable{//每次登录成功之后或者登出成功之后，都要跟其他的人同步当前用户的列表
    @Serial
    private static final long serialVersionUID = -4476989302262161367L;

    private String userName;
    private String password;
    private ArrayList<String> onlineUsers;
    //登录成功标志
    private Boolean loginSucceessFlag = false;

    public Boolean getLoginSucceessFlag() {
        return loginSucceessFlag;
    }
    public void setLoginSucceessFlag(Boolean loginSucceessFlag) {
        this.loginSucceessFlag = loginSucceessFlag;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public ArrayList<String> getOnlineUsers() {
        return onlineUsers;
    }
    public void setOnlineUsers(ArrayList<String> onlineUsers) {
        this.onlineUsers = onlineUsers;
    }
}
