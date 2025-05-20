package telecommunicate;

import java.net.Socket;
import info.*;
import Frame.ChatWindow;
import Frame.LoginWindow;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import info.Login_info;
import io.IOStream;

public class ClientHandler extends Thread{
    ChatWindow chatFrame;
    Socket socket;
    LoginWindow loginFrame;
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
                    Login_info lg = INFO.get_login_info();
                    loginResult(lg);
                }
                System.out.println("客户端" +obj);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void loginResult(Login_info tfi) {
        if(tfi.getLoginSucceessFlag()) {
            //登录成功，打开主界面
            chatFrame = new ChatWindow();
            chatFrame.setVisible(true);
            loginFrame.dispose();//关闭窗体
        }else {
            //登录失败
            System.out.println("客户端接收到登录失败");//干点啥事，不着急，后续再写
        }
    }
}
