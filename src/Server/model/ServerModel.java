package Server.model;

import Server.ChatServer;
import info.Login_info;
import info.encap_info;
import io.FileIO;
import io.IOStream;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerModel {
    ChatServer server;
    HashMap<Integer,ArrayList<String>> OrgIDToUserList=new HashMap<>();//这是维护的一个，尚未接受邀请的小组的哈希表，是小组ID到未同意邀请的用户的列表
    public void addUserByOrgID(int orgID,ArrayList<String> userList){//将用户添加到动态的列表中
        ArrayList<String> CurrUserList=OrgIDToUserList.get(orgID);
        if(CurrUserList==null){
            CurrUserList=new ArrayList<>();
        }
        ArrayList<String> mergedList = Stream.concat(CurrUserList.stream(), userList.stream())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        OrgIDToUserList.put(orgID,mergedList);
    }
    public ArrayList<String> getUserByOrgID(int orgID){
        return OrgIDToUserList.get(orgID);
    }
    public void removeUserByOrgID(int orgID, String User) {
        // 1. 检查 orgID 是否存在
        if (!OrgIDToUserList.containsKey(orgID)) {
            return; // 或抛出异常，如 throw new IllegalArgumentException("orgID 不存在");
        }

        // 2. 直接操作列表（无需 remove 和重新 put）
        ArrayList<String> currUserList = OrgIDToUserList.get(orgID);
        currUserList.remove(User);
        OrgIDToUserList.put(orgID,currUserList);
    }
    public ServerModel(ChatServer server) {
        this.server = server;
    }
    public boolean checkUserLogin(Login_info tfi) throws IOException {
        String userName = tfi.getUserName();
        String password = tfi.getPassword();
        FileIO fileio = new FileIO();
        return fileio.validateUser(userName,password);//用户名密码正确
    }
    public boolean checkUserOnline(String User,ArrayList<String> online_users){
        boolean flag = false;
        for(int i = 0;i<online_users.size();i++){
            if(online_users.get(i).equals(User)){
                flag = true;
                break;
            }
        }
        return flag;
    }
    public void sendALL(encap_info INFO){
        for (int i = 0; i < server.online_sockets.size(); i++) {
            Socket tempSocket = server.online_sockets.get(i);
            IOStream.writeMessage(tempSocket , INFO);
        }
    }
    /*
    * 过滤转发列表中非在线的用户
    * */
    public void filterOnlineMembers(ArrayList<String> group_members, ArrayList<String> online_users) {
        if(online_users.isEmpty()){
            return;
        }
        if(group_members==null){
            return;
        }
        // 将在线用户列表转为 HashSet 提高查找效率
        Set<String> onlineSet = new HashSet<>(online_users);

        // 使用 removeIf 方法过滤不在线成员（Java 8+ 特性）
        group_members.removeIf(member -> !onlineSet.contains(member));
    }
    /*
    * 转发消息到指定用户
    * */
    public void Send2Users(encap_info INFO, ArrayList<String> to_user){
        // --- MODIFIED --- 添加了安全检查
        if(to_user == null || to_user.isEmpty()){
            return;
        }
        for(String user : to_user) {
            //先从hashmap中拿到对应用户的socket
            Socket tempSocket = server.userSocketMap.get(user);

            // 安全检查：只在socket不为null时发送
            if (tempSocket != null) {
                IOStream.writeMessage(tempSocket , INFO);
            }
        }
    }
    /*
    * 该方法用于将得到的ID表，构建一个对应到具体用户的映射
    * */

/*
* 用于检查，组中是否存在，不在群聊中的人，两个for，比较丑陋
* */
    public boolean IsInGroup(ArrayList<String> group_members, ArrayList<String> org_members) {
        boolean flag = false;//是否存在不在群聊中的人
        for (String orgMember : org_members) {
            boolean is_member = false;
            for (String groupMember : group_members) {
                if (orgMember.equals(groupMember)) {
                    is_member = true;//在群中
                    break;
                }
            }
            if (!is_member) {
                flag = true;
                break;
            }
        }
        return flag;
    }
    /**
     * 该方法现在可以为任何成员文件（群组或小组）构建成员映射
     * @param idList ID列表 (可以是群聊ID列表，也可以是小组ID列表)
     * @param fileio 指定要读取哪个文件的 FileIO 实例
     * @return ID到成员列表的映射
     */
    public Map<Integer, ArrayList<String>> groupMap(ArrayList<Integer> idList, FileIO fileio) throws IOException {
        Map<Integer, ArrayList<String>> resultMap = new HashMap<>();
        if (idList == null || fileio == null) {
            return resultMap;
        }

        for (Integer id : idList) {
            ArrayList<String> members = fileio.getGroupMembers(id);
            resultMap.put(id, members); // 即使找不到成员(null)，也放进去，让调用方处理
        }
        return resultMap;
    }

}
