package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;


import Server.handler.MessageHandler;
import client.controller.ChatController;
import client.model.ClientModel;
import info.*;

import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import client.view.GroupInvitationDialog;
public class OrgMessageHandler implements ClientMessageHandler {
    private ChatController chatController;
    private ClientModel clientModel;
    public OrgMessageHandler(ChatController chatController,ClientModel clientModel) {
        this.chatController = chatController;
        this.clientModel = clientModel;
    }



    @Override
    public void handle(encap_info info) {
        Org_info INFO = info.get_org_info();
        //收到消息有三种情况，一种是收到了错误消息，一种是收到了邀请进群消息。
        if (!INFO.isSuccess()){
            //发生错误，弹出报错窗口
            JOptionPane.showMessageDialog(
                    null,                   // 报错窗口
                    "服务端返回报错，您是否添加了不属于该群的成员，或该群聊不存在",      // 错误消息
                    "错误",                 // 窗口标题
                    JOptionPane.ERROR_MESSAGE // 消息类型（显示错误图标）
            );
        }else{
            if(INFO.getType()==2){
                boolean flag = GroupInvitationDialog.showInvitationDialog(INFO.getOrg_name(),INFO.getFromUser());
                if(flag){//如果选择接受，那么回复一个ack消息
                    chatController.InviteAgreement(INFO);
                    clientModel.updateGOrg(INFO);
                }
            }else if(INFO.getType()==5){//你被T了
                int org_id = INFO.getOrg_id();
                clientModel.removeOrg(org_id);//数据库中清除
                JOptionPane.showMessageDialog(
                        null,                   // 报错窗口
                        "你被群聊"+INFO.getOrg_name()+"踢了",      // 错误消息
                        "很遗憾",                 // 窗口标题
                        JOptionPane.INFORMATION_MESSAGE // 消息类型（显示错误图标）
                );
            }

        }
    }
}

