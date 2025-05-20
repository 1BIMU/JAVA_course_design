package info;
import java.io.Serializable;
import java.io.Serial;
public class Group_info implements Serializable {//该info用于封装群聊类型的消息。主要动作包含，群聊建立，群聊解散，群聊加人，群聊名字
    @Serial
    private static final long serialVersionUID = 4982100191297084473L;
    String name;//群聊的名称
    int group_id;//唯一识别id
    String[] added_people;
    String[] removed_people;
    boolean establish;//是否为新建立的群聊
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
    public String[] getAdded_people(){
        return added_people;
    }
    public void setAdded_people(String[] added_people){
        this.added_people = added_people;
    }
    public String[] getRemoved_people(){
        return removed_people;
    }
    public void setRemoved_people(String[] removed_people){
        this.removed_people = removed_people;
    }
    public boolean isEstablish(){
        return establish;
    }
    public void setEstablish(boolean establish) {
        this.establish = establish;
    }
}
