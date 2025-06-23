package io;

import info.Org_info;

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

    public void writeGroup(int groupId, String groupName, String member) throws IOException {
        // 将单个用户转为单元素列表，调用原方法
        writeGroup(groupId, groupName, new ArrayList<>(Collections.singletonList(member)));
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
    
    private void ensureFilesExist() throws IOException {
        if (!Files.exists(userFilePath)) {
            Files.createFile(userFilePath);
        }
        if (!Files.exists(groupFilePath)) {
            Files.createFile(groupFilePath);
        }
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

    // --- NEW ---
    // region 新增的小组(Org)相关操作，并添加了详细日志
    /**
     * 将小组信息写入文件 (新格式: orgId|parentGroupId|orgName|members)
     * @param orgId 小组ID
     * @param parentGroupId 父群聊ID
     * @param orgName 小组名称
     * @param members 成员列表
     * @throws IOException
     */
    public void writeOrg(int orgId, int parentGroupId, String orgName, ArrayList<String> members) throws IOException {
        Path orgFilePath = this.groupFilePath;
        // --- NEW LOGGING ---
        System.out.println("[FileIO.writeOrg] 准备写入小组到文件: " + orgFilePath.toAbsolutePath());

        List<String> orgs = Files.exists(orgFilePath) ?
                Files.readAllLines(orgFilePath) :
                new ArrayList<>();

        orgs.removeIf(line -> line.startsWith(orgId + "|"));

        String record = orgId + "|" + parentGroupId + "|" + orgName + "|" + String.join(",", members);
        // --- NEW LOGGING ---
        System.out.println("[FileIO.writeOrg] 待写入的记录: " + record);
        orgs.add(record);

        Files.write(orgFilePath, orgs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // --- NEW LOGGING ---
        System.out.println("[FileIO.writeOrg] 写入成功.");
    }

    /**
     * 通过用户获取其所属的所有小组信息
     * @param username 用户名
     * @return 包含该用户的小组信息列表
     * @throws IOException
     */
    public ArrayList<Org_info> getAllOrgsByUser(String username) throws IOException {
        Path orgFilePath = this.groupFilePath;
        // --- NEW LOGGING ---
        System.out.println("[FileIO.getAllOrgsByUser] 为用户 '" + username + "' 读取小组信息，文件路径: " + orgFilePath.toAbsolutePath());

        ArrayList<Org_info> userOrgs = new ArrayList<>();

        if (!Files.exists(orgFilePath)) {
            // --- NEW LOGGING ---
            System.out.println("[FileIO.getAllOrgsByUser] 文件不存在: " + orgFilePath.toAbsolutePath());
            return userOrgs;
        }

        List<String> lines = Files.readAllLines(orgFilePath);
        // --- NEW LOGGING ---
        System.out.println("[FileIO.getAllOrgsByUser] 文件存在，共 " + lines.size() + " 行。");

        for (String line : lines) {
            // --- NEW LOGGING ---
            System.out.println("[FileIO.getAllOrgsByUser] 正在处理行: " + line);
            String[] parts = line.split("\\|");
            if (parts.length >= 4) { // 新格式: orgId|parentGroupId|orgName|members
                List<String> members = Arrays.asList(parts[3].split(","));
                // --- NEW LOGGING ---
                System.out.println("[FileIO.getAllOrgsByUser] 解析出的成员: " + members);

                if (members.contains(username)) {
                    // --- NEW LOGGING ---
                    System.out.println("[FileIO.getAllOrgsByUser] 成功：在此小组中找到用户 '" + username + "'。");
                    try {
                        Org_info org = new Org_info();
                        org.setOrg_id(Integer.parseInt(parts[0]));
                        org.setGroup_id(Integer.parseInt(parts[1]));
                        org.setOrg_name(parts[2]);
                        org.setMembers(new ArrayList<>(members));
                        userOrgs.add(org);
                    } catch (NumberFormatException e) {
                        System.err.println("[FileIO.getAllOrgsByUser] 警告：无法解析行内数字: " + line);
                    }
                } else {
                    // --- NEW LOGGING ---
                    System.out.println("[FileIO.getAllOrgsByUser] 信息：用户 '" + username + "' 不在此小组成员列表中。");
                }
            } else {
                // --- NEW LOGGING ---
                System.err.println("[FileIO.getAllOrgsByUser] 警告：跳过格式错误的行 (字段数少于4): " + line);
            }
        }
        // --- NEW LOGGING ---
        System.out.println("[FileIO.getAllOrgsByUser] 处理完毕。为用户 '" + username + "' 找到 " + userOrgs.size() + " 个小组。");
        return userOrgs;
    }

    /**
     * 获取指定小组的完整信息
     * @param orgId 小组ID
     * @return Org_info 对象, 如果未找到则为 null
     * @throws IOException
     */
    public Org_info getOrgInfo(int orgId) throws IOException {
        Path orgFilePath = this.groupFilePath; // 假设此实例用于orgs.dat
        if (!Files.exists(orgFilePath)) return null;

        return Files.lines(orgFilePath)
                .filter(line -> line.startsWith(orgId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) { // 新格式
                        try {
                            Org_info org = new Org_info();
                            org.setOrg_id(Integer.parseInt(parts[0]));
                            org.setGroup_id(Integer.parseInt(parts[1]));
                            org.setOrg_name(parts[2]);
                            org.setMembers(new ArrayList<>(Arrays.asList(parts[3].split(","))));
                            return org;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    /**
     * 获取指定小组的成员列表 (新方法，用于 orgs.dat)
     * @param orgId 小组ID
     * @return 成员列表
     * @throws IOException
     */
    public ArrayList<String> getOrgMembers(int orgId) throws IOException {
        Path orgFilePath = this.groupFilePath; // 假设此实例用于orgs.dat
        if (!Files.exists(orgFilePath)) return null;

        return Files.lines(orgFilePath)
                .filter(line -> line.startsWith(orgId + "|"))
                .findFirst()
                .map(line -> {
                    String[] parts = line.split("\\|");
                    if (parts.length < 4 || parts[3].isEmpty()) {
                        return new ArrayList<String>();
                    }
                    return new ArrayList<>(Arrays.asList(parts[3].split(",")));
                })
                .orElse(null);
    }

    /**
     * 管理小组的成员（添加/删除），适配新文件格式
     * @param orgId 小组ID
     * @param addUsers 要添加的用户列表
     * @param removeUsers 要删除的用户列表
     * @throws IOException
     */
    public void manageOrgMembers(int orgId, ArrayList<String> addUsers, ArrayList<String> removeUsers) throws IOException {
        Path orgFilePath = this.groupFilePath;
        List<String> orgs = Files.exists(orgFilePath) ?
                Files.readAllLines(orgFilePath) :
                new ArrayList<>();

        boolean found = false;
        for (int i = 0; i < orgs.size(); i++) {
            String line = orgs.get(i);
            if (line.startsWith(orgId + "|")) {
                found = true;
                String[] parts = line.split("\\|");
                // 保持 parentGroupId 和 orgName
                String parentGroupIdStr = parts[1];
                String orgName = parts[2];
                ArrayList<String> members = (parts.length < 4 || parts[3].isEmpty()) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(parts[3].split(",")));

                // 添加新成员 (去重)
                if (addUsers != null) {
                    for (String user : addUsers) {
                        if (!members.contains(user)) {
                            members.add(user);
                        }
                    }
                }
                // 移除成员
                if (removeUsers != null) {
                    members.removeAll(removeUsers);
                }

                // 更新行记录
                orgs.set(i, orgId + "|" + parentGroupIdStr + "|" + orgName + "|" + String.join(",", members));
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("小组 " + orgId + " 不存在，无法修改成员。");
        }

        Files.write(orgFilePath, orgs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    // endregion
}

