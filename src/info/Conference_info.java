package info;

import io.AudioIO;

import java.io.*;
import java.net.InetAddress;

public class Conference_info implements Serializable {//这里是
    @Serial
    private static final long serialVersionUID = 1L;
    /*  定义的一些动作常量因为这个数据要传，存着有点浪费空间。
        ACTION_CREATE = 1; // 创建会议
        ACTION_JOIN = 2;   // 加入会议
        ACTION_LEAVE = 3;  // 离开会议
        ACTION_SEND_MESSAGE = 4;//发送最大大小为1500字节的消息

        ACTION_CREATE_RESPONSE = 11; // 创建会议回复
        ACTION_JOIN_RESPONSE = 12;   // 加入会议回复
        ACTION_LEAVE_RESPONSE = 13;  // 离开会议回复
        ACTION_SEND_RESPONSE = 14;
    */
    private int actionType; // 动作类型
    private int conferenceId; // 会议ID，这里直接用group中的ID就行，因为一个group一个时间只能发起一个连接
    private boolean success; // 操作是否成功
    private String fromUser;// 这个包来自于哪个用户
    AudioIO.AudioData audioData;//储存的音频数据

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }
    public String getFromUser() {
        return fromUser;
    }
    public AudioIO.AudioData getAudioData() {
        return audioData;
    }
    public void setAudioData(AudioIO.AudioData audioData) {
        this.audioData = audioData;
    }
    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public int getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(int conferenceId) {
        this.conferenceId = conferenceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}