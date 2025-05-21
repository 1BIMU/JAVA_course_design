package Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.Socket;
import io.IOStream;
import info.*;
import telecommunicate.ClientHandler;
import java.io.IOException;
public class LoginWindow extends JFrame {

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);

    public LoginWindow() {
        initializeUI();
        setupComponents();
    }

    private void initializeUI() {
        setTitle("用户登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 220);
        setResizable(false);
        centerWindow();
    }

    private void centerWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2,
                (screenSize.height - getHeight()) / 2);
    }

    private void setupComponents() {
        // 使用GridBagLayout布局管理器
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 用户名组件
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);

        // 密码组件
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;  // 修改为1列宽度
        JButton loginButton = new JButton("登录");
        loginButton.addActionListener(this::performLogin);
        mainPanel.add(loginButton, gbc);

        // 新增注册按钮
        gbc.gridx = 1;
        gbc.gridy = 2;
        JButton registerButton = new JButton("注册");
        registerButton.addActionListener(this::performRegistration); // 绑定注册方法
        mainPanel.add(registerButton, gbc);

        add(mainPanel);
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        //进行消息封装
        Login_info login = new Login_info();
        login.setPassword(password);
        login.setUserName(username);
        connectionServer(login);//发送沟通消息，尝试连接
    }
    public void connectionServer(Login_info lg) {
        try {
            encap_info INFO = new encap_info();
            INFO.set_login_info(lg);
            INFO.set_type(3);
            Socket socket = new Socket("127.0.0.1", 6688);//和6688端口建立连接

            IOStream.writeMessage(socket , INFO);//写入流

            ClientHandler clientHandler = new ClientHandler(socket , this);
            clientHandler.start();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    private void performRegistration(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // 输入验证
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 封装注册信息
        Reg_info REG = new Reg_info();
        REG.set_info(username,password,0);
        // 连接服务器
        connectionRegistrationServer(REG);
    }

    // 新增的注册连接方法
    private void connectionRegistrationServer(Reg_info reg) {
        try {
            encap_info INFO = new encap_info();
            INFO.set_reg_info(reg);  // 假设有设置注册信息的方法
            INFO.set_type(5);                 // 假设5表示注册类型

            Socket socket = new Socket("127.0.0.1", 6688);
            IOStream.writeMessage(socket, INFO);

            // 启动客户端处理器
            ClientHandler clientHandler = new ClientHandler(socket, this);
            clientHandler.start();

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "连接服务器失败", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow window = new LoginWindow();
            window.setVisible(true);
        });
    }
}