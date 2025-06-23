package Server.controller;

import Server.ChatServer;
import Server.ServerHandler;
import Server.model.ServerModel;
import Server.view.ServerWindow;
import info.*;
import io.FileIO;
import io.IOStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Queue;

public class ServerController {
    Socket socket;
    ChatServer server;
    ServerWindow ServerFrame;
    ServerHandler ServerHandler;
    HashMap<Integer,ArrayList<String>> OrgIDToUserList=new HashMap<>();//这是维护的一个，尚未接受邀请的小组的哈希表，是小组ID到未同意邀请的用户的列表
    //实例化model
    ServerModel model;
    String current_user;//标记当前线程服务的用户
    public ServerController(Socket socket, ChatServer server, ServerWindow ServerFrame, ServerHandler handler) {//定义构造方法
        this.socket = socket;
        this.server = server;
        this.ServerFrame = ServerFrame;
        this.ServerHandler = handler;
        model = new ServerModel(server);
    }
    public boolean Login_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Login_info tfi = INFO.get_login_info();
        ServerFrame.appendLog("尝试登录用户: " + tfi.getUserName());
        FileIO FI = new FileIO();
        FileIO FI_org = new FileIO("users.dat","orgs.dat");
        boolean flag = model.checkUserLogin(tfi);
        boolean isOline = model.checkUserOnline(tfi.getUserName(),this.server.online_users);//检查该用户是否已上线，支持漫游
        tfi.setLoginSuccessFlag(false);
        
        // 获取所有注册用户列表
        ArrayList<String> allUsers = FI.getAllUsers();
        
        if(flag) {
            //返回登录成功给客户端
            ServerFrame.appendLog("用户 " + tfi.getUserName() + " 登录成功");
            this.current_user = tfi.getUserName(); // 只有登录成功才设置当前用户
            this.server.add_online_socket(socket);
            if(isOline) {//如果用户已经上线了，那么需要把原来的socket干掉，然后发送一条被踢出的信息
                ServerFrame.appendLog("用户 "+ tfi.getUserName() + "已经在线，踢出原设备消息已发送");
                Socket old_socket = server.userSocketMap.get(this.current_user);
                ServerHandler oldHandler = server.SocketHandlerMap.get(old_socket);
                //返回被踢出消息
                tfi.setLoginSuccessFlag(true);
                tfi.setKicked(true);
                tfi.setOnlineUsers(server.online_users);//同步在线用户列表消息
                tfi.setAllUsers(allUsers); // 设置所有注册用户列表
                RETURN.set_login_info(tfi);
                RETURN.set_type(3);
                IOStream.writeMessage(old_socket, RETURN);
                //服务端数据注销
                ServerFrame.appendLog("服务端数据开始注销");
                ServerFrame.appendLog("开始关闭线程"+oldHandler);
                oldHandler.shutdown();//关闭原有线程
                ServerFrame.appendLog("旧线程关闭成功");
                this.server.remove_online_socket(old_socket);
                ServerFrame.appendLog("注销  "+ old_socket);
                this.server.userSocketMap.remove(this.current_user);
                this.server.SocketHandlerMap.remove(old_socket);
                tfi.setKicked(false);
            }else{//如果并非已经在线，那么
                server.add_online_user(tfi.getUserName());
                ServerFrame.appendLog("更新在线用户列表: " + server.online_users);
                tfi.setOnlineUsers(server.online_users);//同步在线用户列表消息
                tfi.setAllUsers(allUsers); // 设置所有注册用户列表
                //更新前端页面
                ServerFrame.updateUserList(server.online_users);
                //消息设置和封装
                tfi.setLoginSuccessFlag(true);
                RETURN.set_login_info(tfi);
                RETURN.set_type(3);
                ServerFrame.appendLog("通知所有用户 " + tfi.getUserName() + " 已上线");
                model.sendALL(INFO);//通知所有人，该用户已上线
            }
            //服务端记录该用户信息
            server.userSocketMap.put(tfi.getUserName(), socket);

            //发送所有的群聊信息给该用户
            ArrayList<Integer> groups = FI.getGroupsByUser(tfi.getUserName());//获得所有和该用户相关的groupID的表
            Map<Integer,ArrayList<String>> groupMap = model.groupMap(groups);
            tfi.setGroupMap(groupMap);
            tfi.setGroupIDList(groups);
            
            // 获取群组名称信息
            Map<Integer, String> groupNameMap = new HashMap<>();
            for (Integer groupId : groups) {
                String groupName = FI.getGroupName(groupId);
                groupNameMap.put(groupId, groupName);
            }
            tfi.setGroupNameMap(groupNameMap);

            //发送所有的组消息给该用户
            ArrayList<Integer> orgs = FI_org.getGroupsByUser(tfi.getUserName());
            Map<Integer,ArrayList<String>> orgMap = model.groupMap(orgs);
            tfi.setOrgMap(orgMap);
            tfi.setOrgIDList(orgs);

            RETURN.set_login_info(tfi);
            RETURN.set_type(3);
            IOStream.writeMessage(socket, RETURN);//单独给这个用户回这个稍微长一点的消息
            return true;
        }else {
            ServerFrame.appendLog("用户 " + tfi.getUserName() + " 登录失败");
            //返回登录失败给客户端
            // 即使登录失败，也需要设置在线用户列表，避免客户端出现NullPointerException
            tfi.setOnlineUsers(server.online_users);
            tfi.setAllUsers(allUsers); // 设置所有注册用户列表
            RETURN.set_login_info(tfi);
            RETURN.set_type(3);

            IOStream.writeMessage(socket , RETURN);
            return false;
        }
    }

    public void Chat_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Chat_info ci = INFO.get_chat_info();
        ci.setTransfer_status(true);
        FileIO FI = new FileIO();
        if(ci.isType()){//如果是群聊消息
            ServerFrame.appendLog("处理群聊消息，群ID: " + ci.getGroup_id() + "，发送者: " + current_user);
            ArrayList<String> group_members = FI.getGroupMembers(ci.getGroup_id());//通过id获取需要转发的成员列表
            model.filterOnlineMembers(group_members,this.server.online_users);//过滤一下非在线的人
            //已经确定要转发给谁了，那么就封装一下消息吧
            ServerFrame.appendLog("转发给在线群成员: " + group_members);
            RETURN.set_chat_info(ci);
            RETURN.set_type(4);
            model.Send2Users(RETURN, group_members);
        }else{//如果是私聊消息
            String to_user = ci.getTo_username();
            ServerFrame.appendLog(current_user + " 发送私聊消息给 " + to_user);
            if(this.server.online_users.contains(to_user)) {//如果在线的话
                RETURN.set_chat_info(ci);
                RETURN.set_type(4);
                Socket socket = server.userSocketMap.get(to_user);
                IOStream.writeMessage(socket , RETURN);//直接发消息了
                IOStream.writeMessage(this.socket , RETURN);//给自己也得回一个
                ServerFrame.appendLog("私聊消息已成功发送给 " + to_user);
            }else{//如果不在线，给自己回一个发送失败
                ServerFrame.appendLog(to_user + " 不在线，消息发送失败");
                ci.setTransfer_status(false);
                RETURN.set_chat_info(ci);
                RETURN.set_type(4);
                IOStream.writeMessage(this.socket , RETURN);//给自己
            }
        }
    }
    public void REG_handler(encap_info INFO,encap_info RETURN) throws IOException {
        Reg_info reg = INFO.get_reg_info();
        String username = reg.getUsername();
        FileIO fileio = new FileIO();
        ServerFrame.appendLog("尝试注册新用户: " + username);
        boolean flag = fileio.userExists(username);
        if(flag){//已经存在已有用户
            ServerFrame.appendLog("用户 " + username + " 已存在，注册失败");
            ServerFrame.appendLog("注册新用户: " + username);
            reg.setReg_status(2);//注册失败
        }else{
            reg.setReg_status(1);//注册成功
            fileio.writeUser(username,reg.getPassword());
        }
        RETURN.set_reg_info(reg);
        RETURN.set_type(5);
        IOStream.writeMessage(socket , RETURN);
    }
    public void Group_handler(encap_info INFO,encap_info RETURN) throws IOException {
        Group_info gi = INFO.get_group_info();
        if(gi.isEstablish()){//如果是建立群聊的消息
            ServerFrame.appendLog(current_user + " 尝试创建新群组");
            FileIO fileio = new FileIO();
            ArrayList<String> to_user = gi.getMembers();
            //为这个群聊分配一个随机ID
            int ID;
            while(true){
                Random rand = new Random();
                int randomInt = rand.nextInt();
                if (!fileio.groupExists(randomInt)){//如果生成的ID并非已存在，那么跳出循环
                    ID = randomInt;
                    break;
                }
            }
            //写入服务端的数据文件中
            System.out.println(ID);
            System.out.println(to_user);
            String groupName = gi.get_Group_name();
            // 如果名称为空，使用默认名称
            if (groupName == null || groupName.isEmpty()) {
                groupName = "群聊 " + ID;
            }
            ServerFrame.appendLog("创建新群组 ID: " + ID + ", 名称: " + groupName + ", 成员: " + to_user);
            fileio.writeGroup(ID, groupName, to_user);
            //添加回复消息，给所有人回复对应的添加消息，邀请他们进入群聊
            gi.set_Group_id(ID);
            model.Send2Users(INFO,to_user);
            ServerFrame.appendLog("已通知所有群成员: " + to_user);
        }else{//如果不是建立群聊，那么是对文件中进行修改
            ServerFrame.appendLog("修改群组 " + gi.get_Group_id() + " 的成员");
            FileIO fileio = new FileIO();
            ArrayList<String> added_people = gi.get_added_people();
            ArrayList<String> removed_people = gi.get_removed_people();
            fileio.manageGroupMembers(gi.get_Group_id(),added_people,removed_people);
            ServerFrame.appendLog("添加成员: " + added_people);
            //然后发消息，通知added_people被添加
            Group_info added = gi;
            added.setExist(true);
            RETURN.set_group_info(added);
            model.Send2Users(INFO,added_people);

            ServerFrame.appendLog("移除成员: " + removed_people);
            //发消息，告诉removed_people被删除
            Group_info removed = gi;
            removed.setExist(false);
            RETURN.set_group_info(removed);
            model.Send2Users(INFO,removed_people);
        }
    }
    public void LogoutHandler(encap_info INFO, encap_info RETURN) throws IOException {
        //维护相关动态表格
        ServerFrame.appendLog("用户 " + current_user + " 正在注销");
        
        // 获取所有注册用户列表
        FileIO FI = new FileIO();
        ArrayList<String> allUsers = FI.getAllUsers();
        
        // 先创建登出通知消息，在移除用户前准备好消息
        Login_info logoutInfo = new Login_info();
        logoutInfo.setUserName(current_user);
        logoutInfo.setLoginSuccessFlag(false); // 设置为false表示用户下线
        logoutInfo.setOnlineUsers(new ArrayList<>(server.online_users)); // 复制当前在线用户列表
        logoutInfo.setAllUsers(allUsers); // 设置所有注册用户列表
        
        // 从在线用户列表中移除当前用户
        this.server.userSocketMap.remove(current_user);
        this.server.online_users.remove(current_user);
        
        // 更新服务器界面的用户列表
        ServerFrame.updateUserList(server.online_users);
        this.server.online_sockets.remove(socket);
        
        // 更新登出通知消息中的在线用户列表（已移除当前用户）
        logoutInfo.setOnlineUsers(server.online_users);
        
        // 创建广播消息
        encap_info broadcastInfo = new encap_info();
        broadcastInfo.set_type(3); // 登录/登出消息类型
        broadcastInfo.set_login_info(logoutInfo);
        
        // 广播用户下线消息给所有在线用户
        ServerFrame.appendLog("广播用户 " + current_user + " 下线消息给所有在线用户");
        model.sendALL(broadcastInfo);
        
        ServerFrame.appendLog("用户 " + current_user + " 已成功注销");
        ServerFrame.appendLog("当前在线用户: " + server.online_users);
    }
    public void Org_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Org_info oi = INFO.get_org_info();
        FileIO fileio_org = new FileIO("users.dat","orgs.dat");//新建的一个组的数据文件
        FileIO fileio_group = new FileIO();//群消息读取，用于检查群聊是否存在
        int group_id =  oi.getGroup_id();
        ArrayList<String> org_members = oi.getMembers();
        ArrayList<String> group_members = fileio_group.getGroupMembers(group_id);
        if(oi.getType()==1){//如果是建立组的消息
            ServerFrame.appendLog(current_user + " 尝试创建新群内的组");
            boolean flag = model.IsInGroup(group_members,org_members);
            if(!fileio_group.groupExists(group_id)||flag){//如果群聊不存在，或者有人不在群聊中，那么告诉它，建立错误就行了
                ServerFrame.appendLog("错误，在尝试创建成员为： "+ org_members+"的组时发生错误，该群聊不存在，或者组中有人不在群聊中，返回报错信息");
                oi.setSuccess(false);
                ArrayList<String> back_user = new ArrayList<>();
                back_user.add(current_user);
                RETURN.set_org_info(oi);
                model.Send2Users(INFO,back_user);
                return;
            }else oi.setSuccess(true);
            //为这个群聊分配一个随机ID
            int ID;
            while(true){
                Random rand = new Random();
                int randomInt = rand.nextInt();
                if (!fileio_org.groupExists(randomInt)){//如果生成的ID并非已存在，那么跳出循环
                    ID = randomInt;
                    break;
                }
            }
            //把初始用户写入服务端的数据文件中
            ServerFrame.appendLog("创建新组 ID: " + ID + "，初始成员: " + oi.getFromUser());

            fileio_org.writeGroup(ID,oi.getOrg_name(),oi.getFromUser());//写入到文件中

            //把这些人都加入到哈希表的维护中
            group_members.remove(oi.getFromUser());
            model.addUserByOrgID(group_id,group_members);

            //添加回复消息，给所有人回复对应的添加消息，邀请他们进入群聊
            oi.setOrg_id(ID);
            oi.setType(2);
            oi.setExist(true);
            oi.setSuccess(true);

            model.Send2Users(INFO,org_members);
            ServerFrame.appendLog("已通知所有群成员: " + org_members);
        }else if(oi.getType()==4){//如果不是建立群聊，那么是对文件中进行修改
            ServerFrame.appendLog("修改群组 " + oi.getOrg_id() + " 的成员");
            FileIO fileio = new FileIO();
            ArrayList<String> added_people = oi.getAdded_people();
            ArrayList<String> removed_people = oi.getRemoved_people();
            if(!fileio_group.groupExists(group_id)||
                    model.IsInGroup(org_members,removed_people)||
            model.IsInGroup(group_members,added_people)){
                ServerFrame.appendLog("发生错误，在尝试添加用户 " + added_people + " 并删去用户"+ removed_people +
                        " 时发生错误");
                oi.setSuccess(false);
                ArrayList<String> back_user = new ArrayList<>();
                back_user.add(current_user);
                RETURN.set_org_info(oi);
                model.Send2Users(INFO,back_user);
                return;
            }
            fileio.manageGroupMembers(oi.getOrg_id(),added_people,removed_people);
            ServerFrame.appendLog("添加成员: " + added_people);
            //然后发消息，通知added_people被添加
            Org_info added = oi;
            added.setExist(true);
            added.setType(2);
            added.setSuccess(true);
            RETURN.set_org_info(added);
            model.Send2Users(INFO,added_people);
            ServerFrame.appendLog("移除成员: " + removed_people);
            //发消息，告诉removed_people被删除
            Org_info removed = oi;
            removed.setSuccess(true);
            removed.setExist(false);
            RETURN.set_org_info(removed);
            model.Send2Users(INFO,removed_people);
        }else if(oi.getType()==3){//如果接收到客户回复的同意邀请的结果，那么：
            if(oi.isSuccess()){//如果确实是同意邀请
                //首先检查用户是否在Hashmap中
                ArrayList<String> User_List = model.getUserByOrgID(group_id);
                if(User_List.contains(oi.getFromUser())){//如果包含，那么删除它，然后写入文件
                    model.removeUserByOrgID(group_id,oi.getFromUser());
                    //写入文件
                    fileio_org.writeGroup(group_id,oi.getOrg_name(),oi.getFromUser());
                }
            }else{//如果明确的拒绝邀请
                model.removeUserByOrgID(group_id,oi.getFromUser());
            }
        }
    }
    /**Add commentMore actions
     * 处理语音通话消息
     * @param INFO 接收到的语音通话消息
     * @param RETURN 响应消息
     * @throws IOException 如果IO操作失败
     */
    public void VoiceCall_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Voice_info voiceInfo = INFO.get_voice_info();
        if (voiceInfo == null) return;

        String fromUsername = voiceInfo.getFrom_username();
        ServerFrame.appendLog("接收到语音通话请求: 来自用户 " + fromUsername);
        
        // 获取发送者的实际IP地址
        InetAddress clientAddress = socket.getInetAddress();
        String realIpAddress = clientAddress.getHostAddress();
        ServerFrame.appendLog("发送方真实IP地址: " + realIpAddress);
        
        // 将实际IP添加到Voice_info对象中
        voiceInfo.setReal_host(realIpAddress);
        INFO.set_voice_info(voiceInfo);

        // 转发语音通话消息给目标用户
        List<String> participants = voiceInfo.getParticipants();
        if (participants != null && !participants.isEmpty()) {
            for (String participant : participants) {
                // 获取目标用户的Socket
                Socket targetSocket = server.getSocketByUsername(participant);
                if (targetSocket != null && !targetSocket.isClosed()) {
                    // 转发语音通话消息
                    IOStream.writeMessage(targetSocket, INFO);
                    ServerFrame.appendLog("转发语音通话消息到用户: " + participant);
                } else {
                    // 目标用户不在线，发送错误响应给发起者
                    ServerFrame.appendLog("用户 " + participant + " 不在线，无法接收语音通话");
                    voiceInfo.setError("用户 " + participant + " 不在线");
                    voiceInfo.setStatus(Voice_info.CallStatus.ERROR);
                    RETURN.set_type(8);
                    RETURN.set_voice_info(voiceInfo);
                    IOStream.writeMessage(socket, RETURN);
                }
            }
        }
    }
}
