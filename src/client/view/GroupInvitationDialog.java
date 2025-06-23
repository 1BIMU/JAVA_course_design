package client.view;

import javax.swing.*;

public class GroupInvitationDialog {
    public static boolean showInvitationDialog(String groupName, String inviter) {
        // 自定义按钮文本
        Object[] options = {"接受", "拒绝"};

        // 弹出选择对话框
        int choice = JOptionPane.showOptionDialog(
                null,
                inviter + " 邀请您加入群组: " + groupName,
                "群组邀请",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0] // 默认选中"接受"
        );

        // 返回用户选择 (true=接受, false=拒绝)
        return choice == JOptionPane.YES_OPTION;
    }
}