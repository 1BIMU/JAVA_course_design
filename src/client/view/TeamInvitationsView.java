package client.view;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Group_info;
import info.Org_info;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 显示和处理小组邀请的窗口
 */
public class TeamInvitationsView extends JDialog {

    private ChatController controller;
    private ClientModel model;
    private JPanel mainPanel;
    private List<JButton> acceptButtons = new ArrayList<>();

    public TeamInvitationsView(JFrame parent, ChatController controller, ClientModel model) {
        super(parent, "待处理的小组邀请", true);
        this.controller = controller;
        this.model = model;

        setSize(500, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initComponents();
        populateInvitations();
    }

    private void initComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void populateInvitations() {
        mainPanel.removeAll();
        acceptButtons.clear();
        List<Org_info> invitations = model.getPendingTeamInvitations();
        Map<Integer, Group_info> groups = model.getGroups();

        if (invitations.isEmpty()) {
            mainPanel.add(new JLabel("没有待处理的邀请。"));
        } else {
            for (Org_info invitation : invitations) {
                // 查找父级群聊的名称
                Group_info parentGroup = groups.get(invitation.getGroup_id());
                String parentGroupName = (parentGroup != null) ? parentGroup.get_Group_name() : "未知群聊";

                mainPanel.add(createInvitationPanel(invitation, parentGroupName));
                mainPanel.add(Box.createRigidArea(new Dimension(0, 5))); // 分隔
            }
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private JPanel createInvitationPanel(Org_info invitation, String parentGroupName) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String text = String.format("<html><b>%s</b> 邀请您加入小组 <b>%s</b> (属于群聊: %s)</html>",
                invitation.getFromUser(), invitation.getOrg_name(), parentGroupName);
        panel.add(new JLabel(text), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton acceptButton = new JButton("接受");
        JButton declineButton = new JButton("拒绝");

        acceptButtons.add(acceptButton);

        acceptButton.addActionListener(e -> {
            controller.respondToTeamInvitation(invitation.getOrg_id(), true);
            disableAllAcceptButtons();
            acceptButton.setText("已接受");
            acceptButton.setEnabled(false);
            declineButton.setVisible(false);
        });

        declineButton.addActionListener(e -> {
            controller.respondToTeamInvitation(invitation.getOrg_id(), false);
            panel.setVisible(false); // 隐藏此邀请条目
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        return panel;
    }

    private void disableAllAcceptButtons() {
        for (JButton button : acceptButtons) {
            button.setEnabled(false);
            button.setText("已禁用");
        }
    }
}