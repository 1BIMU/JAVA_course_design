package io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileIO {
    // 默认文件路径
    private static final String DEFAULT_USERS_FILE = "users.dat";
    private static final String DEFAULT_GROUPS_FILE = "groups.dat";

    // 文件路径配置
    private final Path userFilePath;
    private final Path groupFilePath;
    // 聊天记录文件默认配置
    private static final String GROUP_CHAT_DIR = "chat_group_history";
    private static final String SINGLE_CHAT_DIR = "chat_single_history";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public FileIO() {
        this(DEFAULT_USERS_FILE, DEFAULT_GROUPS_FILE);
    }

    public FileIO(String userFile, String groupFile) {
        this.userFilePath = Paths.get(userFile);
        this.groupFilePath = Paths.get(groupFile);

        // 确保文件存在
        try {
            ensureFilesExist();
            Files.createDirectories(Paths.get(GROUP_CHAT_DIR));
            Files.createDirectories(Paths.get(SINGLE_CHAT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // region 用户相关操作
    public boolean writeUser(String username, String password) throws IOException {
        if (userExists(username)) {
            return false;
        }

        String record = username + "|" + password + "\n";
        Files.write(userFilePath, record.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        return true;
    }

    public boolean userExists(String username) throws IOException {
        if (!Files.exists(userFilePath)) return false;

        return Files.lines(userFilePath)
                .anyMatch(line -> {
                    String[] parts = line.split("\\|");
                    return parts.length > 0 && parts[0].equals(username);
                });
    }

    public boolean validateUser(String username, String password) throws IOException {
        if (!Files.exists(userFilePath)) return false;

        return Files.lines(userFilePath)
                .anyMatch(line -> {
                    String[] parts = line.split("\\|");
                    return parts.length >= 2
                            && parts[0].equals(username)
                            && parts[1].equals(password);
                });
    }
    // endregion

    // region 群聊相关操作
    public void writeGroup(int groupId, ArrayList<String> members) throws IOException {
        // 移除旧记录（如果存在）
        List<String> groups = Files.exists(groupFilePath) ?
                Files.readAllLines(groupFilePath) :
                new ArrayList<>();

        groups.removeIf(line -> line.startsWith(groupId + "|"));

        // 添加新记录
        String record = groupId + "|" + String.join(",", members);
        groups.add(record);

        Files.write(groupFilePath, groups,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public boolean groupExists(int groupId) throws IOException {
        if (!Files.exists(groupFilePath)) return false;

        return Files.lines(groupFilePath)
                .anyMatch(line -> {
                    String[] parts = line.split("\\|");
                    return parts.length > 0 && parts[0].equals(String.valueOf(groupId));
                });
    }

    public ArrayList<Integer> getGroupsByUser(String username) throws IOException {
        ArrayList<Integer> groupIds = new ArrayList<>();

        if (!Files.exists(groupFilePath)) {
            return groupIds;
        }

        Files.lines(groupFilePath).forEach(line -> {
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    int groupId = Integer.parseInt(parts[0]);
                    List<String> members = Arrays.asList(parts[1].split(","));
                    if (members.contains(username)) {
                        groupIds.add(groupId);
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误的群组ID
                }
            }
        });

        return groupIds;
    }
    public ArrayList<String> getGroupMembers(int groupId) throws IOException {
        if (!Files.exists(groupFilePath)) return null;

        return Files.lines(groupFilePath)
                .filter(line -> line.startsWith(groupId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length < 2 || parts[1].isEmpty()) {
                        return new ArrayList<String>();
                    }
                    return new ArrayList<>(Arrays.asList(parts[1].split(",")));
                })
                .orElse(null);
    }
    
    /*
        更新群组成员列表
    */
    public void updateGroupMembers(int groupId, ArrayList<String> members) throws IOException {
        // 读取现有群组数据
        List<String> groups = Files.exists(groupFilePath) ?
                Files.readAllLines(groupFilePath) :
                new ArrayList<>();

        // 查找目标群组
        boolean found = false;
        for (int i = 0; i < groups.size(); i++) {
            String line = groups.get(i);
            if (line.startsWith(groupId + "|")) {
                found = true;
                // 更新记录
                groups.set(i, groupId + "|" + String.join(",", members));
                break;
            }
        }

        if (!found) {
            // 如果群组不存在，则创建新记录
            groups.add(groupId + "|" + String.join(",", members));
        }

        // 写回文件
        Files.write(groupFilePath, groups,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /*
        删除群组
    */
    public void deleteGroup(int groupId) throws IOException {
        // 读取现有群组数据
        List<String> groups = Files.exists(groupFilePath) ?
                Files.readAllLines(groupFilePath) :
                new ArrayList<>();

        // 移除目标群组
        groups.removeIf(line -> line.startsWith(groupId + "|"));

        // 写回文件
        Files.write(groupFilePath, groups,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void ensureFilesExist() throws IOException {
        if (!Files.exists(userFilePath)) {
            Files.createFile(userFilePath);
        }
        if (!Files.exists(groupFilePath)) {
            Files.createFile(groupFilePath);
        }
    }
    // endregion
    
    public void manageGroupMembers(int groupId, ArrayList<String> addUsers, ArrayList<String> removeUsers) throws IOException {
        // 读取现有群组数据
        List<String> groups = Files.exists(groupFilePath) ?
                Files.readAllLines(groupFilePath) :
                new ArrayList<>();

        // 查找目标群组
        boolean found = false;
        for (int i = 0; i < groups.size(); i++) {
            String line = groups.get(i);
            if (line.startsWith(groupId + "|")) {
                found = true;
                // 解析现有成员
                String[] parts = line.split("\\|");
                ArrayList<String> members;
                
                if (parts.length < 2 || parts[1].isEmpty()) {
                    members = new ArrayList<>();
                } else {
                    members = new ArrayList<>(Arrays.asList(parts[1].split(",")));
                }

                // 添加新成员（去重）
                if (addUsers != null) {
                    for (String user : addUsers) {
                        if (!members.contains(user)) {
                            members.add(user);
                        }
                    }
                }

                // 移除指定成员
                if (removeUsers != null) {
                    members.removeAll(removeUsers);
                }

                // 更新记录
                groups.set(i, groupId + "|" + String.join(",", members));
                break;
            }
        }

        if (!found) {
            // 如果群组不存在，则创建新记录（仅当有添加用户时）
            if (addUsers != null && !addUsers.isEmpty()) {
                groups.add(groupId + "|" + String.join(",", addUsers));
            } else {
                throw new IllegalArgumentException("群组 " + groupId + " 不存在");
            }
        }

        // 写回文件
        Files.write(groupFilePath, groups,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    // 在用户相关操作区域添加以下方法
    public boolean updatePassword(String username, String newPassword) throws IOException {
        if (!Files.exists(userFilePath)) return false;

        // 读取所有用户记录
        List<String> lines = Files.readAllLines(userFilePath);
        boolean found = false;

        // 查找并更新密码
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\\|");
            if (parts.length > 0 && parts[0].equals(username)) {
                lines.set(i, username + "|" + newPassword);
                found = true;
                break;
            }
        }

        if (found) {
            // 覆盖写入
            Files.write(userFilePath, lines,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        }
        return false;
    }
    // region 统一聊天记录管理
    /**
     * 通用聊天记录保存方法
     * @param chatInfo 包含完整消息信息的对象
     */
    public synchronized void saveChatMessage(info.Chat_info chatInfo) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String record = String.join("|",
                timestamp,
                chatInfo.getFrom_username(),
                chatInfo.getMessage()
        );

        if (chatInfo.isType()) {
            // 群聊消息
            Path groupFile = Paths.get(GROUP_CHAT_DIR,
                    chatInfo.getGroup_id() + ".dat");
            Files.write(groupFile, (record + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } else {
            // 私聊消息
            String[] users = {chatInfo.getFrom_username(), chatInfo.getTo_username()};
            Arrays.sort(users); // 保证文件名一致性
            Path privateFile = Paths.get(SINGLE_CHAT_DIR,
                    users[0] + "_" + users[1] + ".dat");
            Files.write(privateFile, (record + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }

    /**
     * 获取聊天历史记录
     * @param chatInfo 包含查询条件的对象
     * @return 按时间排序的消息列表
     */
    public List<String> getChatHistory(info.Chat_info chatInfo) throws IOException {
        Path chatFile;

        if (chatInfo.isType()) {
            // 群聊记录路径
            chatFile = Paths.get(GROUP_CHAT_DIR,
                    chatInfo.getGroup_id() + ".dat");
        } else {
            // 私聊记录路径
            String[] users = {chatInfo.getFrom_username(), chatInfo.getTo_username()};
            Arrays.sort(users);
            chatFile = Paths.get(SINGLE_CHAT_DIR,
                    users[0] + "_" + users[1] + ".dat");
        }

        if (!Files.exists(chatFile)) {
            return Collections.emptyList();
        }

        return Files.readAllLines(chatFile);
    }
}

