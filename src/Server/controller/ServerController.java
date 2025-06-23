package Server.controller;

import Server.ChatServer;
import Server.ServerHandler;
import Server.model.ServerModel;
import Server.view.ServerWindow;
import info.*;
import io.FileIO;
import io.IOStream;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

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
        boolean isAlreadyOnline = model.checkUserOnline(current_user, this.server.online_users);
        if (isAlreadyOnline) {
            ServerFrame.appendLog("用户 "+ current_user + " 已经在线，准备强制下线旧连接...");

            // 查找旧的Socket和对应的处理器
            Socket old_socket = server.userSocketMap.get(this.current_user);
            ServerHandler oldHandler = server.SocketHandlerMap.get(old_socket);

            if (old_socket != null) {
                // 准备并发送“被踢下线”的通知
                encap_info kickInfoEncap = new encap_info();
                Login_info kickInfo = new Login_info();
                kickInfo.setKicked(true); // 设置被踢下线标志
                kickInfo.setLoginSuccessFlag(false); // 登录在旧设备上已失效
                kickInfoEncap.set_type(3); // 同样使用登录消息类型
                kickInfoEncap.set_login_info(kickInfo);
                IOStream.writeMessage(old_socket, kickInfoEncap);
                ServerFrame.appendLog("已向旧连接 " + old_socket.getRemoteSocketAddress() + " 发送被踢下线通知。");

                // 清理旧的连接和映射关系
                this.server.online_sockets.remove(old_socket);
                this.server.userSocketMap.remove(this.current_user);
                this.server.SocketHandlerMap.remove(old_socket);

                // 关闭旧的Socket，这将导致对应的ServerHandler线程结束
                try {
                    old_socket.close();
                    ServerFrame.appendLog("旧连接已成功关闭。");
                } catch (IOException e) {
                    ServerFrame.appendLog("关闭旧连接时出错: " + e.getMessage());
                }
            } else {
                ServerFrame.appendLog("警告：用户 " + current_user + " 在线，但在Map中找不到旧Socket。");
            }
        }

        // 3. 更新服务器状态，将新用户/连接加入
        // 如果用户之前不在线，则加入在线列表
        if (!isAlreadyOnline) {
            server.add_online_user(current_user);
        }
        // 无论如何，都更新/添加用户的Socket映射和Handler映射
        server.userSocketMap.put(current_user, socket);
        server.online_sockets.add(socket);
        // 注意：SocketHandlerMap 在 ServerHandler 构造时已添加，这里无需重复

        ServerFrame.updateUserList(server.online_users);
        ServerFrame.appendLog("用户 " + current_user + " 的新连接已确立。");

        // 4. 向所有其他在线用户广播“上线”通知
        Login_info broadcastInfo = new Login_info();
        broadcastInfo.setUserName(current_user);
        broadcastInfo.setLoginSuccessFlag(true); // true代表上线
        broadcastInfo.setOnlineUsers(new ArrayList<>(server.online_users)); // 发送最新的在线列表
        broadcastInfo.setAllUsers(allUsers);

        encap_info broadcastEncap = new encap_info();
        broadcastEncap.set_type(3);
        broadcastEncap.set_login_info(broadcastInfo);

        // 使用一个新的model实例来发送广播，避免并发问题
        ServerModel broadcastModel = new ServerModel(server);
        // 创建一个副本，移除当前登录者自己，避免给自己发送“你已上线”的广播
        ArrayList<String> otherOnlineUsers = new ArrayList<>(server.online_users);
        otherOnlineUsers.remove(current_user);
        broadcastModel.Send2Users(broadcastEncap, otherOnlineUsers);
        ServerFrame.appendLog("已向其他在线用户广播 " + current_user + " 的上线状态。");


        // 5. 准备【只发给当前新登录者】的详细数据回执
        Login_info detailedResponseInfo = new Login_info();
        detailedResponseInfo.setUserName(current_user);
        detailedResponseInfo.setLoginSuccessFlag(true);
        detailedResponseInfo.setKicked(false); // 明确告知新设备没有被踢
        detailedResponseInfo.setOnlineUsers(new ArrayList<>(server.online_users)); // 发送最新的在线列表
        detailedResponseInfo.setAllUsers(allUsers);

        // 添加群聊信息
        ArrayList<Integer> groups = fileIO.getGroupsByUser(current_user);
        Map<Integer, ArrayList<String>> groupMap = model.groupMap(groups, fileIO);
        Map<Integer, String> groupNameMap = new HashMap<>();
        for (Integer groupId : groups) {
            groupNameMap.put(groupId, fileIO.getGroupName(groupId));
        }
        detailedResponseInfo.setGroupIDList(groups);
        detailedResponseInfo.setGroupMap(groupMap);
        detailedResponseInfo.setGroupNameMap(groupNameMap);

        // 添加小组信息 (使用已修复的逻辑)
        FileIO fileio_org = new FileIO("users.dat", "orgs.dat");
        ArrayList<Org_info> userOrgs = fileio_org.getAllOrgsByUser(current_user);
        ArrayList<Integer> orgIDs = new ArrayList<>();
        Map<Integer, ArrayList<String>> orgMap = new HashMap<>();
        Map<Integer, String> orgNameMap = new HashMap<>();
        for (Org_info org : userOrgs) {
            orgIDs.add(org.getOrg_id());
            orgMap.put(org.getOrg_id(), org.getMembers());
            orgNameMap.put(org.getOrg_id(), org.getOrg_name());
        }
        detailedResponseInfo.setOrgIDList(orgIDs);
        detailedResponseInfo.setOrgMap(orgMap);
        detailedResponseInfo.setOrgNameMap(orgNameMap);
        ServerFrame.appendLog("为用户 " + current_user + " 同步 " + userOrgs.size() + " 个小组信息。");

        // 6. 发送详细回执给当前登录者
        RETURN.set_login_info(detailedResponseInfo);
        RETURN.set_type(3);
        IOStream.writeMessage(socket, RETURN);

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
                // 如果是小组消息, 从 orgs.dat 文件读取，并使用新的 getOrgMembers 方法
                ServerFrame.appendLog("处理小组聊天消息，小组ID: " + targetId);
                FileIO teamFileIO = new FileIO("users.dat", "orgs.dat");
                members = teamFileIO.getOrgMembers(targetId);
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
            System.out.println("[ServerController.Org_handler] 收到小组创建请求:");
            System.out.println("[ServerController.Org_handler]   - 来自用户: " + oi.getFromUser());
            System.out.println("[ServerController.Org_handler]   - 父群聊ID: " + oi.getGroup_id());
            System.out.println("[ServerController.Org_handler]   - 小组名称: " + oi.getOrg_name());
            System.out.println("[ServerController.Org_handler]   - 拟定成员: " + oi.getMembers());

            // 检查1: 拟定的小组成员是否都在父群聊中?
            // IsInGroup 方法如果发现有成员不在群聊中，会返回 true。所以它代表“有问题”。
            boolean problem_memberMissing = (group_members == null || model.IsInGroup(group_members, org_members));
            System.out.println("[ServerController.Org_handler] 检查1 (是否有成员不在父群聊中?): " + problem_memberMissing);

            // 检查2: 拟定的成员是否已经在这个父群聊的其他小组里了?
            boolean problem_memberConflict = isAnyMemberInTeamForGroup(org_members, group_id, fileio_org);
            System.out.println("[ServerController.Org_handler] 检查2 (是否有成员已在此群聊的其他小组中?): " + problem_memberConflict);

            // 最终判断
            if (!fileio_group.groupExists(group_id) || problem_memberMissing || problem_memberConflict) {
                String errorReason = "";
                if (!fileio_group.groupExists(group_id)) errorReason = "父群聊不存在";
                else if (problem_memberMissing) errorReason = "有成员不属于该父群聊";
                else if (problem_memberConflict) errorReason = "有成员已在该群聊的其他小组中";

                ServerFrame.appendLog("创建小组失败: " + errorReason);
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

            // 使用新的 writeOrg 方法，将 parentGroupId 写入文件
            fileio_org.writeOrg(ID, group_id, oi.getOrg_name(), creatorList);

            //把这些人都加入到哈希表的维护中
            if (group_members != null) {
                group_members.remove(oi.getFromUser());
                model.addUserByOrgID(group_id, group_members);
            }

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

            // --- MODIFIED --- 先过滤出在线的被邀请者，再发送邀请
            model.filterOnlineMembers(invitees, server.online_users);
            ServerFrame.appendLog("筛选后在线的被邀请者: " + invitees);

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
                ServerFrame.appendLog("已向在线的被邀请者 " + invitees + " 发送小组邀请。");
            }
        } else if (oi.getType() == 4) {//如果不是建立群聊，那么是对文件中进行修改
            // 修复了修改小组成员的逻辑
            ServerFrame.appendLog("修改小组 " + oi.getOrg_id() + " 的成员");
            ArrayList<String> added_people = oi.getAdded_people();
            ArrayList<String> removed_people = oi.getRemoved_people();

            // 检查要添加的人是否在父群聊中
            boolean addedPeopleValid = (group_members != null && model.IsInGroup(group_members, added_people));
            if (!addedPeopleValid) {
                ServerFrame.appendLog("错误：尝试添加的成员不在父群聊中。");
                // 返回错误信息
                oi.setSuccess(false);
                ArrayList<String> back_user = new ArrayList<>();
                back_user.add(current_user);
                RETURN.set_org_info(oi);
                model.Send2Users(INFO, back_user);
                return;
            }

            // 使用新的 manageOrgMembers 方法
            fileio_org.manageOrgMembers(oi.getOrg_id(), added_people, removed_people);

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
                int orgId = oi.getOrg_id();
                int parentGroupId = oi.getGroup_id(); // 获取父群聊ID

                ServerFrame.appendLog("用户 " + fromUser + " 接受了小组 " + orgId + " 的邀请。");

                // 更新小组成员的逻辑
                Org_info existingOrg = fileio_org.getOrgInfo(orgId);
                if (existingOrg == null) {
                    ServerFrame.appendLog("错误：用户 " + fromUser + " 尝试加入一个不存在的小组 ID: " + orgId);
                    return;
                }
                String orgName = existingOrg.getOrg_name();
                ArrayList<String> currentMembers = existingOrg.getMembers();

                if (!currentMembers.contains(fromUser)) {
                    currentMembers.add(fromUser);
                }

                // 使用新的 writeOrg 方法将包含父群聊ID的完整信息写回
                fileio_org.writeOrg(orgId, parentGroupId, orgName, currentMembers);
                ServerFrame.appendLog("已将 " + fromUser + " 添加到小组 " + orgId + "。当前成员: " + currentMembers);

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

    public void VoiceCall_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Voice_info voiceInfo = INFO.get_voice_info();
        if (voiceInfo == null) return;

        String fromUsername = voiceInfo.getFrom_username();
        ServerFrame.appendLog("接收到语音通话请求: 来自用户 " + fromUsername);

        // 获取当前客户端的真实IP地址并设置到Voice_info中
        String clientRealIP = socket.getInetAddress().getHostAddress();
        voiceInfo.setServerDetectedHost(clientRealIP);
        ServerFrame.appendLog("检测到用户 " + fromUsername + " 的真实IP地址: " + clientRealIP);

        // 转发语音通话消息给目标用户
        List<String> participants = voiceInfo.getParticipants();
        if (participants != null && !participants.isEmpty()) {
            for (String participant : participants) {
                // 获取目标用户的Socket
                Socket targetSocket = server.getSocketByUsername(participant);
                if (targetSocket != null && !targetSocket.isClosed()) {
                    // 转发语音通话消息(包含发送方的真实IP)
                    IOStream.writeMessage(targetSocket, INFO);
                    ServerFrame.appendLog("转发语音通话消息到用户: " + participant + ", 包含发送方真实IP: " + clientRealIP);
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
     * [新增辅助方法] 检查指定成员是否已在特定群聊的某个小组中
     * @param members 要检查的成员列表
     * @param parentGroupId 父群聊的ID
     * @param orgFileIo 指向 orgs.dat 的 FileIO 实例
     * @return 如果有任何一个成员已在该群聊的小组中，则返回 true
     */
    private boolean isAnyMemberInTeamForGroup(ArrayList<String> members, int parentGroupId, FileIO orgFileIo) throws IOException {
        if (members == null) return false;

        for (String member : members) {
            // 获取该用户所属的所有小组
            ArrayList<Org_info> userTeams = orgFileIo.getAllOrgsByUser(member);

            // 检查这些小组中是否有任何一个属于当前的父群聊
            for (Org_info team : userTeams) {
                if (team.getGroup_id() == parentGroupId) {
                    // 冲突：该成员已在此父群聊的某个小组中
                    ServerFrame.appendLog("创建小组失败：成员 " + member + " 已在群聊 " + parentGroupId + " 的另一个小组中。");
                    return true;
                }
            }
        }

        // 所有成员都没有冲突
        return false;
    }
}