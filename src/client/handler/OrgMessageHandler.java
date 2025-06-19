package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Org_info;
import info.encap_info;

/**
 * 客户端小组消息处理器
 */
public class OrgMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private ChatController chatController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param chatController 聊天控制器
     */
    public OrgMessageHandler(ClientModel model, ChatController chatController) {
        this.model = model;
        this.chatController = chatController;
    }
    
    @Override
    public void handle(encap_info message) {
        Org_info orgInfo = message.get_org_info();
        if (orgInfo == null) return;
        
        if (orgInfo.isEstablish()) {
            // 新建小组
            if (orgInfo.isSuccess()) {
                model.updateOrg(orgInfo);
                chatController.onOrgCreated(orgInfo);
            } else {
                // 创建小组失败
                chatController.showError("创建小组失败: 可能是有成员已在其他小组中");
            }
        } else if (!orgInfo.isExist()) {
            // 被移出小组
            model.removeOrg(orgInfo.getOrg_id());
            chatController.onRemovedFromOrg(orgInfo);
        } else {
            // 小组更新
            model.updateOrg(orgInfo);
            chatController.onOrgUpdated(orgInfo);
        }
    }
} 