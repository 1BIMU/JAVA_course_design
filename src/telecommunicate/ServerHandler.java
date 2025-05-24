package telecommunicate;
import info.*;
import io.FileIO;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;

import Frame.ServerWindow;

import java.util.*;

import io.IOStream;

public class ServerHandler extends Thread {

    Socket socket;
    ChatServer server;
    String current_user;//标记当前线程服务的用户
    ServerWindow ServerFrame;
    public ServerHandler(Socket socket, ChatServer server, ServerWindow serverframe) {
        this.socket = socket;
        this.server = server;
        this.ServerFrame = serverframe;
    }

    @Override
    public void run() {
        //默认重复拿
        while(true) {
            try {
                Object obj = IOStream.readMessage(socket);
                encap_info INFO = (encap_info)obj;
                encap_info RETURN = new encap_info();
                if(INFO.get_type()==3) {//处理login消息
                    if(!Login_handler(INFO,RETURN)){//如果登录失败
                        break;
                    }
                }else if(INFO.get_type()==4) {//收到一条消息
                    Chat_handler(INFO,RETURN);
                }else if(INFO.get_type()==1) {//如果收到的消息为群聊控制消息
                    Group_handler(INFO,RETURN);
                }else if(INFO.get_type()==5) {//收到了注册消息
                    REG_handler(INFO,RETURN);
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private boolean Login_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Login_info tfi = INFO.get_login_info();
        this.current_user = tfi.getUserName();
        boolean flag = checkUserLogin(tfi);
        tfi.setLoginSucceessFlag(false);
        if(flag) {
            //返回登录成功给客户端
            this.server.add_online_user(tfi.getUserName());
            this.server.add_online_socket(socket);
            server.userSocketMap.put(tfi.getUserName(), socket);
            tfi.setOnlineUsers(server.online_users);//同步在线用户列表消息
            tfi.setLoginSucceessFlag(true);
            RETURN.set_login_info(tfi);
            RETURN.set_type(3);
            sendALL(INFO);//通知所有人，该用户已上线
            return true;
        }else {
            System.out.println("登录失败");//暂时先这么写，后续封装消息
            //返回登录失败给客户端
            RETURN.set_login_info(tfi);
            RETURN.set_type(3);
            IOStream.writeMessage(socket , RETURN);
            return false;
        }
    }

    private void Chat_handler(encap_info INFO, encap_info RETURN) throws IOException {
        Chat_info ci = INFO.get_chat_info();
        ci.setTransfer_status(true);
        FileIO FI = new FileIO();
        if(ci.isType()){//如果是群聊消息
            ArrayList<String> group_members = FI.getGroupMembers(ci.getGroup_id());//通过id获取需要转发的成员列表
            filterOnlineMembers(group_members,this.server.online_users);//过滤一下非在线的人
            //已经确定要转发给谁了，那么就封装一下消息吧
            RETURN.set_chat_info(ci);
            RETURN.set_type(4);
            Send2Users(RETURN, group_members);
        }else{//如果是私聊消息
            String to_user = ci.getTo_username();
            if(this.server.online_users.contains(to_user)) {//如果在线的话
                RETURN.set_chat_info(ci);
                RETURN.set_type(4);
                Socket socket = server.userSocketMap.get(to_user);
                IOStream.writeMessage(socket , RETURN);//直接发消息了
                IOStream.writeMessage(this.socket , RETURN);//给自己也得回一个
            }else{//如果不在线，给自己回一个发送失败
                ci.setTransfer_status(false);
                RETURN.set_chat_info(ci);
                RETURN.set_type(4);
                IOStream.writeMessage(this.socket , RETURN);//给自己
            }
        }
    }
    private void REG_handler(encap_info INFO,encap_info RETURN) throws IOException {
        Reg_info reg = INFO.get_reg_info();
        String username = reg.getUsername();
        FileIO fileio = new FileIO();
        boolean flag = fileio.userExists(username);
        if(flag){//已经存在已有用户
            reg.setReg_status(2);//注册失败
        }else{
            reg.setReg_status(1);//注册成功
            fileio.writeUser(username,reg.getPassword());
        }
        RETURN.set_reg_info(reg);
        RETURN.set_type(5);
        IOStream.writeMessage(socket , RETURN);
    }
    private void Group_handler(encap_info INFO,encap_info RETURN) throws IOException {
        Group_info gi = INFO.get_group_info();
        if(gi.isEstablish()){//如果是建立群聊的消息
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

            fileio.writeGroup(ID,to_user);
            //添加回复消息，给所有人回复对应的添加消息，邀请他们进入群聊
            gi.set_Group_id(ID);
            Send2Users(INFO,to_user);
        }else{//如果不是建立群聊，那么是对文件中进行修改
            FileIO fileio = new FileIO();
            ArrayList<String> added_people = gi.get_added_people();
            ArrayList<String> removed_people = gi.get_removed_people();
            fileio.manageGroupMembers(gi.get_Group_id(),added_people,removed_people);
            //然后发消息，通知added_people被添加
            Group_info added = gi;
            added.setExist(true);
            RETURN.set_group_info(added);
            Send2Users(INFO,added_people);
            //发消息，告诉removed_people被删除
            Group_info removed = gi;
            removed.setExist(false);
            RETURN.set_group_info(removed);
            Send2Users(INFO,removed_people);
        }
    }
    public boolean checkUserLogin(Login_info tfi) throws IOException {
            String userName = tfi.getUserName();
            String password = tfi.getPassword();
            FileIO fileio = new FileIO();
            return fileio.validateUser(userName,password);
    }

    public void sendALL(encap_info INFO){
        for (int i = 0; i < server.online_sockets.size(); i++) {
            Socket tempSocket = server.online_sockets.get(i);
            IOStream.writeMessage(tempSocket , INFO);
        }
    }
    public void filterOnlineMembers(ArrayList<String> group_members, ArrayList<String> online_users) {
        // 将在线用户列表转为 HashSet 提高查找效率
        Set<String> onlineSet = new HashSet<>(online_users);

        // 使用 removeIf 方法过滤不在线成员（Java 8+ 特性）
        group_members.removeIf(member -> !onlineSet.contains(member));
    }
    public void Send2Users(encap_info INFO,ArrayList<String> to_user){
        for(int i = 0;i<to_user.size();i++) {
            //先从hashmap中拿到对应用户的socket
            Socket tempSocket = server.userSocketMap.get(to_user.get(i));
            IOStream.writeMessage(tempSocket , INFO);
        }
    }
}

