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
                } else if(INFO.get_type()==4) {//处理聊天消息
                    handleChatMessage(INFO.get_chat_info());
                }
                System.out.println("客户端" +obj);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理接收到的聊天消息
     * @param chatInfo 聊天信息对象
     */
    private void handleChatMessage(Chat_info chatInfo) {
        if (chatFrame != null) {
            // 格式化消息显示
            String formattedMessage = formatChatMessage(chatInfo);
            // 调用聊天窗口的方法显示消息
            chatFrame.displayMessage(formattedMessage);
        }
    }

    /**
     * 格式化聊天消息，添加发送者和时间信息
     * @param chatInfo 聊天信息对象
     * @return 格式化后的消息字符串
     */
    private String formatChatMessage(Chat_info chatInfo) {
        // 获取当前时间
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        
        // 根据消息类型构建不同的格式
        if (chatInfo.isType()) { // 群聊消息
            return String.format("[%s] %s (群聊): %s", 
                    timeStamp, 
                    chatInfo.getFrom_username(), 
                    chatInfo.getMessage());
        } else { // 私聊消息
            return String.format("[%s] %s (私聊): %s", 
                    timeStamp, 
                    chatInfo.getFrom_username(), 
                    chatInfo.getMessage());
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
