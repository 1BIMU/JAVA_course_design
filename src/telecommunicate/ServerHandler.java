package telecommunicate;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import info.Chat_info;
import java.util.*;
import info.Login_info;
import io.IOStream;


public class ServerHandler extends Thread {

    Socket socket;
    ChatServer server;
    String current_user;//标记当前线程服务的用户
    public ServerHandler(Socket socket,ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        //默认重复拿
        while(true) {
            try {
                Object obj = IOStream.readMessage(socket);
                if(obj instanceof Login_info) {
                    Login_info tfi = (Login_info)obj;
                    this.current_user = tfi.getUserName();
                    boolean flag = checkUserLogin(tfi);
                    tfi.setLoginSucceessFlag(false);
                    if(flag) {
                        //返回登录成功给客户端
                        this.server.add_online_user(tfi.getUserName());
                        tfi.setLoginSucceessFlag(true);
                        IOStream.writeMessage(socket , tfi);

                    }else {
                        System.out.println("登录失败");//暂时先这么写，后续封装消息
                        //返回登录失败给客户端
                        IOStream.writeMessage(socket , tfi);
                        break;//登录失败了就关闭这个线程，节省资源
                    }
                }else if(obj instanceof Chat_info) {//收到一条消息
                    Chat_info ci = (Chat_info)obj;
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
                    IOStream.writeMessage(socket , ci);//发送相关信息给对应的客户端
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
}
