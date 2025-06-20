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
        if (voiceInfo == null) return;
        
        // 调用语音通话控制器处理消息
        voiceCallController.handleVoiceCallMessage(voiceInfo);
    }
} 