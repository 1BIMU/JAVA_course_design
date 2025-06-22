package info;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class Org_info implements Serializable {//组类型的消息
    //不得不说这个组消息是纯构式，没啥屁用，纯增加复杂度的东西
    @Serial
    private static final long serialVersionUID = 1L;
    private int Group_id;//属于的群的ID
    private String Org_name;//组名
    private int Org_id;//组id
    private String FromUser;
    private ArrayList<String> members; // 群组成员列表
    private ArrayList<String> added_people;//添加成员
    private ArrayList<String> removed_people;//删去该成员
    private int type;//1代表建立小组，2代表邀请用户信息，3代表用户对邀请信息的响应，4代表添加用户信息
    private boolean exist;//用于告知用户，你是否还存在于这个组中
    private boolean success;//用于判断消息是否成功
    public int getType() {
        return type;
    }
    public void setFromUser(String fromUser) {
        FromUser = fromUser;
    }
    public String getFromUser() {
        return FromUser;
    }
    public void setType(int type) {this.type = type;}
    public void setExist(boolean exist) {
        this.exist = exist;
    }
    public boolean isExist() {
        return exist;
    }
    public void setGroup_id(int Group_id) {
        this.Group_id = Group_id;
    }
    public int getGroup_id() {
        return Group_id;
    }
    public void setOrg_name(String Org_name) {
        this.Org_name = Org_name;
    }
    public String getOrg_name() {
        return Org_name;
    }
    public void setOrg_id(int Org_id) {
        this.Org_id = Org_id;
    }
    public int getOrg_id() {
        return Org_id;
    }
    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }
    public ArrayList<String> getMembers() {
        return members;
    }
    public void setAdded_people(ArrayList<String> added_people) {
        this.added_people = added_people;
    }
    public ArrayList<String> getAdded_people() {
        return added_people;
    }
    public void setRemoved_people(ArrayList<String> removed_people) {
        this.removed_people = removed_people;
    }
    public ArrayList<String> getRemoved_people() {
        return removed_people;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean isSuccess() {
        return success;
    }
}
