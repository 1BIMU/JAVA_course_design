package telecommunicate;

import java.net.Socket;

import Frame.ChatWindow;
import Frame.LoginWindow;
import javax.swing.JOptionPane;

import info.Login_info;
import io.IOStream;

public class ClientHandler extends Thread{

    Socket socket;
    LoginWindow loginFrame;

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
                if(obj instanceof Login_info) {//判断类型为登录消息
                    Login_info tfi = (Login_info)obj;
                    loginResult(tfi);

                }
                System.out.println("客户端" +obj);
                if("登录成功".equals(obj)) {
                    System.out.println("客户端收到登录成功的消息");
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void loginResult(Login_info tfi) {
        if(tfi.getLoginSucceessFlag()) {
            //登录成功，打开主界面
            ChatWindow chatFrame = new ChatWindow();
            chatFrame.setVisible(true);
            loginFrame.dispose();//关闭窗体
        }else {
            //登录失败
            System.out.println("客户端接收到登录失败");//干点啥事，不着急，后续再写
        }
    }
}
