package client.view;

import client.controller.VoiceCallController;
import info.Voice_info;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 语音通话界面
 * 显示通话状态和提供用户交互
 */
public class VoiceCallView extends JFrame {
    // 语音通话控制器
    private final VoiceCallController controller;
    // 语音信息
    private Voice_info voiceInfo;
    // 当前登录用户名
    private String currentUsername;
    // 通话状态标签
    private JLabel statusLabel;
    // 通话时长标签
    private JLabel durationLabel;
    // 通话时长计时器
    private Timer durationTimer;
    // 通话开始时间
    private long callStartTime;
    // 状态面板
    private JPanel statusPanel;
    // 操作面板
    private JPanel actionPanel;
    // 接受按钮
    private JButton acceptButton;
    // 拒绝/挂断按钮
    private JButton rejectButton;
    // 静音按钮
    private JToggleButton muteButton;

    /**
     * 构造函数
     * @param controller 语音通话控制器
     * @param voiceInfo 语音信息
     * @param currentUsername 当前用户名
     */
    public VoiceCallView(VoiceCallController controller, Voice_info voiceInfo, String currentUsername) {
        this.controller = controller;
        this.voiceInfo = voiceInfo;
        this.currentUsername = currentUsername;

        // 判断是否为呼叫发起方
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);
        
        // 输出调试信息
        System.out.println("创建语音通话界面: " + 
                          "当前用户=" + currentUsername + 
                          ", 呼叫方=" + voiceInfo.getFrom_username() + 
                          ", 是否为发起方=" + isInitiator + 
                          ", 通话状态=" + voiceInfo.getStatus());

        // 设置窗口标题和属性
        setTitle(isInitiator ? "呼出通话" : "来电");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 结束通话
                controller.endCall(voiceInfo.getCall_id());
            }
        });

        // 初始化界面
        initUI();

        // 根据通话状态更新界面
        updateCallStatus(voiceInfo.getStatus());
    }

    /**
     * 初始化界面
     */
    private void initUI() {
        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建标题面板
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 创建状态面板
        statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.CENTER);

        // 创建操作面板
        actionPanel = createActionPanel();
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        // 添加主面板到窗口
        setContentPane(mainPanel);
    }

    /**
     * 创建标题面板
     * @return 标题面板
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // 判断是否为呼叫发起方
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);
        
        // 创建标题标签
        String title;
        if (voiceInfo.isIs_conference()) {
            title = "语音会议";
        } else {
            if (isInitiator) {
                // 如果是呼出通话
                String participant = voiceInfo.getParticipants().get(0);
                title = "呼叫 " + participant;
            } else {
                // 如果是来电
                title = "来自 " + voiceInfo.getFrom_username() + " 的通话";
            }
        }

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建状态面板
     * @return 状态面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1, 5, 5));

        // 创建状态标签
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        panel.add(statusLabel);

        // 创建时长标签
        durationLabel = new JLabel("", SwingConstants.CENTER);
        durationLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        panel.add(durationLabel);

        return panel;
    }

    /**
     * 创建操作面板
     * @return 操作面板
     */
    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // 创建接受按钮
        acceptButton = new JButton("接受");
        acceptButton.setPreferredSize(new Dimension(100, 40));
        acceptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.acceptCall(voiceInfo.getCall_id());
            }
        });

        // 创建拒绝/挂断按钮
        rejectButton = new JButton("拒绝");
        rejectButton.setPreferredSize(new Dimension(100, 40));
        rejectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (voiceInfo.getStatus() == Voice_info.CallStatus.REQUESTING) {
                    controller.rejectCall(voiceInfo.getCall_id());
                } else {
                    controller.endCall(voiceInfo.getCall_id());
                }
            }
        });

        // 创建静音按钮
        muteButton = new JToggleButton("静音");
        muteButton.setPreferredSize(new Dimension(100, 40));
        muteButton.setEnabled(false);
        muteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: 实现静音功能
                boolean muted = muteButton.isSelected();
                if (muted) {
                    muteButton.setText("取消静音");
                } else {
                    muteButton.setText("静音");
                }
            }
        });

        // 根据通话状态添加按钮
        if (voiceInfo.getStatus() == Voice_info.CallStatus.REQUESTING) {
            // 判断是否为呼叫发起方
            boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);
            
            if (!isInitiator) {
                // 如果是呼入通话，显示接受和拒绝按钮
                panel.add(acceptButton);
                panel.add(rejectButton);
            } else {
                // 如果是呼出通话，只显示取消按钮
                rejectButton.setText("取消");
                panel.add(rejectButton);
            }
        } else {
            // 如果通话已连接，显示挂断和静音按钮
            rejectButton.setText("挂断");
            panel.add(rejectButton);
            panel.add(muteButton);
        }

        return panel;
    }

    /**
     * 更新通话状态
     * @param status 通话状态
     */
    public void updateCallStatus(Voice_info.CallStatus status) {
        voiceInfo.setStatus(status);
        
        // 判断是否为呼叫发起方
        boolean isInitiator = voiceInfo.getFrom_username().equals(currentUsername);

        switch (status) {
            case REQUESTING:
                if (isInitiator) {
                    statusLabel.setText("正在呼叫...");
                } else {
                    statusLabel.setText("来电...");
                }
                break;

            case CONNECTING:
                statusLabel.setText("正在连接...");
                // 更新按钮
                updateButtonsForConnecting();
                break;

            case CONNECTED:
                statusLabel.setText("通话已连接");
                // 开始计时
                startDurationTimer();
                // 更新按钮
                updateButtonsForConnected();
                break;

            case ENDED:
                statusLabel.setText("通话已结束");
                // 停止计时
                stopDurationTimer();
                // 禁用所有按钮
                disableAllButtons();
                break;

            case REJECTED:
                statusLabel.setText("通话被拒绝");
                // 禁用所有按钮
                disableAllButtons();
                break;

            case ERROR:
                statusLabel.setText("通话错误");
                // 禁用所有按钮
                disableAllButtons();
                break;

            default:
                statusLabel.setText("未知状态");
        }
    }

    /**
     * 更新按钮状态为连接中
     */
    private void updateButtonsForConnecting() {
        // 移除所有按钮
        actionPanel.removeAll();

        // 更新拒绝按钮为挂断
        rejectButton.setText("挂断");
        actionPanel.add(rejectButton);

        // 刷新面板
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    /**
     * 更新按钮状态为已连接
     */
    private void updateButtonsForConnected() {
        // 移除所有按钮
        actionPanel.removeAll();

        // 添加挂断和静音按钮
        rejectButton.setText("挂断");
        muteButton.setEnabled(true);
        actionPanel.add(rejectButton);
        actionPanel.add(muteButton);

        // 刷新面板
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    /**
     * 禁用所有按钮
     */
    private void disableAllButtons() {
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        muteButton.setEnabled(false);
    }

    /**
     * 开始计时器
     */
    public void startDurationTimer() {
        // 记录开始时间
        callStartTime = System.currentTimeMillis();

        // 停止现有计时器
        stopDurationTimer();

        // 创建新计时器
        durationTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDurationLabel();
            }
        });
        durationTimer.start();

        // 立即更新一次
        updateDurationLabel();
    }

    /**
     * 停止计时器
     */
    private void stopDurationTimer() {
        if (durationTimer != null && durationTimer.isRunning()) {
            durationTimer.stop();
            durationTimer = null;
        }
    }

    /**
     * 更新时长标签
     */
    private void updateDurationLabel() {
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - callStartTime;
        
        // 转换为时分秒
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        // 格式化时间
        String timeStr;
        if (hours > 0) {
            timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeStr = String.format("%02d:%02d", minutes, seconds);
        }
        
        durationLabel.setText("通话时长: " + timeStr);
    }

    /**
     * 显示通话被拒绝
     */
    public void showCallRejected() {
        statusLabel.setText("通话被拒绝");
        disableAllButtons();
    }

    /**
     * 显示通话结束
     */
    public void showCallEnded() {
        statusLabel.setText("通话已结束");
        stopDurationTimer();
        disableAllButtons();
    }

    /**
     * 显示通话错误
     * @param voiceInfo 语音信息
     */
    public void showCallError(Voice_info voiceInfo) {
        statusLabel.setText("通话错误: " + voiceInfo.getError_message());
        stopDurationTimer();
        disableAllButtons();
    }

    /**
     * 获取语音信息
     * @return 语音信息
     */
    public Voice_info getVoiceInfo() {
        return voiceInfo;
    }

    /**
     * 设置语音信息
     * @param voiceInfo 语音信息
     */
    public void setVoiceInfo(Voice_info voiceInfo) {
        this.voiceInfo = voiceInfo;
    }
} 