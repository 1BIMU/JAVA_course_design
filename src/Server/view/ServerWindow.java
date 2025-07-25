package Server.view;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;

//服务器端主界面
public class ServerWindow extends JFrame {
    public static final int WIDTH = 550;
    public static final int HEIGHT = 500;
    // 添加 volatile 确保线程可见性
    private volatile JTextField txtNumber;
    private volatile JTextField txtServerName;
    private volatile JTextField txtIP;
    private volatile JTextField txtPort;
    private volatile JTextPane txtLog;
    private volatile JList<String> userList;
    @Serial
    private static final long serialVersionUID = 8659545959675588211L;
    public JLabel UserPanel;
    public JPanel ServerInfo;
    public ServerWindow() {
        initializeUI();
        setupComponents();
    }

    private void initializeUI() {
        this.setSize(WIDTH, HEIGHT);
        this.setTitle("Server Window");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        centerWindow();
    }
    public JPanel getServerInfoPanel(){
        //整个第一个选项服务板，包括日志区域 一整个大的面板
        JPanel pnlServer = new JPanel();
        pnlServer.setOpaque(false);
        pnlServer.setLayout(null);
        pnlServer.setBounds(0, 0, WIDTH, HEIGHT);

        //日志区域
        JLabel lblLog = new JLabel("[服务器日志]");
        lblLog.setForeground(Color.BLACK);
        lblLog.setFont(new Font("宋体",Font.PLAIN, 16));
        lblLog.setBounds(130, 5, 100, 30);
        pnlServer.add(lblLog);

        //日志区域
        txtLog = new JTextPane();
        txtLog.setOpaque(false);
        txtLog.setFont(new Font("宋体", Font.PLAIN, 12));

        JScrollPane scoPaneOne = new JScrollPane(txtLog);// 设置滚动条
        scoPaneOne.setBounds(130, 35, 340, 360);
        scoPaneOne.setOpaque(false);
        scoPaneOne.getViewport().setOpaque(false);
        pnlServer.add(scoPaneOne);
        pnlServer.add(getServerParam());
        return pnlServer;

    }
    private void centerWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2,
                (screenSize.height - getHeight()) / 2);
    }
    public JLabel getUserPanel() {
        // 用户面板
        JPanel pnlUser = new JPanel();
        pnlUser.setLayout(null);
        pnlUser.setBackground(new Color(52, 130, 203));
        pnlUser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        pnlUser.setBounds(50, 5, 300, 400);
        pnlUser.setOpaque(false);//设置透明

        JLabel lblUser = new JLabel("[在线用户列表]");
        lblUser.setFont(new Font("宋体", 0, 16));
        lblUser.setBounds(50, 10, 200, 30);
        pnlUser.add(lblUser);

        //用户列表
        userList = new JList<>();
        userList.setFont(new Font("宋体", 0, 14));
        userList.setVisibleRowCount(17);
        userList.setFixedCellWidth(180);
        userList.setFixedCellHeight(18);
        userList.setOpaque(false);

        JScrollPane spUser = new JScrollPane(userList);
        spUser.setFont(new Font("宋体", 0, 14));
        spUser.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        spUser.setBounds(50, 35, 200, 360);
        spUser.setOpaque(false);
        pnlUser.add(spUser);


        //创建一个标签并将图片添加进去
        JLabel lblBackground = new JLabel();
        //设置图片的位置和大小
        lblBackground.setBounds(0, 200, 300, 300);
        //添加到当前窗体中
        lblBackground.add(pnlUser);

        return lblBackground;
    }
    public JPanel getServerParam(){
        JPanel serverParamPanel = new JPanel();
        serverParamPanel.setOpaque(false);
        serverParamPanel.setBounds(5, 5, 100, 400);
        serverParamPanel.setFont(new Font("宋体", 0, 14));
        serverParamPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        JLabel lblNumber = new JLabel("当前在线人数:");
        lblNumber.setFont(new Font("宋体", 0, 14));
        serverParamPanel.add(lblNumber);

        txtNumber = new JTextField("0 人", 10);
        txtNumber.setFont(new Font("宋体", 0, 14));
        txtNumber.setEditable(false);
        serverParamPanel.add(txtNumber);

        JLabel lblServerName = new JLabel("服务器名称:");
        lblServerName.setFont(new Font("宋体", 0, 14));
        serverParamPanel.add(lblServerName);

        txtServerName = new JTextField(10);
        txtServerName.setFont(new Font("宋体", 0, 14));
        txtServerName.setEditable(false);
        serverParamPanel.add(txtServerName);

        JLabel lblIP = new JLabel("服务器IP:");
        lblIP.setFont(new Font("宋体", 0, 14));
        serverParamPanel.add(lblIP);

        txtIP = new JTextField(10);
        txtIP.setFont(new Font("宋体", 0, 14));
        txtIP.setEditable(false);
        serverParamPanel.add(txtIP);

        JLabel lblPort = new JLabel("服务器端口:");
        lblPort.setFont(new Font("宋体", 0, 14));
        serverParamPanel.add(lblPort);

        txtPort = new JTextField("展示当前端口" , 10);//拼接一个字符串
        txtPort.setFont(new Font("宋体", 0, 14));
        txtPort.setEditable(false);
        serverParamPanel.add(txtPort);
        return serverParamPanel;
    }
    private void setupComponents() {
        //选项卡
        JTabbedPane Server = new JTabbedPane(JTabbedPane.TOP);
        Server.setBackground(Color.WHITE);
        Server.setFont(new Font("宋体", Font.PLAIN, 16));

        UserPanel = getUserPanel();
        ServerInfo = getServerInfoPanel();
        Server.add("服务器信息",ServerInfo);
        Server.add("在线用户",UserPanel);

        this.add(Server);
    }

    public void updateUserList(ArrayList<String> users) {
        SwingUtilities.invokeLater(() -> {
            String[] userArray = users.toArray(new String[0]);
            userList.setListData(userArray);
            txtNumber.setText(users.size() + " 人");
        });
    }

    /**
     * 添加日志消息
     * @param message 日志内容（自动添加换行）
     */
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 获取当前时间并格式化为 [yyyy-MM-dd HH:mm:ss]
                String timestamp = new java.text.SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ")
                        .format(new java.util.Date());

                StyledDocument doc = txtLog.getStyledDocument();

                // 插入带时间戳的消息
                doc.insertString(doc.getLength(), timestamp + message + "\n", null);

                // 自动滚动到底部（取消注释以启用）
                txtLog.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 设置服务器基础信息
     * @param name 服务器名称
     * @param ip 服务器IP地址
     * @param port 服务器端口号
     */
    public void setServerInfo(String name, String ip, int port) {
        SwingUtilities.invokeLater(() -> {
            txtServerName.setText(name);
            txtIP.setText(ip);
            txtPort.setText(String.valueOf(port));  // 修改这里直接设置端口号
        });
    }
    /**
     * 清空日志区域
     */
    public void clearLog() {
        SwingUtilities.invokeLater(() -> {
            txtLog.setText("");
        });
    }

    /**
     * 清空用户列表
     */
    public void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            userList.setListData(new String[0]);
            txtNumber.setText("0 人");
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            ServerWindow window = new ServerWindow();
            window.setVisible(true);
        });
    }
}
