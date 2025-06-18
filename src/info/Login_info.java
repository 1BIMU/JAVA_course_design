package info;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

//登录消息封装，服务端和客户端
public class Login_info implements Serializable{//每次登录成功之后或者登出成功之后，都要跟其他的人同步当前用户的列表
    @Serial
    private static final long serialVersionUID = -4476989302262161367L;

    //添加同步信息，在用户登录时发送在服务端存储的所有和该用户相关的群，包括群ID，群所有成员
    private ArrayList<Integer> groupIDList;
    private Map<Integer,ArrayList<String>> groupMap;
    private boolean Kicked = false;//默认为false，标记是否被漫游用户挤下去
    private String userName;
    private String password;
    private ArrayList<String> onlineUsers;
    //登录成功标志
    private Boolean loginSuccessFlag = false;

    public void setKicked(boolean kicked) {
        Kicked = kicked;
    }
    public boolean isKicked() {
        return Kicked;
    }
    public void setGroupIDList(ArrayList<Integer> groupIDList) {
        this.groupIDList = groupIDList;
    }
    public void setGroupMap(Map<Integer,ArrayList<String>> groupMap) {
        this.groupMap = groupMap;
    }
    public Map<Integer,ArrayList<String>> getGroupMap() {
        return groupMap;
    }
    public ArrayList<Integer> getGroupIDList() {
        return groupIDList;
    }
    public Boolean getLoginSuccessFlag() {
        return loginSuccessFlag;
    }
    public void setLoginSuccessFlag(Boolean loginSuccessFlag) {
        this.loginSuccessFlag = loginSuccessFlag;
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
