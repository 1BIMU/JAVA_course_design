package Frame;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
//聊天主界面
public class ChatWindow extends JFrame {
    public static final int WIDTH = 750;
    public static final int HEIGHT = 600;
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
        JList Users = new JList();
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
        bg.add(send);
        this.add(bg);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            ChatWindow window = new ChatWindow();
            window.setVisible(true);
        });
    }
}
