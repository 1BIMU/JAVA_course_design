package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Group_info;
import info.encap_info;

/**
 * 客户端群组消息处理器
 */
public class GroupMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private ChatController chatController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param chatController 聊天控制器
     */
    public GroupMessageHandler(ClientModel model, ChatController chatController) {
        this.model = model;
        this.chatController = chatController;
    }
    
    @Override
    public void handle(encap_info message) {
        Group_info groupInfo = message.get_group_info();
        if (groupInfo == null) return;
        
        if (groupInfo.isEstablish()) {
            // 新建群组
            model.updateGroup(groupInfo);
            chatController.onGroupCreated(groupInfo);
        } 
        else {
            // 群组更新
            model.updateGroup(groupInfo);
            chatController.onGroupUpdated(groupInfo);
        }
    }
} 