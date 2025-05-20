package Frame;
import javax.swing.text.Document;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.Serial;
import java.net.Socket;
import java.util.ArrayList;

import info.*;
import io.IOStream;
import telecommunicate.ClientHandler;

import javax.swing.text.BadLocationException;
//聊天主界面
public class ChatWindow extends JFrame {
    public static final int WIDTH = 750;
    public static final int HEIGHT = 600;
    private DefaultListModel<String> usersModel = new DefaultListModel<>();
    JList<String> Users = new JList<>(usersModel);
    @Serial
    private static final long serialVersionUID = 2612988528480049031L;
    public ChatWindow() {
        initializeUI();
        setupComponents();
    }

    private void initializeUI() {
        this.setSize(WIDTH, HEIGHT);
        this.setTitle("Chat Window");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        centerWindow();
    }
    private void centerWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2,
                (screenSize.height - getHeight()) / 2);
    }
    private void setupComponents() {
        // 接受框
        JTextPane messagePane = new JTextPane();
        messagePane.setOpaque(false);
        messagePane.setFont(new Font("宋体", Font.PLAIN, 16));
        messagePane.setEditable(false);
        //滚动条
        JScrollPane scrollPane = new JScrollPane(messagePane);
        scrollPane.setBounds(15,20,500,332);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        //背景标签
        JLabel bg = new JLabel();
        bg.setBounds(0,0,WIDTH,HEIGHT);
        bg.add(scrollPane);
        //发送框
        JTextPane sendPane = new JTextPane();
        sendPane.setOpaque(false);
        sendPane.setFont(new Font("宋体", Font.PLAIN, 16));

        //滚动条
        JScrollPane Sendscroll = new JScrollPane(sendPane);
        Sendscroll.setBounds(15,400,500,122);
        Sendscroll.setOpaque(false);
        Sendscroll.getViewport().setOpaque(false);
        bg.add(Sendscroll);

        //在线用户列表

        Users.setFont(new Font("宋体",Font.PLAIN,16));
        Users.setVisibleRowCount(17);//可以显示多少行
        Users.setFixedCellWidth(180);//每个单元格宽高
        Users.setFixedCellHeight(18);
        //滚动条
        JScrollPane Userscroll = new JScrollPane(Users);
        Userscroll.setFont(new Font("宋体",Font.PLAIN,16));
        Userscroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        Userscroll.setBounds(530,17,200,507);
        bg.add(Userscroll);
        //发送按钮
        JButton send = new JButton("发送信息");
        send.setBounds(15,533,125,25);
        getRootPane().setDefaultButton(send);
        send.addActionListener(e -> handleSendMessage(sendPane,messagePane)); // 绑定点击事件
        bg.add(send);

        this.add(bg);
    }
    public void updateOnlineUsers(ArrayList<String> onlineUsers) {
        SwingUtilities.invokeLater(() -> {
            usersModel.clear();
            onlineUsers.forEach(usersModel::addElement);
            // 如果需要保持滚动位置可以添加：
            Users.ensureIndexIsVisible(usersModel.getSize() - 1);
        });
    }
    private void handleSendMessage(JTextPane sendPane,JTextPane messagePane) {
        String message = sendPane.getText().trim();
        if (!message.isEmpty()) {
            //创建一个聊天的消息
            encap_info INFO = new encap_info();
            Chat_info chat = new Chat_info();
            chat.setMessage(message);
            //下面要获取当前群聊的一些信息，这里还未实现，先空着



            INFO.set_chat_info(chat);
            INFO.set_type(4);
            connectionServer(INFO);
            Document doc = messagePane.getDocument();
            // 自动滚动到底部
            messagePane.setCaretPosition(doc.getLength());
            // 清空发送框
            sendPane.setText("");
        }
    }

    public void connectionServer(encap_info INFO) {
        try {
            Socket socket = new Socket("127.0.0.1", 6688);//和6688端口建立连接
            IOStream.writeMessage(socket , INFO);//发送消息
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            ChatWindow window = new ChatWindow();
            window.setVisible(true);
        });
    }
}
