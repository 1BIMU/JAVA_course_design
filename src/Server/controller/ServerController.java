package Server.controller;

import Server.ChatServer;
import Server.ServerHandler;
import Server.model.ServerModel;
import Server.view.ServerWindow;
import info.*;
import io.FileIO;
import io.IOStream;

import java.io.IOException;
import java.lang.reflect.Array;
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
        Login_info clientLoginRequest = INFO.get_login_info();
        ServerFrame.appendLog("尝试登录用户: " + clientLoginRequest.getUserName());

        FileIO fileIO = new FileIO();
        boolean loginSuccess = model.checkUserLogin(clientLoginRequest);
        ArrayList<String> allUsers = fileIO.getAllUsers();

        // 1. 处理登录失败的情况
        if (!loginSuccess) {
            ServerFrame.appendLog("用户 " + clientLoginRequest.getUserName() + " 登录失败");
            clientLoginRequest.setLoginSuccessFlag(false);
            clientLoginRequest.setAllUsers(allUsers);
            RETURN.set_login_info(clientLoginRequest);
            RETURN.set_type(3);
            IOStream.writeMessage(socket, RETURN);
            return false;
        }

        // --- 登录成功后的逻辑 ---
        ServerFrame.appendLog("用户 " + clientLoginRequest.getUserName() + " 登录成功");
        this.current_user = clientLoginRequest.getUserName();

        // 2. 处理重复登录（踢出旧连接）
        if (model.checkUserOnline(current_user, this.server.online_users)) {
            // ... 此处省略您已有的、正确的踢人逻辑 ...
            // 注意：踢人逻辑执行后，当前线程应该结束，不应再往下执行
        }

        // 3. 准备【广播】给所有人的“上线通知”
        // 创建一个【新的、干净的】Login_info 用于广播
        Login_info broadcastInfo = new Login_info();
        broadcastInfo.setUserName(current_user);
        broadcastInfo.setLoginSuccessFlag(true); // 表示是上线通知
        // 更新在线列表并加入通知
        server.add_online_user(current_user);
        ServerFrame.updateUserList(server.online_users);
        broadcastInfo.setOnlineUsers(server.online_users);
        broadcastInfo.setAllUsers(allUsers);

        encap_info broadcastEncap = new encap_info();
        broadcastEncap.set_type(3);
        broadcastEncap.set_login_info(broadcastInfo);
        ServerFrame.appendLog("向所有人广播 " + current_user + " 已上线...");
        model.sendALL(broadcastEncap); // 广播这个干净的通知，而不是原始的INFO

        // 4. 准备【只发给登录者本人】的详细数据回执
        // 创建另一个【新的、干净的】Login_info 用于详细回执
        Login_info detailedResponseInfo = new Login_info();
        detailedResponseInfo.setUserName(current_user);
        detailedResponseInfo.setLoginSuccessFlag(true);
        detailedResponseInfo.setOnlineUsers(server.online_users);
        detailedResponseInfo.setAllUsers(allUsers);

        // 添加群聊信息
        ArrayList<Integer> groups = fileIO.getGroupsByUser(current_user);
        Map<Integer,ArrayList<String>> groupMap = model.groupMap(groups, fileIO);
        Map<Integer, String> groupNameMap = new HashMap<>();
        for (Integer groupId : groups) {
            groupNameMap.put(groupId, fileIO.getGroupName(groupId));
        }
        detailedResponseInfo.setGroupIDList(groups);
        detailedResponseInfo.setGroupMap(groupMap);
        detailedResponseInfo.setGroupNameMap(groupNameMap);

        // 添加小组信息
        FileIO fileio_org = new FileIO("users.dat", "orgs.dat");
        ArrayList<Integer> orgs = fileio_org.getGroupsByUser(current_user);
        Map<Integer, ArrayList<String>> orgMap = model.groupMap(orgs,fileio_org);
        Map<Integer, String> orgNameMap = new HashMap<>();
        for (Integer orgId : orgs) {
            orgNameMap.put(orgId, fileio_org.getGroupName(orgId));
        }
        detailedResponseInfo.setOrgIDList(orgs);
        detailedResponseInfo.setOrgMap(orgMap);
        detailedResponseInfo.setOrgNameMap(orgNameMap);
        ServerFrame.appendLog("为用户 " + current_user + " 同步 " + orgs.size() + " 个小组信息。");

        // 5. 发送详细回执给当前登录者
        RETURN.set_login_info(detailedResponseInfo);
        RETURN.set_type(3);
        IOStream.writeMessage(socket, RETURN);

        // 6. 更新服务器内部状态
        server.userSocketMap.put(current_user, socket);
        server.add_online_socket(socket);

        return true;
    }

    public void Chat_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Chat_info ci = INFO.get_chat_info();
        ci.setTransfer_status(true);
        FileIO FI = new FileIO();
        int targetId = ci.getGroup_id();
        ArrayList<String> members;
        if(ci.isType()){//如果是群聊消息
            if (ci.isOrg()) {
                // 如果是小组消息, 从 orgs.dat 文件读取
                ServerFrame.appendLog("处理小组聊天消息，小组ID: " + targetId);
                FileIO teamFileIO = new FileIO("users.dat", "orgs.dat");
                members = teamFileIO.getGroupMembers(targetId);
            } else {
                // 否则，是普通群聊消息, 从 groups.dat 文件读取
                ServerFrame.appendLog("处理群聊消息，群聊ID: " + targetId);
                FileIO groupFileIO = new FileIO();
                members = groupFileIO.getGroupMembers(targetId);
            }

            ServerFrame.appendLog("处理群类消息，群ID: " + ci.getGroup_id() + "，发送者: " + current_user);

            model.filterOnlineMembers(members,this.server.online_users);//过滤一下非在线的人
            //已经确定要转发给谁了，那么就封装一下消息吧
            ServerFrame.appendLog("转发给在线成员: " + members);
            RETURN.set_chat_info(ci);
            RETURN.set_type(4);
            model.Send2Users(RETURN, members);
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
        FileIO fileio_org = new FileIO("users.dat", "orgs.dat");//新建的一个组的数据文件
        FileIO fileio_group = new FileIO();//群消息读取，用于检查群聊是否存在
        int group_id = oi.getGroup_id();
        ArrayList<String> org_members = oi.getMembers();
        ArrayList<String> group_members = fileio_group.getGroupMembers(group_id);
        if (oi.getType() == 1) {//如果是建立组的消息
            ServerFrame.appendLog(current_user + " 尝试创建新群内的组");
            boolean flag = model.IsInGroup(group_members, org_members);
            if (!fileio_group.groupExists(group_id) || flag || isAnyMemberAlreadyInTeam(oi.getMembers(), fileio_org)) {//如果群聊不存在，或者有人不在群聊中，那么告诉它，建立错误就行了
                ServerFrame.appendLog("错误，在尝试创建成员为： " + org_members + "的组时发生错误，该群聊不存在，或者组中有人不在群聊中，或者有人已经有小组了，返回报错信息");
                //后续可以考虑加个错误信息啥的，这里先不做了
                oi.setSuccess(false);
                ArrayList<String> back_user = new ArrayList<>();
                back_user.add(current_user);
                RETURN.set_org_info(oi);
                model.Send2Users(INFO, back_user);
                return;
            } else oi.setSuccess(true);
            //为这个群聊分配一个随机ID
            int ID;
            while (true) {
                Random rand = new Random();
                int randomInt = rand.nextInt();
                if (!fileio_org.groupExists(randomInt)) {//如果生成的ID并非已存在，那么跳出循环
                    ID = randomInt;
                    break;
                }
            }
            //把初始用户写入服务端的数据文件中
            ServerFrame.appendLog("创建新组 ID: " + ID + "，初始成员: " + oi.getFromUser());
            ArrayList<String> creatorList = new ArrayList<>();
            creatorList.add(oi.getFromUser());
            fileio_org.writeGroup(ID, oi.getOrg_name(), creatorList);

            //把这些人都加入到哈希表的维护中
            group_members.remove(oi.getFromUser());
            model.addUserByOrgID(group_id, group_members);

            Org_info creationSuccessInfo = new Org_info();
            creationSuccessInfo.setSuccess(true);
            creationSuccessInfo.setType(1); // 类型1代表创建成功
            creationSuccessInfo.setOrg_id(ID);
            creationSuccessInfo.setOrg_name(oi.getOrg_name());
            creationSuccessInfo.setMembers(creatorList);
            creationSuccessInfo.setGroup_id(oi.getGroup_id());

            RETURN.set_org_info(creationSuccessInfo);
            RETURN.set_type(6); // 信封类型是6
            IOStream.writeMessage(socket, RETURN); // 直接使用当前线程的socket发回给创建者
            ServerFrame.appendLog("已向创建者 " + oi.getFromUser() + " 发送创建成功确认。");


            //添加回复消息，给所有人回复对应的添加消息，邀请他们进入群聊
            ArrayList<String> invitees = new ArrayList<>(org_members);
            invitees.remove(oi.getFromUser());

            if (!invitees.isEmpty()) {
                // 准备邀请消息
                oi.setOrg_id(ID);
                oi.setType(2); // 类型2是邀请
                oi.setExist(true);
                oi.setSuccess(true);

                // 创建一个新的信封用于广播邀请
                encap_info invitationBroadcast = new encap_info();
                invitationBroadcast.set_type(6);
                invitationBroadcast.set_org_info(oi);

                model.Send2Users(invitationBroadcast, invitees);
                ServerFrame.appendLog("已向 " + invitees + " 发送小组邀请。");
            }
        } else if (oi.getType() == 4) {//如果不是建立群聊，那么是对文件中进行修改
            ServerFrame.appendLog("修改群组 " + oi.getOrg_id() + " 的成员");
            FileIO fileio = new FileIO();
            ArrayList<String> added_people = oi.getAdded_people();
            ArrayList<String> removed_people = oi.getRemoved_people();
            if (!fileio_group.groupExists(group_id) ||
                    model.IsInGroup(org_members, removed_people) ||
                    model.IsInGroup(group_members, added_people)) {
                ServerFrame.appendLog("发生错误，在尝试添加用户 " + added_people + " 并删去用户" + removed_people +
                        " 时发生错误");
                oi.setSuccess(false);
                ArrayList<String> back_user = new ArrayList<>();
                back_user.add(current_user);
                RETURN.set_org_info(oi);
                model.Send2Users(INFO, back_user);
                return;
            }
            fileio.manageGroupMembers(oi.getOrg_id(), added_people, removed_people);
            ServerFrame.appendLog("添加成员: " + added_people);
            //然后发消息，通知added_people被添加
            Org_info added = oi;
            added.setExist(true);
            added.setType(2);
            added.setSuccess(true);
            RETURN.set_org_info(added);
            model.Send2Users(INFO, added_people);
            ServerFrame.appendLog("移除成员: " + removed_people);
            //发消息，告诉removed_people被删除
            Org_info removed = oi;
            removed.setSuccess(true);
            removed.setExist(false);
            RETURN.set_org_info(removed);
            model.Send2Users(INFO, removed_people);
        } else if (oi.getType() == 3) {//如果接收到客户回复的同意邀请的结果，那么：
            if (oi.isSuccess()) {//如果确实是同意邀请
                String fromUser = oi.getFromUser();
                int orgId = oi.getOrg_id(); // 关键：使用正确的小组ID
                String orgName = fileio_org.getGroupName(orgId);

                ServerFrame.appendLog("用户 " + fromUser + " 接受了小组 " + orgName + " 的邀请。");

                // --- 正确的更新逻辑 ---
                // 1. 先从文件读取该小组当前的完整成员列表
                ArrayList<String> currentMembers = fileio_org.getGroupMembers(orgId);
                if (currentMembers == null) {
                    // 如果小组不存在，创建一个新的列表
                    currentMembers = new ArrayList<>();
                }

                // 2. 将新成员添加进去 (如果不存在的话)
                if (!currentMembers.contains(fromUser)) {
                    currentMembers.add(fromUser);
                }

                // 3. 将更新后的完整成员列表写回到文件
                fileio_org.writeGroup(orgId, orgName, currentMembers);
                ServerFrame.appendLog("已将 " + fromUser + " 添加到小组 " + orgId + "。当前成员: " + currentMembers);

                // ------------------------

                // 从待处理邀请列表中移除该用户 (如果需要的话，您的 model 里有这个逻辑)
                model.removeUserByOrgID(orgId, fromUser);

                // 准备并发送包含完整、最新信息的确认回执给客户端
                Org_info updatedOrgInfo = new Org_info();
                updatedOrgInfo.setSuccess(true);
                updatedOrgInfo.setType(3); // 使用类型3作为“加入成功”的确认
                updatedOrgInfo.setOrg_id(orgId);
                updatedOrgInfo.setOrg_name(orgName);
                updatedOrgInfo.setMembers(currentMembers); // <-- 发送更新后的完整成员列表
                updatedOrgInfo.setGroup_id(oi.getGroup_id());

                RETURN.set_org_info(updatedOrgInfo);
                RETURN.set_type(6);
                IOStream.writeMessage(socket, RETURN);

                ServerFrame.appendLog("已向 " + fromUser + " 发送小组更新确认。");
                ArrayList<String> broadcastList = new ArrayList<>(currentMembers);
                broadcastList.remove(fromUser); // 移除自己，因为已经单独通知过了
                model.filterOnlineMembers(broadcastList, server.online_users); // 筛选出在线的人

                if (!broadcastList.isEmpty()) {
                    model.Send2Users(RETURN, broadcastList);
                    ServerFrame.appendLog("已向小组成员 " + broadcastList + " 广播了成员更新。");
                }

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
    /**
     * [新增辅助方法] 检查列表中是否有任何成员已经属于某个小组
     * @param members 要检查的成员列表
     * @param fileIo 指向 orgs.dat 的 FileIO 实例
     * @return 如果至少有一人已在小组中，返回 true；否则返回 false
     */
    private boolean isAnyMemberAlreadyInTeam(ArrayList<String> members, FileIO fileIo) throws IOException {
        for (String member : members) {
            // 复用 getGroupsByUser 方法，检查该用户所在的小组列表是否为空
            if (!fileIo.getGroupsByUser(member).isEmpty()) {
                // 只要发现有一个成员已经有小组了，就立刻返回 true
                return true;
            }
        }
        // 遍历完所有成员，都没发现有小组的，返回 false
        return false;
    }
}
