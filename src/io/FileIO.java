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
import java.util.Map;
import java.util.HashMap;

public class FileIO {
    // 默认文件路径
    private static final String DEFAULT_USERS_FILE = "users.dat";
    private static final String DEFAULT_GROUPS_FILE = "groups.dat";
    private static final String DEFAULT_ORGS_FILE = "orgs.dat";

    // 文件路径配置
    private final Path userFilePath;
    private final Path groupFilePath;
    private final Path orgFilePath; // 小组文件路径
    
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
        this.orgFilePath = null; // 不使用小组文件

        // 确保文件存在
        try {
            ensureFilesExist();
            Files.createDirectories(Paths.get(GROUP_CHAT_DIR));
            Files.createDirectories(Paths.get(SINGLE_CHAT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 使用用户文件和小组文件初始化
     * @param userFile 用户文件路径
     * @param orgFile 小组文件路径
     */
    public FileIO(String userFile, String orgFile, boolean isOrgFile) {
        this.userFilePath = Paths.get(userFile);
        this.groupFilePath = null; // 不使用群组文件
        this.orgFilePath = Paths.get(orgFile);

        // 确保文件存在
        try {
            if (!Files.exists(userFilePath)) {
                Files.createFile(userFilePath);
            }
            if (!Files.exists(orgFilePath)) {
                Files.createFile(orgFilePath);
            }
            Files.createDirectories(Paths.get(GROUP_CHAT_DIR));
            Files.createDirectories(Paths.get(SINGLE_CHAT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // region 用户相关操作
    // 修改现有writeUser方法以兼容旧格式
    public boolean writeUser(String username, String password) throws IOException {
        return writeUser(username, password, ""); // 默认空IP
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
    public void writeGroup(int groupId, String groupName, ArrayList<String> members) throws IOException {
        // 移除旧记录（如果存在）
        List<String> groups = Files.exists(groupFilePath) ?
                Files.readAllLines(groupFilePath) :
                new ArrayList<>();

        groups.removeIf(line -> line.startsWith(groupId + "|"));

        // 添加新记录，包含群组名称
        String record = groupId + "|" + groupName + "|" + String.join(",", members);
        groups.add(record);

        Files.write(groupFilePath, groups,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    // 兼容旧方法，但加上默认群名
    public void writeGroup(int groupId, ArrayList<String> members) throws IOException {
        // 默认群组名称
        String defaultGroupName = "群聊 " + groupId;
        writeGroup(groupId, defaultGroupName, members);
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
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {  // 现在格式是 groupId|groupName|members
                try {
                    int groupId = Integer.parseInt(parts[0]);
                    // 第三部分是成员列表
                    List<String> members = Arrays.asList(parts[2].split(","));
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
                    if (parts.length < 3 || parts[2].isEmpty()) {
                        return new ArrayList<String>();
                    }
                    return new ArrayList<>(Arrays.asList(parts[2].split(",")));
                })
                .orElse(null);
    }
    
    /**
     * 获取群组名称
     * @param groupId 群组ID
     * @return 群组名称，如果不存在则返回默认名称
     */
    public String getGroupName(int groupId) throws IOException {
        if (!Files.exists(groupFilePath)) return "群聊 " + groupId;

        return Files.lines(groupFilePath)
                .filter(line -> line.startsWith(groupId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length < 2 || parts[1].isEmpty()) {
                        return "群聊 " + groupId;
                    }
                    return parts[1];
                })
                .orElse("群聊 " + groupId);
    }
    
    /**
     * 获取群组信息（名称和成员）
     * @param groupId 群组ID
     * @return 包含名称和成员的Map，如果不存在则返回null
     */
    public Map<String, Object> getGroupInfo(int groupId) throws IOException {
        if (!Files.exists(groupFilePath)) return null;

        return Files.lines(groupFilePath)
                .filter(line -> line.startsWith(groupId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    Map<String, Object> info = new HashMap<>();
                    
                    // 设置默认名称
                    String name = "群聊 " + groupId;
                    
                    // 如果有名称部分，使用它
                    if (parts.length >= 2) {
                        name = parts[1];
                    }
                    
                    info.put("name", name);
                    
                    // 处理成员列表
                    ArrayList<String> members = new ArrayList<>();
                    if (parts.length >= 3 && !parts[2].isEmpty()) {
                        members = new ArrayList<>(Arrays.asList(parts[2].split(",")));
                    }
                    
                    info.put("members", members);
                    return info;
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
                // 解析群组名称
                String[] parts = line.split("\\|");
                String groupName;
                
                // 获取群组名称，如果有
                if (parts.length >= 2) {
                    groupName = parts[1];
                } else {
                    groupName = "群聊 " + groupId;
                }
                
                // 更新记录，保持群组名称不变
                groups.set(i, groupId + "|" + groupName + "|" + String.join(",", members));
                break;
            }
        }

        if (!found) {
            // 如果群组不存在，则创建新记录，使用默认名称
            String defaultGroupName = "群聊 " + groupId;
            groups.add(groupId + "|" + defaultGroupName + "|" + String.join(",", members));
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
                // 解析现有成员和群组名称
                String[] parts = line.split("\\|");
                String groupName;
                ArrayList<String> members;
                
                // 获取群组名称
                if (parts.length >= 2) {
                    groupName = parts[1];
                } else {
                    groupName = "群聊 " + groupId;
                }
                
                // 获取成员列表
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    members = new ArrayList<>(Arrays.asList(parts[2].split(",")));
                } else {
                    members = new ArrayList<>();
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

                // 更新记录，保持群组名称不变
                groups.set(i, groupId + "|" + groupName + "|" + String.join(",", members));
                break;
            }
        }

        if (!found) {
            // 如果群组不存在，则创建新记录（仅当有添加用户时）
            if (addUsers != null && !addUsers.isEmpty()) {
                // 创建新群组，使用默认名称
                String groupName = "群聊 " + groupId;
                groups.add(groupId + "|" + groupName + "|" + String.join(",", addUsers));
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
                chatInfo.getText()
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

    /**
     * 获取所有注册用户的列表
     * @return 所有注册用户的列表
     * @throws IOException 如果读取文件时出错
     */
    public ArrayList<String> getAllUsers() throws IOException {
        ArrayList<String> users = new ArrayList<>();
        
        if (!Files.exists(userFilePath)) {
            return users;
        }
        
        Files.lines(userFilePath).forEach(line -> {
            String[] parts = line.split("\\|");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                users.add(parts[0]);
            }
        });
        
        return users;
    }

    // region 小组相关操作
    /**
     * 写入小组信息
     * @param orgId 小组ID
     * @param orgName 小组名称
     * @param members 小组成员
     * @throws IOException 如果发生I/O错误
     */
    public void writeOrg(int orgId, String orgName, ArrayList<String> members) throws IOException {
        if (orgFilePath == null) {
            throw new IOException("未配置小组文件路径");
        }
        
        // 移除旧记录（如果存在）
        List<String> orgs = Files.exists(orgFilePath) ?
                Files.readAllLines(orgFilePath) :
                new ArrayList<>();

        orgs.removeIf(line -> {
            String[] parts = line.split("\\|");
            return parts.length > 0 && parts[0].equals(String.valueOf(orgId));
        });

        // 添加新记录，包含小组名称
        String record = orgId + "|" + orgName + "|" + String.join(",", members);
        orgs.add(record);

        Files.write(orgFilePath, orgs,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * 检查小组是否存在
     * @param orgId 小组ID
     * @return 如果小组存在则返回true
     * @throws IOException 如果发生I/O错误
     */
    public boolean orgExists(int orgId) throws IOException {
        if (orgFilePath == null || !Files.exists(orgFilePath)) {
            return false;
        }

        return Files.lines(orgFilePath)
                .anyMatch(line -> {
                    String[] parts = line.split("\\|");
                    return parts.length > 0 && parts[0].equals(String.valueOf(orgId));
                });
    }
    
    /**
     * 获取用户所属的小组ID列表
     * @param username 用户名
     * @return 用户所属的小组ID列表
     * @throws IOException 如果发生I/O错误
     */
    public ArrayList<Integer> getOrgsByUser(String username) throws IOException {
        ArrayList<Integer> orgIds = new ArrayList<>();

        if (orgFilePath == null || !Files.exists(orgFilePath)) {
            return orgIds;
        }

        Files.lines(orgFilePath).forEach(line -> {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {  // 格式是 orgId|orgName|members
                try {
                    int orgId = Integer.parseInt(parts[0]);
                    // 第三部分是成员列表
                    String[] membersArray = parts[2].split(",");
                    List<String> members = Arrays.asList(membersArray);
                    if (members.contains(username)) {
                        orgIds.add(orgId);
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误的小组ID
                }
            }
        });

        return orgIds;
    }
    
    /**
     * 获取小组成员
     * @param orgId 小组ID
     * @return 小组成员列表
     * @throws IOException 如果发生I/O错误
     */
    public ArrayList<String> getOrgMembers(int orgId) throws IOException {
        if (orgFilePath == null || !Files.exists(orgFilePath)) {
            return null;
        }

        return Files.lines(orgFilePath)
                .filter(line -> line.startsWith(orgId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length < 3 || parts[2].isEmpty()) {
                        return new ArrayList<String>();
                    }
                    return new ArrayList<>(Arrays.asList(parts[2].split(",")));
                })
                .orElse(null);
    }
    
    /**
     * 获取小组名称
     * @param orgId 小组ID
     * @return 小组名称，如果不存在则返回默认名称
     * @throws IOException 如果发生I/O错误
     */
    public String getOrgName(int orgId) throws IOException {
        if (orgFilePath == null || !Files.exists(orgFilePath)) {
            return "小组 " + orgId;
        }

        return Files.lines(orgFilePath)
                .filter(line -> line.startsWith(orgId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length < 2 || parts[1].isEmpty()) {
                        return "小组 " + orgId;
                    }
                    return parts[1];
                })
                .orElse("小组 " + orgId);
    }
    
    /**
     * 管理小组成员
     * @param orgId 小组ID
     * @param addUsers 要添加的用户
     * @param removeUsers 要移除的用户
     * @throws IOException 如果发生I/O错误
     */
    public void manageOrgMembers(int orgId, ArrayList<String> addUsers, ArrayList<String> removeUsers) throws IOException {
        if (orgFilePath == null) {
            throw new IOException("未配置小组文件路径");
        }
        
        // 获取当前小组成员
        ArrayList<String> currentMembers = getOrgMembers(orgId);
        if (currentMembers == null) {
            throw new IOException("小组不存在: " + orgId);
        }
        
        // 执行添加操作
        if (addUsers != null && !addUsers.isEmpty()) {
            for (String user : addUsers) {
                if (!currentMembers.contains(user)) {
                    currentMembers.add(user);
                }
            }
        }
        
        // 执行移除操作
        if (removeUsers != null && !removeUsers.isEmpty()) {
            currentMembers.removeAll(removeUsers);
        }
        
        // 获取小组名称
        String orgName = getOrgName(orgId);
        
        // 更新小组
        writeOrg(orgId, orgName, currentMembers);
    }
    // endregion

    // 在用户相关操作区域添加以下方法

    /**
     * 写入用户IP地址
     * @param username 用户名
     * @param ip IP地址
     * @return 是否成功写入
     * @throws IOException 如果发生I/O错误
     */
    public boolean writeUserIP(String username, String ip) throws IOException {
        if (!Files.exists(userFilePath)) return false;

        // 读取所有用户记录
        List<String> lines = Files.readAllLines(userFilePath);
        boolean found = false;

        // 查找用户并更新IP
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\\|");
            if (parts.length > 0 && parts[0].equals(username)) {
                // 格式: username|password|ip
                String newRecord;
                if (parts.length >= 3) {
                    // 替换现有IP
                    newRecord = parts[0] + "|" + parts[1] + "|" + ip;
                } else if (parts.length == 2) {
                    // 添加IP字段
                    newRecord = lines.get(i) + "|" + ip;
                } else {
                    // 无效记录，跳过
                    continue;
                }
                lines.set(i, newRecord);
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

    /**
     * 获取用户IP地址
     * @param username 用户名
     * @return IP地址，如果用户不存在或没有IP则返回空字符串
     * @throws IOException 如果发生I/O错误
     */
    public String getUserIP(String username) throws IOException {
        if (!Files.exists(userFilePath)) return "";

        return Files.lines(userFilePath)
                .filter(line -> line.startsWith(username + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    // 格式: username|password|ip
                    if (parts.length >= 3) {
                        return parts[2];
                    }
                    return ""; // 没有IP记录
                })
                .orElse(""); // 用户不存在
    }

    /**
     * 获取所有用户及其IP的映射
     * @return 用户-IP映射表
     * @throws IOException 如果发生I/O错误
     */
    public Map<String, String> getAllUserIPs() throws IOException {
        Map<String, String> userIPs = new HashMap<>();

        if (!Files.exists(userFilePath)) {
            return userIPs;
        }

        Files.lines(userFilePath).forEach(line -> {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) { // 确保有IP字段
                userIPs.put(parts[0], parts[2]);
            } else if (parts.length >= 1) {
                // 用户存在但没有IP记录
                userIPs.put(parts[0], "");
            }
        });

        return userIPs;
    }

    /**
     * 更新用户写入方法以支持IP (可选)
     * 保持原有方法签名不变，增加带IP的注册方法
     */
    public boolean writeUser(String username, String password, String ip) throws IOException {
        if (userExists(username)) {
            return false;
        }

        String record = username + "|" + password + "|" + ip + "\n";
        Files.write(userFilePath, record.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        return true;
    }


}

