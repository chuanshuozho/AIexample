# AI 智能助手 — Code Wiki

## 1. 项目概述

本项目是一个基于 Spring Boot 的 AI 智能客服聊天应用，集成了 DeepSeek 大语言模型 API，提供服装领域的智能问答服务。用户通过 Web 前端界面发送问题，后端调用 LLM 获取回复，并将对话记录持久化到 MySQL 数据库中。

**技术栈：**
- Java 8 + Spring Boot 2.7.18
- Spring Data JPA（数据持久化）
- OkHttp 4.12.0（HTTP 客户端，调用 LLM API）
- Jackson（JSON 序列化/反序列化）
- MySQL（数据库）
- Maven（构建工具）
- 前端：原生 HTML/CSS/JavaScript + marked.js（Markdown 渲染）+ highlight.js（代码高亮）

---

## 2. 项目结构

```
Ai_example/AI_example/
├── pom.xml                          # Maven 项目配置与依赖管理
├── mvnw / mvnw.cmd                  # Maven Wrapper 脚本
├── src/
│   ├── main/
│   │   ├── java/com/example/ai/
│   │   │   ├── AiExampleApplication.java      # Spring Boot 启动类
│   │   │   ├── config/
│   │   │   │   └── LlmConfig.java             # LLM API 配置（密钥、地址）
│   │   │   ├── controller/
│   │   │   │   └── AiController.java          # REST 控制器（聊天 & 历史记录接口）
│   │   │   ├── dto/
│   │   │   │   ├── AiRequest.java             # 请求 DTO
│   │   │   │   └── AiResponse.java            # 响应 DTO
│   │   │   ├── entity/
│   │   │   │   └── ChatMessage.java           # JPA 实体（聊天消息）
│   │   │   ├── repository/
│   │   │   │   └── ChatMessageRepository.java # 数据访问层
│   │   │   └── service/
│   │   │       ├── AiService.java             # LLM 调用服务
│   │   │       └── ChatMessageService.java    # 消息持久化服务
│   │   └── resources/
│   │       ├── application.properties         # 应用配置
│   │       └── static/
│   │           └── index.html                 # 前端页面（单页应用）
│   └── test/
│       └── java/com/example/ai/
│           └── AiExampleApplicationTests.java # Spring Boot 上下文加载测试
└── target/                                    # 编译输出目录
```

---

## 3. 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (index.html)                      │
│         HTML/CSS/JS · marked.js · highlight.js           │
└──────────────┬──────────────────────┬───────────────────┘
               │ POST /ai/chat        │ GET /ai/history
               ▼                      ▼
┌─────────────────────────────────────────────────────────┐
│                  AiController (REST)                     │
│         聊天接口 · 历史记录接口 · 首页转发                  │
└──────┬──────────────────────────────┬───────────────────┘
       │                              │
       ▼                              ▼
┌──────────────────┐    ┌──────────────────────────┐
│    AiService      │    │   ChatMessageService      │
│  调用 DeepSeek API │    │   消息保存 & 查询           │
└──────┬───────────┘    └──────────┬───────────────┘
       │                           │
       ▼                           ▼
┌──────────────────┐    ┌──────────────────────────┐
│  DeepSeek LLM    │    │  ChatMessageRepository    │
│  (外部 API)       │    │  (Spring Data JPA)        │
└──────────────────┘    └──────────┬───────────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │     MySQL 数据库       │
                        │   表: chat_messages    │
                        └──────────────────────┘
```

**请求流程：**
1. 用户在前端输入问题，通过 `POST /ai/chat` 发送到后端
2. `AiController` 接收请求，调用 `AiService.chat()` 向 DeepSeek API 发送请求
3. 获取 AI 回复后，调用 `ChatMessageService.saveMessage()` 将对话保存到数据库
4. 返回 `AiResponse` 给前端展示
5. 前端通过 `GET /ai/history` 加载历史对话记录

---

## 4. 模块职责详解

### 4.1 启动类 — `AiExampleApplication`

| 项目 | 说明 |
|------|------|
| 包路径 | `com.example.ai` |
| 注解 | `@SpringBootApplication` |
| 职责 | Spring Boot 应用入口，启动内嵌 Tomcat 服务器 |

### 4.2 配置层 — `config`

#### `LlmConfig`

常量配置类，存储 DeepSeek API 的访问凭证和端点地址。

| 常量 | 说明 |
|------|------|
| `API_KEY` | DeepSeek API 密钥 |
| `API_URL` | API 端点：`https://api.deepseek.com/v1/chat/completions` |

> ⚠️ **注意：** API Key 以硬编码方式存储在源码中，生产环境应改为环境变量或配置中心管理。

### 4.3 控制器层 — `controller`

#### `AiController`

| 注解 | `@Controller` |
|------|------|
| 依赖注入 | `AiService`、`ChatMessageService`（构造器注入） |

**接口列表：**

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET /` | 首页 | 转发到 `index.html` 静态页面 |
| `POST /ai/chat` | 聊天 | 接收 `AiRequest`，调用 LLM，保存消息，返回 `AiResponse` |
| `GET /ai/history` | 历史记录 | 返回所有聊天记录（按时间升序） |

**异常处理：** `chat()` 方法内部 try-catch，失败时返回 `"调用失败: {错误信息}"`。

### 4.4 服务层 — `service`

#### `AiService`

核心 LLM 调用服务，负责与 DeepSeek API 通信。

| 项目 | 说明 |
|------|------|
| HTTP 客户端 | OkHttp，超时配置：连接 60s / 读取 120s / 写入 60s |
| 模型 | `deepseek-chat` |
| 系统提示词 | `"你是一个服装专业客服助手，请简洁回答"` |
| temperature | 0.7 |

**`chat(String userMessage)` 方法：**
1. 手动拼接 JSON 请求体（包含 system 和 user 两条消息）
2. 对用户输入进行转义处理（双引号、换行符）
3. 通过 OkHttp 发送 POST 请求，携带 Bearer Token 认证
4. 解析响应 JSON，提取 `choices[0].message.content` 作为回复

#### `ChatMessageService`

消息持久化服务。

| 方法 | 说明 |
|------|------|
| `saveMessage(String userMessage, String assistantReply)` | 创建 `ChatMessage` 实体并保存到数据库 |
| `getAllMessages()` | 查询所有消息，按创建时间升序排列 |

### 4.5 数据传输对象 — `dto`

#### `AiRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| `message` | `String` | 用户发送的消息内容 |

#### `AiResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| `reply` | `String` | AI 回复内容 |

### 4.6 实体层 — `entity`

#### `ChatMessage`

JPA 实体，映射到 `chat_messages` 表。

| 字段 | 类型 | 数据库约束 | 说明 |
|------|------|-----------|------|
| `id` | `Long` | 主键，自增 | 消息 ID |
| `userMessage` | `String` | `NOT NULL, TEXT` | 用户消息 |
| `assistantReply` | `String` | `NOT NULL, TEXT` | AI 回复 |
| `createTime` | `LocalDateTime` | `NOT NULL` | 创建时间（`@PrePersist` 自动填充） |

### 4.7 数据访问层 — `repository`

#### `ChatMessageRepository`

继承 `JpaRepository<ChatMessage, Long>`，提供标准 CRUD 操作。

| 自定义方法 | 说明 |
|-----------|------|
| `findAllByOrderByCreateTimeAsc()` | 按创建时间升序查询所有消息 |

### 4.8 前端 — `index.html`

单页应用，Apple 风格 UI 设计。

**功能模块：**
- 聊天主界面：消息输入、发送、展示（支持 Markdown 渲染和代码高亮）
- 侧边栏：历史对话列表，点击可回顾历史对话
- 响应式布局：移动端侧边栏可折叠
- 加载动画：等待 AI 回复时显示跳动圆点

**外部依赖（CDN）：**
- `marked.js` — Markdown 转 HTML
- `highlight.js` — 代码语法高亮（支持 Java、JavaScript、Python、CSS、SQL、Bash、JSON）

---

## 5. 依赖关系

### Maven 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `spring-boot-starter` | 2.7.18 (parent) | Spring Boot 核心 |
| `spring-boot-starter-web` | — | Web MVC、内嵌 Tomcat |
| `spring-boot-starter-data-jpa` | — | JPA / Hibernate ORM |
| `mysql-connector-java` | — | MySQL JDBC 驱动 |
| `okhttp` | 4.12.0 | HTTP 客户端（调用 LLM API） |
| `jackson-databind` | — | JSON 处理 |
| `spring-boot-starter-test` | — | 测试框架 |

### 类依赖关系

```
AiController
├── AiService          → LlmConfig（API 配置）
│                      → OkHttp（HTTP 调用）
│                      → Jackson ObjectMapper（JSON 解析）
└── ChatMessageService → ChatMessageRepository → ChatMessage（JPA 实体）
```

---

## 6. 数据库设计

**数据库名：** `ai_chat`

**表：** `chat_messages`

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键 |
| `user_message` | TEXT | NOT NULL | 用户消息 |
| `assistant_reply` | TEXT | NOT NULL | AI 回复 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |

> JPA 配置 `ddl-auto=update`，表结构会自动创建/更新。

---

## 7. API 接口文档

### POST `/ai/chat`

发送消息并获取 AI 回复。

**请求体：**
```json
{
  "message": "这件衣服有什么面料？"
}
```

**成功响应：**
```json
{
  "reply": "这件衣服采用了100%纯棉面料，透气舒适..."
}
```

**失败响应：**
```json
{
  "reply": "调用失败: LLM调用失败: ..."
}
```

### GET `/ai/history`

获取所有历史聊天记录（按时间升序）。

**响应：**
```json
[
  {
    "id": 1,
    "userMessage": "你好",
    "assistantReply": "您好！请问有什么可以帮您的？",
    "createTime": "2025-01-01T10:00:00"
  }
]
```

### GET `/`

返回前端页面 `index.html`。

---

## 8. 配置说明

`application.properties` 关键配置：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `server.port` | `8081` | 应用端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/ai_chat` | MySQL 连接地址 |
| `spring.datasource.username` | `root` | 数据库用户名 |
| `spring.datasource.driver-class-name` | `com.mysql.cj.jdbc.Driver` | MySQL 8 驱动 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动更新表结构 |
| `spring.jpa.show-sql` | `true` | 控制台打印 SQL |

---

## 9. 项目运行方式

### 前置条件

1. **Java 8+** 已安装
2. **MySQL** 已安装并运行
3. 创建数据库：
   ```sql
   CREATE DATABASE ai_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. 根据实际情况修改 `application.properties` 中的数据库连接信息

### 启动步骤

```bash
# 进入项目目录
cd Ai_example/AI_example

# 使用 Maven Wrapper 启动（无需预装 Maven）
./mvnw spring-boot:run

# 或者先打包再运行
./mvnw clean package
java -jar target/AI_example-0.0.1-SNAPSHOT.jar
```

### 访问应用

启动后浏览器访问：`http://localhost:8081`

---

## 10. 已知问题与改进建议

| 类别 | 问题 | 建议 |
|------|------|------|
| 安全 | API Key 硬编码在源码中 | 使用环境变量或 Spring 配置加密 |
| 安全 | 数据库密码明文存储 | 使用 Jasypt 加密或外部配置 |
| 健壮性 | JSON 请求体通过字符串拼接构建 | 使用 Jackson ObjectMapper 序列化 |
| 功能 | 不支持多轮对话上下文 | 维护会话历史，发送多条 messages |
| 功能 | 无用户认证机制 | 添加 Spring Security |
| 性能 | 历史记录无分页 | 添加分页查询 |
| 可观测性 | 无日志记录 | 添加 SLF4J 日志 |
