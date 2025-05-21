package telecommunicate;
import io.FileIO;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

import Frame.ServerWindow;
import info.Chat_info;
import java.util.*;

import info.Group_info;
import info.Login_info;
import io.IOStream;
import info.encap_info;

import javax.swing.*;

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

                    }else {
                        System.out.println("登录失败");//暂时先这么写，后续封装消息
                        //返回登录失败给客户端
                        RETURN.set_login_info(tfi);
                        RETURN.set_type(3);
                        IOStream.writeMessage(socket , RETURN);
                        break;//登录失败了就关闭这个线程，节省资源
                    }
                }else if(INFO.get_type()==4) {//收到一条消息
                    Chat_info ci = INFO.get_chat_info();
                    boolean flag = false;
                    for(int i = 0;i<ci.getTo_username().length;i++) {//先检查，自己是不是别人发的对象
                        if(current_user.equals(ci.getTo_username()[i])) {
                            flag = true;
                        }
                    }
                    if(!flag) continue;//直接丢掉这个包，它不属于我QAQ
                    String []valid_user = vaild_user_check(ci);//得到有效的用户长度
                    if(valid_user.length==0){//如果里面啥都没有
                        ci.setTransfer_status(false);
                    }else{
                        ci.setTransfer_status(true);
                    }
                    ci.setTo_username(valid_user);
                    RETURN.set_chat_info(ci);
                    RETURN.set_type(4);
                    IOStream.writeMessage(socket , RETURN);//发送相关信息给对应的客户端
                }else if(INFO.get_type()==1) {//如果收到的消息为群聊控制消息
                    Group_info gi = INFO.get_group_info();
                    if(gi.isEstablish()==true){//如果是建立群聊的消息
                        FileIO fileio = new FileIO();
                        ArrayList<String> to_user = gi.get_added_people();
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
                        fileio.writeGroup(ID,to_user);
                        //添加回复消息，给所有人回复对应的添加消息，邀请他们进入群聊
                        gi.set_Group_id(ID);
                        Send2Users(INFO,to_user);

                    }else{//如果不是建立群聊，那么是对文件中进行修改

                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public String[] vaild_user_check(Chat_info ci){//这里需要统计在线人数，然后对在线人数列表进行遍历
        //解包
        try{

            String from_user = ci.getFrom_username();
            String[] to_user = ci.getTo_username();
            List<String> avaliable_user = new ArrayList<>();
            String message = ci.getMessage();
            for(int i=0;i<to_user.length;i++) {//循环遍历到达用户的数组
                if(!isonline(to_user[i])){//检查是否每个用户都在线

                }else{
                    avaliable_user.add(to_user[i]);
                }
            }
            return avaliable_user.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isonline(String user) throws IOException {
        for(int i = 0;i<this.server.online_users.size();i++) {
            if(user.equals(this.server.online_users.get(i))){
                return true;
            }
        }
        return false;
    }

    public boolean checkUserLogin(Login_info tfi) {
        try {
            String userName = tfi.getUserName();
            String password = tfi.getPassword();
            FileInputStream fis = new FileInputStream(new File("E:\\java_course_design\\chat\\src\\user.txt"));// 储存用户信息的文件（不用SQL了）
            DataInputStream dis = new DataInputStream(fis);
            String row = null;
            while((row = dis.readLine()) != null) {
                //从文件中读取的行
                if((userName+"|"+password).equals(row)) {
                    System.out.println("服务端：用户名，密码正确");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendALL(encap_info INFO){
        for (int i = 0; i < server.online_sockets.size(); i++) {
            Socket tempSocket = server.online_sockets.get(i);
            IOStream.writeMessage(tempSocket , INFO);
        }
    }

    public void Send2Users(encap_info INFO,ArrayList<String> to_user){
        for(int i = 0;i<to_user.size();i++) {
            //先从hashmap中拿到对应用户的socket
            Socket tempSocket = server.userSocketMap.get(to_user.get(i));
            IOStream.writeMessage(tempSocket , INFO);
        }
    }
}

