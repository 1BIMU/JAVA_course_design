package client.handler;

import client.controller.VoiceCallController;
import client.model.ClientModel;
import info.Voice_info;
import info.encap_info;

/**
 * 客户端语音通话消息处理器
 * 用于处理语音通话相关的消息
 */
public class VoiceCallMessageHandler implements ClientMessageHandler {
    private ClientModel model;
    private VoiceCallController voiceCallController;

    /**
     * 构造函数
     * @param model 客户端数据模型
     * @param voiceCallController 语音通话控制器
     */
    public VoiceCallMessageHandler(ClientModel model, VoiceCallController voiceCallController) {
        this.model = model;
        this.voiceCallController = voiceCallController;
    }

    @Override
    public void handle(encap_info message) {
        // 获取语音通话信息
        Voice_info voiceInfo = message.get_voice_info();
        if (voiceInfo == null) {
            System.err.println("收到语音通话消息，但语音信息为空");
            return;
        }

        // 记录接收到的语音通话消息
        System.out.println("收到语音通话消息: " +
                "通话ID=" + voiceInfo.getCall_id() +
                ", 状态=" + voiceInfo.getStatus() +
                ", 发送方=" + voiceInfo.getFrom_username() +
                ", 参与者数=" + (voiceInfo.getParticipants() != null ? voiceInfo.getParticipants().size() : 0));

        // 调用语音通话控制器处理消息
        voiceCallController.handleVoiceCallMessage(voiceInfo);
    }
}
