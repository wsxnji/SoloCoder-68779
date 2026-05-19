# 即时聊天室

一个基于 React + Spring Boot + WebSocket + SQLite 的即时聊天项目。

## 项目结构

```
.
├── README.md                    # 项目说明文档
├── backend/                     # Spring Boot 后端
│   ├── pom.xml                  # Maven 依赖配置
│   ├── chat.db                  # SQLite 数据库文件
│   └── src/main/
│       ├── java/com/chat/
│       │   ├── ChatApplication.java       # 启动类
│       │   ├── config/
│       │   │   └── CorsConfig.java        # CORS 跨域配置
│       │   ├── controller/
│       │   │   └── RoomController.java    # REST API 控制器
│       │   ├── dto/
│       │   │   └── ChatMessage.java       # 消息数据传输对象
│       │   ├── entity/
│       │   │   ├── Room.java              # 房间实体
│       │   │   └── User.java              # 用户实体
│       │   ├── repository/
│       │   │   ├── RoomRepository.java    # 房间数据访问层
│       │   │   └── UserRepository.java    # 用户数据访问层
│       │   ├── service/
│       │   │   └── RoomService.java       # 业务逻辑层
│       │   └── websocket/
│       │       ├── WebSocketConfig.java        # WebSocket 配置
│       │       ├── ChatHandshakeInterceptor.java  # 握手拦截器
│       │       └── ChatWebSocketHandler.java    # WebSocket 消息处理器
│       └── resources/
│           └── application.properties     # 应用配置
└── frontend/                    # React 前端
    ├── index.html               # HTML 入口
    ├── package.json             # npm 依赖配置
    ├── vite.config.js           # Vite 配置
    └── src/
        ├── main.jsx             # 应用入口
        ├── App.jsx              # 应用组件
        ├── index.css            # 全局样式
        └── pages/
            ├── Login.jsx        # 登录页面（创建/加入房间）
            └── ChatRoom.jsx     # 聊天室页面
```

## 技术栈

- **前端**: React 18 + Vite 5 + React Router 6
- **后端**: Spring Boot 3.2 + Java 17
- **实时通信**: WebSocket
- **数据库**: SQLite

## 功能特性

1. **免注册加入**
   - 创建房间：自动生成6位唯一房间号
   - 加入房间：输入房间号和匿名昵称

2. **房间状态校验**
   - 前端通过 REST API 验证房间号是否存在
   - WebSocket 连接时二次校验

3. **实时在线用户列表**
   - 右侧边栏显示在线人数和用户列表
   - 用户加入/离开时实时更新

4. **文本消息同步**
   - 实时消息广播，秒级接收
   - 消息格式：`[时间] 昵称: 消息内容`

5. **消息气泡区分**
   - 自己发送的消息居右（蓝色渐变）
   - 他人发送的消息居左（白色）

6. **系统广播通知**
   - 用户加入/离开时顶部显示通知
   - 3秒后自动消失

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+

### 启动后端服务

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动。

### 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动。

### 使用方式

1. 打开浏览器访问 http://localhost:3000
2. 选择「创建房间」生成新房间，或选择「加入房间」输入已有房间号
3. 输入匿名昵称后进入聊天室
4. 在聊天室中可以实时发送和接收消息

## API 接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/room/create` | POST | 创建新房间 |
| `/api/room/check/{roomNumber}` | GET | 检查房间是否存在 |
| `/api/room/join` | POST | 加入房间 |
| `/api/room/leave` | POST | 离开房间 |
| `/api/room/users/{roomNumber}` | GET | 获取房间用户列表 |
| `/api/room/count/{roomNumber}` | GET | 获取房间在线人数 |

## WebSocket 端点

- `ws://localhost:8080/ws/chat?roomNumber={roomNumber}&nickname={nickname}&sessionId={sessionId}`

## 消息类型

| 类型 | 说明 |
|------|------|
| `CHAT` | 普通聊天消息 |
| `SYSTEM` | 系统通知消息 |
| `USER_LIST` | 用户列表更新 |
