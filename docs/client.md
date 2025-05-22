# Java网络聊天室客户端设计文档

## 1. 架构设计

### 1.1 MVC架构

客户端采用经典的MVC架构模式，将应用程序分为三个核心部分：

（代补充图片）

#### 1.1.1 模型(Model)

模型层负责管理应用程序的数据、业务规则和逻辑。在本项目的客户端中，模型主要包括：

- **ClientModel**: 核心数据模型，管理客户端的状态和数据
- **用户相关模型**: 存储用户信息、在线用户列表以及用户加入的群聊信息（等待实现）等
- **消息模型**: 不同类型消息的数据结构

#### 2.1.2 视图(View)

视图层负责用户界面的展示，即前端UI，包括：

- **LoginView**: 登录和注册界面
- **ChatView**: 主聊天界面
- **DialogView**: 各类对话框、提示和警告窗口

#### 2.1.3 控制器(Controller)

控制器层处理用户输入，协调模型和视图：

- **LoginController**: 处理用户登录和注册逻辑
- **ChatController**: 管理聊天功能，包括发送消息、创建群组等
- **MessageController**: 处理接收到的各类消息

### 2.2 通信模块

通信模块负责与服务器的网络通信：

- **MessageListener**: 监听服务器消息的线程
- **MessageSender**: 负责向服务器发送消息
- **NetworkManager**: 管理网络连接状态

## 3. 核心类设计

### 3.1 Client类

`Client`是客户端的入口类，负责初始化各组件并协调它们之间的交互：

> 原来的代码直接关闭客户端窗口，用户并没有在服务端下线

```java
public class Client {
    private Socket socket;
    private ClientModel model;
    private LoginController loginController;
    private ChatController chatController;
    
    // 初始化客户端
    // 连接服务器
    // 获取控制器
    // 关闭连接
}
```

### 3.2 ClientModel类

`ClientModel`是核心数据模型，管理客户端的状态：

```java
public class ClientModel {
    private String currentUser;
    private ArrayList<String> onlineUsers;
    private Map<Integer, GroupInfo> groups;
    private List<ChatMessage> messageHistory;
    
    // 数据访问和修改方法
    // 观察者模式支持
}
```

### 3.3 控制器类

控制器类处理用户操作和业务逻辑：

```java
public class ChatController {
    private Socket socket;
    private ClientModel model;
    private ChatView view;
    
    // 发送消息方法
    // 处理用户操作
    // 更新视图
}
```

### 3.4 MessageListener类

`MessageListener`是一个线程类，负责监听和处理服务器消息：

```java
public class MessageListener extends Thread {
    private Socket socket;
    private ClientModel model;
    private LoginController loginController;
    private ChatController chatController;
    
    // 监听消息
    // 分发消息到相应控制器
}
```

## 4. 消息处理流程

### 4.1 发送消息流程

1. 用户在界面输入消息并点击发送
2. 视图将事件传递给控制器
3. 控制器创建消息对象并设置相关属性
4. 控制器调用通信模块发送消息
5. 通信模块将消息序列化并通过Socket发送

### 4.2 接收消息流程

1. MessageListener线程监听Socket输入流
2. 接收到消息后进行反序列化
3. 根据消息类型分发给相应的控制器
4. 控制器更新模型数据
5. 模型通知视图更新界面

## 5. 数据结构设计

### 5.1 消息类型

客户端处理多种类型的消息，每种类型使用单独的类表示：

- **LoginInfo**: 登录相关信息
- **ChatInfo**: 聊天消息
- **GroupInfo**: 群组相关信息
- **RegInfo**: 注册相关信息

### 5.2 消息封装

所有消息都封装在`encap_info`类中：

```java
public class encap_info {
    private int type;  // 消息类型
    private ChatInfo chatInfo;
    private GroupInfo groupInfo;
    private LoginInfo loginInfo;
    private RegInfo regInfo;
}
```

## 6. 用户界面设计

### 6.1 登录界面

### 6.2 聊天主界面
