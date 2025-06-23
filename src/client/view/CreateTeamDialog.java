// ====== FILE: src\client\view\CreateTeamDialog.java ======

package client.view;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Group_info;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 创建小组的对话框 (最终修正版)
 */
public class CreateTeamDialog extends JDialog {
    private ChatController controller;
    private ClientModel model;

    // 直接使用 Group_info 作为 JComboBox 的类型
    private JComboBox<Group_info> parentGroupComboBox;
    private JTextField teamNameField;
    private JList<String> memberList;
    private DefaultListModel<String> memberListModel;

    public CreateTeamDialog(JFrame parent, ChatController controller, ClientModel model) {
        super(parent, "创建小组", true);
        this.controller = controller;
        this.model = model;

        setSize(400, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        populateParentGroupComboBox();
    }

    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 1. 选择父级群聊
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("所属群聊:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        // JComboBox 的泛型已修改为 Group_info
        parentGroupComboBox = new JComboBox<>();
        formPanel.add(parentGroupComboBox, gbc);

        // 2. 小组名称
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("小组名称:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        teamNameField = new JTextField();
        formPanel.add(teamNameField, gbc);

        // 3. 选择成员
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("选择成员:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        memberListModel = new DefaultListModel<>();
        memberList = new JList<>(memberListModel);
        memberList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        formPanel.add(new JScrollPane(memberList), gbc);

        // 监听ComboBox变化，动态更新成员列表
        parentGroupComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateMemberList();
            }
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("创建");
        JButton cancelButton = new JButton("取消");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        okButton.addActionListener(e -> createTeam());
        cancelButton.addActionListener(e -> dispose());

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateParentGroupComboBox() {
        // 清空并直接添加 Group_info 对象
        parentGroupComboBox.removeAllItems();
        Map<Integer, Group_info> groups = model.getGroups();
        for (Group_info groupInfo : groups.values()) {
            parentGroupComboBox.addItem(groupInfo);
        }
        // 初始时更新一次成员列表
        updateMemberList();
    }

    private void updateMemberList() {
        memberListModel.clear();
        // 直接从 ComboBox 获取 Group_info 对象，不再需要强制转换
        Group_info selectedGroup = (Group_info) parentGroupComboBox.getSelectedItem();
        if (selectedGroup != null) {
            ArrayList<String> members = selectedGroup.getMembers();
            Collections.sort(members);
            for (String member : members) {
                memberListModel.addElement(member);
            }
        }
    }

    private void createTeam() {
        // 直接获取 Group_info 对象
        Group_info selectedGroup = (Group_info) parentGroupComboBox.getSelectedItem();
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "请选择一个所属群聊。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String teamName = teamNameField.getText().trim();
        if (teamName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "小组名称不能为空。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> selectedMembers = memberList.getSelectedValuesList();
        if (selectedMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一名小组成员。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 确保创建者在成员列表中
        if (!selectedMembers.contains(model.getCurrentUser())) {
            // JList 的 getSelectedValuesList 返回的是不可修改的 List，需要包装一下
            List<String> mutableMembers = new ArrayList<>(selectedMembers);
            mutableMembers.add(model.getCurrentUser());
            selectedMembers = mutableMembers;
        }

        int parentGroupId = selectedGroup.get_Group_id();
        controller.createTeam(parentGroupId, teamName, selectedMembers);
        dispose();
    }
}