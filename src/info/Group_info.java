package info;
import java.io.Serializable;
import java.io.Serial;
import java.util.ArrayList;
public class Group_info implements Serializable {//该info用于封装群聊类型的消息。主要动作包含，群聊建立，群聊解散，群聊加人，群聊名字
    @Serial
    private static final long serialVersionUID = 4982100191297084473L;
    private String name;//群聊的名称
    private int group_id;//唯一识别id
    private ArrayList<String> members; // 群组成员列表
    private ArrayList<String> added_people;//添加成员
    private ArrayList<String> removed_people;//删去该成员
    private boolean establish;//是否为新建立的群聊
    private boolean exist;//用于告知用户，你是否还存在于这个群聊中
    
    public String get_Group_name(){
        return name;
    }
    
    public void set_Group_name(String name){
        this.name = name;
    }
    
    // 兼容旧方法
    public String get_Name(){
        return name;
    }
    
    public void set_Name(String name){
        this.name = name;
    }
    
    public int get_Group_id(){
        return group_id;
    }
    
    public void set_Group_id(int group_id){
        this.group_id = group_id;
    }
    
    public ArrayList<String> getMembers() {
        return members;
    }
    
    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }
    
    public ArrayList<String> get_added_people(){
        return added_people;
    }
    
    public void set_added_people(ArrayList<String> added_people){
        this.added_people = added_people;
    }
    
    public boolean isEstablish(){
        return establish;
    }
    
    public void setEstablish(boolean establish) {
        this.establish = establish;
    }
    
    public ArrayList<String> get_removed_people(){
        return removed_people;
    }
    
    public void set_removed_people(ArrayList<String> removed_people){
        this.removed_people = removed_people;
    }
    
    public boolean isExist(){
        return exist;
    }
    
    public void setExist(boolean exist){
        this.exist = exist;
    }

    @Override
    public String toString() {
        // 这将使得 Group_info 对象在 JComboBox 中默认显示为它的名称
        return this.name;
    }
}
