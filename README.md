# 多人聊天室系统

## 项目简介
本项目是一个基于Java的多人聊天室系统，支持用户注册登录、私聊、群聊、文件传输和语音通话等功能。系统采用客户端/服务器架构，通过Socket进行网络通信，具有良好的扩展性和稳定性。

## 功能特点
- **用户管理**：支持用户注册、登录和登出，实时更新在线用户列表。
- **私聊与群聊**：用户可以进行一对一私聊和多人群聊，发送文本消息。
- **文件传输**：支持发送和接收文件及图片，包含进度显示和断点续传功能。
- **语音通话**：提供一对一语音通话功能，采用P2P架构降低服务器负载。
- **小组聊天**：群内可创建小组，成员可接收多个小组邀请但只能加入一个。
- **消息记录**：在线时自动保存聊天记录，离线时抛弃未接收消息。

## 技术栈
- **编程语言**：Java
- **网络通信**：Socket编程，TCP协议用于信令和文件传输，UDP协议用于语音通话。
- **图形界面**：Java Swing
- **数据存储**：本地文件存储用户数据和聊天记录

## 项目结构
```
src/
├── client/                # 客户端代码
│   ├── controller/        # 控制器
│   ├── handler/           # 消息处理器
│   ├── io/                # 输入输出流
│   ├── model/             # 数据模型
│   └── view/              # 视图界面
├── Server/                # 服务器代码
│   ├── controller/        # 控制器
│   ├── handler/           # 消息处理器
│   ├── model/             # 数据模型
│   └── view/              # 视图界面
└── info/                  # 消息封装类
```

## 运行环境
- **Java版本**：JDK 11 或更高
- **开发工具**：IntelliJ IDEA 或 Eclipse

## 使用方法
1. **启动服务器**：运行`Server.ChatServer`类启动服务器。
2. **启动客户端**：运行`client.Client`类启动客户端，连接到服务器。
3. **用户操作**：注册新账号或使用已有账号登录，进行聊天、文件传输和语音通话等操作。

## 贡献者
- **主要开发者**：[BIMU，Yokumi]

## 联系方式
如有任何问题或建议，请通过[项目Issue页面]联系我们。

## 版权声明
本项目遵循[MIT License](LICENSE).
