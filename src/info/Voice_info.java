package info;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 语音通话信息类
 * 用于在客户端和服务器之间传递语音通话相关信息
 */
public class Voice_info implements Serializable {
    private static final long serialVersionUID = 1L;

    // 通话ID
    private int call_id;
    // 会议ID（多人通话）
    private int conference_id;
    // 发起人用户名
    private String from_username;
    // 目标用户列表
    private List<String> participants = new ArrayList<>();
    // 是否是会议通话
    private boolean is_conference;
    // 通话状态
    private CallStatus status;
    // 通话类型
    private CallType callType;
    // 主机地址
    private String host;
    // 服务器检测到的真实主机地址
    private String real_host;
    // 端口号
    private int port;
    // 错误消息
    private String error_message;

    /**
     * 通话状态枚举
     */
    public enum CallStatus {
        REQUESTING,    // 请求中
        CONNECTING,    // 连接中
        CONNECTED,     // 已连接
        ACCEPTED,      // 已接受
        REJECTED,      // 已拒绝
        ENDED,         // 已结束
        ERROR          // 错误
    }

    /**
     * 通话类型枚举
     */
    public enum CallType {
        AUDIO_ONLY,    // 仅音频
        VIDEO_ONLY,    // 仅视频
        AUDIO_VIDEO    // 音视频
    }

    /**
     * 默认构造函数
     */
    public Voice_info() {
        this.status = CallStatus.REQUESTING;
        this.callType = CallType.AUDIO_ONLY;
    }

    /**
     * 添加参与者
     * @param username 用户名
     */
    public void addParticipant(String username) {
        if (!participants.contains(username)) {
            participants.add(username);
        }
    }

    /**
     * 移除参与者
     * @param username 用户名
     * @return 是否成功移除
     */
    public boolean removeParticipant(String username) {
        return participants.remove(username);
    }

    /**
     * 设置错误状态和消息
     * @param errorMessage 错误消息
     */
    public void setError(String errorMessage) {
        this.status = CallStatus.ERROR;
        this.error_message = errorMessage;
    }

    // Getters 和 Setters

    public int getCall_id() {
        return call_id;
    }

    public void setCall_id(int call_id) {
        this.call_id = call_id;
    }

    public int getConference_id() {
        return conference_id;
    }

    public void setConference_id(int conference_id) {
        this.conference_id = conference_id;
    }

    public String getFrom_username() {
        return from_username;
    }

    public void setFrom_username(String from_username) {
        this.from_username = from_username;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public boolean isIs_conference() {
        return is_conference;
    }

    public void setIs_conference(boolean is_conference) {
        this.is_conference = is_conference;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取服务器检测到的真实主机地址
     * @return 真实主机地址
     */
    public String getReal_host() {
        return real_host;
    }

    /**
     * 设置服务器检测到的真实主机地址
     * @param real_host 真实主机地址
     */
    public void setReal_host(String real_host) {
        this.real_host = real_host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }
}
