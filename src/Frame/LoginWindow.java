package Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
        char[] password = passwordField.getPassword();

        // 这里应该验证用户名和密码
        if (username.isEmpty() || password.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "用户名和密码不能为空",
                    "登录失败",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 模拟登录成功
        JOptionPane.showMessageDialog(this,
                "登录成功！\n用户名: " + username,
                "登录成功",
                JOptionPane.INFORMATION_MESSAGE);

        // 清除密码字段
        passwordField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow window = new LoginWindow();
            window.setVisible(true);
        });
    }
}