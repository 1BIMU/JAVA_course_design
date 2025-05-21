package telecommunicate;

import java.net.Socket;
import info.*;
import Frame.ChatWindow;
import Frame.LoginWindow;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import info.Login_info;
import io.IOStream;

public class ClientHandler extends Thread{
    ChatWindow chatFrame;
    Socket socket;
    LoginWindow loginFrame;
    private String current_user;
    ArrayList<String> online_users = new ArrayList<String>();//在客户端也需要同步当前在线的用户数
    public ClientHandler(Socket socket , LoginWindow loginFrame) {
        this.socket = socket;
        this.loginFrame = loginFrame;
    }

    @Override
    public void run() {
        //默认重复拿
        while(true) {
            try {
                //模拟一直拿消息，产生阻塞
                Object obj = IOStream.readMessage(socket);
                encap_info INFO = (encap_info)obj;
                if(INFO.get_type()==3) {//判断类型为登录消息
                    Login_info lg = INFO.get_login_info();//解析包
                    online_users = lg.getOnlineUsers();
                    current_user = lg.getUserName();
                    loginResult(lg);
                    // 只有在登录成功且chatFrame不为null时才更新用户列表
                    if (lg.getLoginSucceessFlag() && chatFrame != null) {
                        chatFrame.updateOnlineUsers(online_users);
                    }
                }
                System.out.println("客户端" +obj);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void loginResult(Login_info lg) {
        if (lg.getLoginSucceessFlag()) {
            // 登录成功
            loginFrame.setVisible(false);
            chatFrame = new ChatWindow();
            chatFrame.setVisible(true);
            chatFrame.set_current_user(current_user);//当前用户
        } else {
            // 登录失败
            JOptionPane.showMessageDialog(loginFrame, "登录失败，用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }

}
