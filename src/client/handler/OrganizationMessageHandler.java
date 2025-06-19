package client.handler;

import client.controller.ChatController;
import client.model.ClientModel;
import info.Org_info;
import info.encap_info;

/**
 * 客户端组织消息处理器
 * 用于处理组织相关的消息（创建组织、更新组织、离开组织等）
 */
public class OrganizationMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private ChatController chatController;
    
    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param chatController 聊天控制器
     */
    public OrganizationMessageHandler(ClientModel model, ChatController chatController) {
        this.model = model;
        this.chatController = chatController;
    }
    
    @Override
    public void handle(encap_info message) {
        Org_info orgInfo = message.get_org_info();
        if (orgInfo == null) return;
        
        // 处理组织信息
        if (orgInfo.isEstablish()) {
            // 处理新建组织的消息
            handleNewOrganization(orgInfo);
        } else {
            // 处理组织更新或离开的消息
            handleOrganizationUpdate(orgInfo);
        }
    }
    
    /**
     * 处理新建组织的消息
     * @param orgInfo 组织信息
     */
    private void handleNewOrganization(Org_info orgInfo) {
        if (orgInfo.isSuccess()) {
            // 组织创建成功
            System.out.println("新组织创建成功: " + orgInfo.getOrg_name() + " (ID: " + orgInfo.getOrg_id() + ")");
            // 将新组织添加到模型中
            model.updateOrg(orgInfo);
            // 通知聊天控制器处理新组织创建
            chatController.onOrgCreated(orgInfo);
        } else {
            // 组织创建失败
            System.out.println("新组织创建失败");
            // 显示创建失败的错误消息
            chatController.showError("创建组织失败");
        }
    }
    
    /**
     * 处理组织更新或离开的消息
     * @param orgInfo 组织信息
     */
    private void handleOrganizationUpdate(Org_info orgInfo) {
        int orgId = orgInfo.getOrg_id();
        
        if (orgInfo.isExist()) {
            // 组织仍然存在（可能是更新成员列表）
            System.out.println("组织更新: " + orgInfo.getOrg_name() + " (ID: " + orgId + ")");
            // 更新模型中的组织信息
            model.updateOrg(orgInfo);
            // 通知聊天控制器处理组织更新
            chatController.onOrgUpdated(orgInfo);
        } else {
            // 组织不存在（用户离开了组织）
            System.out.println("已离开组织，ID: " + orgId);
            // 从模型中移除组织
            model.removeOrg(orgId);
            // 通知聊天控制器处理组织删除
            chatController.onRemovedFromOrg(orgInfo);
        }
    }
} 