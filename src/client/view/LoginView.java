package client.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import client.controller.LoginController;

/*
    一些说明：
        这里没什么注意到，主要是美化了一下 UI：
            * 使用 CardLayout 实现登录和注册面板的切换；
            * 采用 GridBagLayout 和 BorderLayout 布局管理器，创建表单界面；
            * 以及其他一些组件；
        
        处理用户交互上：
            * 登录按钮：调用控制器的 login 方法处理登录请求
            * 注册按钮：调用控制器的 register 方法处理注册请求
            * 切换按钮：在登录和注册面板之间切换
        
        状态反馈上：
            * showError：显示错误消息，使用红色字体
            * showMessage：显示普通消息，使用绿色字体
            * showLoginInProgress/showRegisterInProgress：显示操作进行中的状态
        
        表单控制：通过 resetLoginForm/resetRegisterForm 重置表单状态；

*/

/*
    一些边写边学的记录：
        * 使用 SwingUtilities.invokeLater 确保所有UI更新在EDT(Event Dispatch Thread)线程中执行，避免多线程安全问题；
*/

/*
    登录UI，提供登录和注册界面
*/
public class LoginView extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // 控制器引用
    private LoginController controller;
    
    // 界面组件
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    // 登录面板组件
    private JPanel loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton switchToRegisterButton;
    private JLabel statusLabel;
    
    // 注册面板组件
    private JPanel registerPanel;
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JButton registerButton;
    private JButton switchToLoginButton;
    private JLabel regStatusLabel;
    
    /*
        构造函数
    */
    public LoginView(LoginController controller) {
        this.controller = controller;
        
        // 设置窗口属性
        setTitle("多人聊天室 - 登录");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        try {
            // 设置界面风格
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 初始化界面
        initComponents();
        
        // 设置控制器的视图引用
        controller.setLoginView(this);
        
        // 调整窗口大小以适应内容
        pack();
        setLocationRelativeTo(null); // 重新居中显示
    }
    
    /*
        初始化界面组件
    */
    private void initComponents() {
        // 创建卡片布局面板
        cardPanel = new JPanel();
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);
        
        // 初始化登录面板
        initLoginPanel();
        
        // 初始化注册面板
        initRegisterPanel();
        
        // 将面板添加到卡片布局
        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");
        
        // 默认显示登录面板
        cardLayout.show(cardPanel, "login");
        
        // 将卡片面板添加到窗口
        add(cardPanel);
    }
    
    /*
        初始化登录面板
    */
    private void initLoginPanel() {
        loginPanel = new JPanel(new BorderLayout(10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("用户登录", SwingConstants.CENTER);
        titleLabel.setFont(new Font("宋体", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel userLabel = new JLabel("用户名:");
        formPanel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);
        
        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = new JLabel("密码:");
        formPanel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        loginButton = new JButton("登录");
        loginButton.setPreferredSize(new Dimension(100, 30));
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 调用控制器处理登录
                controller.login(usernameField.getText(), new String(passwordField.getPassword()));
            }
        });
        buttonPanel.add(loginButton);
        
        switchToRegisterButton = new JButton("注册账号");
        switchToRegisterButton.setPreferredSize(new Dimension(100, 30));
        switchToRegisterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 切换到注册面板
                controller.switchToRegister();
            }
        });
        buttonPanel.add(switchToRegisterButton);
        
        // 状态标签
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        
        // 组装面板
        loginPanel.add(titlePanel, BorderLayout.NORTH);
        loginPanel.add(formPanel, BorderLayout.CENTER);
        
        // 按钮面板 + 状态面板
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        
        loginPanel.add(southPanel, BorderLayout.SOUTH);
    }
    
    /*
        初始化注册面板
    */
    private void initRegisterPanel() {
        registerPanel = new JPanel(new BorderLayout(10, 10));
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("用户注册", SwingConstants.CENTER);
        titleLabel.setFont(new Font("宋体", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel userLabel = new JLabel("用户名:");
        formPanel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        regUsernameField = new JTextField(20);
        formPanel.add(regUsernameField, gbc);
        
        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel passLabel = new JLabel("密码:");
        formPanel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        regPasswordField = new JPasswordField(20);
        formPanel.add(regPasswordField, gbc);
        
        // 确认密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel confirmLabel = new JLabel("确认密码:");
        formPanel.add(confirmLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        regConfirmPasswordField = new JPasswordField(20);
        formPanel.add(regConfirmPasswordField, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        registerButton = new JButton("注册");
        registerButton.setPreferredSize(new Dimension(100, 30));
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 调用控制器处理注册
                controller.register(
                    regUsernameField.getText(),
                    new String(regPasswordField.getPassword()),
                    new String(regConfirmPasswordField.getPassword())
                );
            }
        });
        buttonPanel.add(registerButton);
        
        switchToLoginButton = new JButton("返回登录");
        switchToLoginButton.setPreferredSize(new Dimension(100, 30));
        switchToLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 切换到登录面板
                controller.switchToLogin();
            }
        });
        buttonPanel.add(switchToLoginButton);
        
        // 状态标签
        regStatusLabel = new JLabel("", SwingConstants.CENTER);
        regStatusLabel.setForeground(Color.RED);
        
        // 组装面板
        registerPanel.add(titlePanel, BorderLayout.NORTH);
        registerPanel.add(formPanel, BorderLayout.CENTER);
        
        // 按钮面板 + 状态标签
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(regStatusLabel, BorderLayout.SOUTH);

        registerPanel.add(southPanel, BorderLayout.SOUTH);
    }
    
    /*
        切换到登录面板
    */
    public void switchToLoginPanel() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(cardPanel, "login");
            setTitle("多人聊天室 - 登录");
        });
    }
    
    /*
        切换到注册面板
    */
    public void switchToRegisterPanel() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(cardPanel, "register");
            setTitle("多人聊天室 - 注册");
        });
    }
    
    /*
        显示错误消息
    */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            boolean isLoginPanelVisible = isLoginPanelShowing();
            if (isLoginPanelVisible) {
                statusLabel.setText(message);
                statusLabel.setForeground(Color.RED);
            } else {
                regStatusLabel.setText(message);
                regStatusLabel.setForeground(Color.RED);
            }
        });
    }
    
    /*
        显示普通消息
    */
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            boolean isLoginPanelVisible = isLoginPanelShowing();
            if (isLoginPanelVisible) {
                statusLabel.setText(message);
                statusLabel.setForeground(new Color(0, 128, 0)); // 绿色
            } else {
                regStatusLabel.setText(message);
                regStatusLabel.setForeground(new Color(0, 128, 0)); // 绿色
            }
        });
    }
    
    /*
        判断当前是否显示登录面板
    */
    private boolean isLoginPanelShowing() {
        return loginPanel.isVisible();
    }
    
    /*
        显示登录进行中状态
    */
    public void showLoginInProgress() {
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(false);
            switchToRegisterButton.setEnabled(false);
            statusLabel.setText("登录中...");
            statusLabel.setForeground(Color.BLUE);
        });
    }
    
    /*
        显示注册进行中状态
    */
    public void showRegisterInProgress() {
        SwingUtilities.invokeLater(() -> {
            registerButton.setEnabled(false);
            switchToLoginButton.setEnabled(false);
            regStatusLabel.setText("注册中...");
            regStatusLabel.setForeground(Color.BLUE);
        });
    }
    
    /*
        重置登录表单
    */
    public void resetLoginForm() {
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(true);
            switchToRegisterButton.setEnabled(true);
            passwordField.setText("");
        });
    }
    
    /*
        重置注册表单
    */
    public void resetRegisterForm() {
        SwingUtilities.invokeLater(() -> {
            registerButton.setEnabled(true);
            switchToLoginButton.setEnabled(true);
            regPasswordField.setText("");
            regConfirmPasswordField.setText("");
        });
    }
    
    /*
        设置用户名
    */
    public void setUsername(String username) {
        SwingUtilities.invokeLater(() -> {
            usernameField.setText(username);
        });
    }
} 