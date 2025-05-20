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

        // 登录按钮
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton loginButton = new JButton("登录");
        loginButton.addActionListener(this::performLogin);
        mainPanel.add(loginButton, gbc);

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
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow window = new LoginWindow();
            window.setVisible(true);
        });
    }
}