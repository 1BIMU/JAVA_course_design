package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Org_info;
import info.encap_info;

import javax.swing.*;

public class OrgMessageHandler implements ClientMessageHandler {
    private ChatController chatController;
    private ClientModel clientModel;

    public OrgMessageHandler(ChatController chatController, ClientModel clientModel) {
        this.chatController = chatController;
        this.clientModel = clientModel;
    }

    @Override
    public void handle(encap_info info) {
        Org_info orgInfo = info.get_org_info();
        if (orgInfo == null) return;

        // 收到错误消息
        if (!orgInfo.isSuccess()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        null,
                        "服务端返回报错，您是否添加了不属于该群的成员，或该群聊不存在",
                        "小组操作错误",
                        JOptionPane.ERROR_MESSAGE
                );
            });
        } else {
            // type 2 代表这是一个邀请
            if (orgInfo.getType() == 2) {
                // 将邀请添加到数据模型中，由视图监听并响应
                clientModel.addPendingTeamInvitation(orgInfo);
                System.out.println("收到小组邀请：" + orgInfo.getOrg_name() + "，已添加到待处理列表。");
            }
            // type 5 代表你被踢出
            else if (orgInfo.getType() == 5) {
                clientModel.removeOrg(orgInfo.getOrg_id()); // 从模型中清除
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "您已被管理员移出小组: " + orgInfo.getOrg_name(),
                            "很遗憾",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            }
            // type 1, 3, 4 代表小组信息更新 (创建成功/加入成功/成员变动)
            else if (orgInfo.getType() == 1 || orgInfo.getType() == 3 || orgInfo.getType() == 4) {
                clientModel.updateGOrg(orgInfo);
                System.out.println("小组信息已更新: " + orgInfo.getOrg_name());
            }
        }
    }
}