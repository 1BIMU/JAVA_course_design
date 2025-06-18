package Server.controller;
import Server.view.ServerWindow;
import Server.ChatServer;
import info.*;
import io.FileIO;
import io.IOStream;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

import Server.model.ServerModel;
public class ServerController {
    Socket socket;
    ChatServer server;
    ServerWindow ServerFrame;
    //实例化model
    ServerModel model;
    String current_user;//标记当前线程服务的用户
    public ServerController(Socket socket, ChatServer server,ServerWindow ServerFrame) {//定义构造方法
        this.socket = socket;
        this.server = server;
        this.ServerFrame = ServerFrame;
        model = new ServerModel(server);
    }
    public boolean Login_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Login_info tfi = INFO.get_login_info();
        this.current_user = tfi.getUserName();
        ServerFrame.appendLog("尝试登录用户: " + tfi.getUserName());
        FileIO FI = new FileIO();
        boolean flag = model.checkUserLogin(tfi);
        tfi.setLoginSucceessFlag(false);
        if(flag) {
            //返回登录成功给客户端
            ServerFrame.appendLog("用户 " + tfi.getUserName() + " 登录成功");
            this.server.add_online_user(tfi.getUserName());
            this.server.add_online_socket(socket);
            server.userSocketMap.put(tfi.getUserName(), socket);
            ServerFrame.appendLog("更新在线用户列表: " + server.online_users);
            tfi.setOnlineUsers(server.online_users);//同步在线用户列表消息
            //更新前端页面
            ServerFrame.updateUserList(server.online_users);
            tfi.setLoginSucceessFlag(true);
            RETURN.set_login_info(tfi);
            RETURN.set_type(3);
            ServerFrame.appendLog("通知所有用户 " + tfi.getUserName() + " 已上线");
            model.sendALL(INFO);//通知所有人，该用户已上线
            ArrayList<Integer> groups = FI.getGroupsByUser(tfi.getUserName());//获得所有和该用户相关的groupID的表
            Map<Integer,ArrayList<String>> groupMap = model.groupMap(groups);
            tfi.setGroupMap(groupMap);
            tfi.setGroupIDList(groups);
            RETURN.set_login_info(tfi);
            RETURN.set_type(3);
            IOStream.writeMessage(socket, RETURN);//单独给这个用户回这个稍微长一点的消息
            return true;
        }else {
            ServerFrame.appendLog("用户 " + tfi.getUserName() + " 登录失败");
            //返回登录失败给客户端
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
            System.out.println(to_user);//为什么这里收到的to_user是Null??
            ServerFrame.appendLog("创建新群组 ID: " + ID + "，成员: " + to_user);
            fileio.writeGroup(ID,to_user);
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
        this.server.userSocketMap.remove(current_user);
        this.server.online_users.remove(current_user);
        ServerFrame.updateUserList(server.online_users);//前端页面移除
        this.server.online_sockets.remove(socket);
        //都登出了，就不会接受返回消息了，就不返回了，主要是懒得再写个返回消息了，麻烦
        ServerFrame.appendLog("用户 " + current_user + " 已成功注销");
        ServerFrame.appendLog("当前在线用户: " + server.online_users);
    }


}
