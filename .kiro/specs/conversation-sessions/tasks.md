# 实现计划：对话会话分组功能

## 概述

基于设计文档，按增量方式实现对话会话分组功能。从后端数据模型开始，逐步构建 Repository、Service、Controller 层，最后修改前端 JavaScript 逻辑，实现会话的创建、切换、消息归组和页面刷新恢复。

## 任务

- [x] 1. 创建 ConversationSession 实体和修改 ChatMessage 实体
  - [x] 1.1 创建 ConversationSession 实体类
    - 在 `Ai_example/AI_example/src/main/java/com/example/ai/entity/` 下创建 `ConversationSession.java`
    - 包含字段：`id`（Long, 自增主键）、`title`（String, 最大100字符, nullable）、`createTime`（LocalDateTime, NOT NULL）、`updateTime`（LocalDateTime, NOT NULL）
    - 使用 `@PrePersist` 在创建时自动设置 `createTime` 和 `updateTime` 为当前时间
    - 映射到 `conversation_sessions` 表
    - _需求：1.1, 1.3_

  - [x] 1.2 修改 ChatMessage 实体，添加 sessionId 字段
    - 在 `ChatMessage.java` 中新增 `sessionId` 字段（Long 类型，nullable），映射到 `session_id` 列
    - 保留现有 `conversationId` 字段不变，确保历史数据兼容
    - _需求：1.2, 7.1_

- [x] 2. 创建和修改 Repository 层
  - [x] 2.1 创建 ConversationSessionRepository
    - 在 `Ai_example/AI_example/src/main/java/com/example/ai/repository/` 下创建 `ConversationSessionRepository.java`
    - 继承 `JpaRepository<ConversationSession, Long>`
    - 添加方法：`findAllByOrderByUpdateTimeDesc()`（返回按 updateTime 倒序排列的所有会话）
    - 添加方法：`findTopByOrderByUpdateTimeDesc()`（返回最近更新的会话，用于页面刷新恢复）
    - _需求：4.3, 6.1_

  - [x] 2.2 修改 ChatMessageRepository，添加按 sessionId 查询方法
    - 在 `ChatMessageRepository.java` 中添加 `findBySessionIdOrderByCreateTimeAsc(Long sessionId)` 方法
    - _需求：5.3_

- [x] 3. 创建 ConversationSessionService 并修改 ChatMessageService
  - [x] 3.1 创建 ConversationSessionService
    - 在 `Ai_example/AI_example/src/main/java/com/example/ai/service/` 下创建 `ConversationSessionService.java`
    - 实现 `createSession()` 方法：创建空会话，设置时间戳，返回保存后的实体
    - 实现 `getAllSessions()` 方法：调用 Repository 返回按 updateTime 倒序排列的会话列表
    - 实现 `getLatestSession()` 方法：返回最近更新的会话（Optional）
    - 实现 `updateSessionOnNewMessage(Long sessionId, String userMessage)` 方法：更新 updateTime，如果会话 title 为空则取 userMessage 前 min(30, 长度) 个字符作为标题
    - _需求：1.3, 1.4, 1.5, 2.3, 2.4, 4.3, 6.1_

  - [x] 3.2 修改 ChatMessageService，支持 sessionId 参数
    - 修改 `saveMessage` 方法，新增重载版本 `saveMessage(Long sessionId, String userMessage, String assistantReply)`，在保存消息时设置 sessionId
    - 新增 `getMessagesBySessionId(Long sessionId)` 方法，调用 Repository 按 sessionId 查询消息
    - 保留原有 `saveMessage(String, String)` 方法以保持向后兼容
    - _需求：3.2, 3.4, 5.3_

- [x] 4. 检查点 - 确保后端数据层编译通过
  - 确保所有实体、Repository、Service 代码编译无误，如有问题请向用户确认。

- [x] 5. 修改 DTO 和 AiController
  - [x] 5.1 修改 AiRequest 和 AiResponse DTO
    - 在 `AiRequest.java` 中添加 `sessionId` 字段（Long 类型）及 getter/setter
    - 在 `AiResponse.java` 中添加 `sessionId` 字段（Long 类型）及 getter/setter，修改构造函数支持 sessionId
    - _需求：3.4, 3.5_

  - [x] 5.2 修改 AiController，添加会话相关接口
    - 注入 `ConversationSessionService`
    - 新增 `POST /ai/sessions` 接口：调用 `createSession()` 返回新会话 JSON
    - 新增 `GET /ai/sessions` 接口：调用 `getAllSessions()` 返回会话列表 JSON
    - 新增 `GET /ai/sessions/{sessionId}/messages` 接口：调用 `getMessagesBySessionId()` 返回消息列表 JSON
    - 修改 `POST /ai/chat` 接口：从 AiRequest 中获取 sessionId，调用带 sessionId 的 `saveMessage` 方法，调用 `updateSessionOnNewMessage` 更新会话信息，返回包含 sessionId 的 AiResponse
    - 处理 sessionId 对应会话不存在的情况，返回 404 错误
    - _需求：2.4, 3.4, 3.5, 4.3, 5.3_

- [x] 6. 检查点 - 确保后端所有接口编译通过
  - 确保所有后端代码编译无误，如有问题请向用户确认。

- [x] 7. 修改前端 JavaScript 逻辑
  - [x] 7.1 添加会话状态管理和核心函数
    - 添加 `currentSessionId` 状态变量（初始为 null）
    - 添加 `sessions` 数组变量存储会话列表
    - 实现 `loadSessions()` 函数：调用 `GET /ai/sessions` 获取会话列表，调用 `renderSessionList()` 渲染侧边栏，如果有会话则自动选中最近的会话（设置 `currentSessionId` 并加载消息）
    - 实现 `createNewSession()` 函数：调用 `POST /ai/sessions` 创建新会话，设置 `currentSessionId`，清空聊天区域显示空状态，刷新会话列表
    - 实现 `switchSession(sessionId)` 函数：设置 `currentSessionId`，调用 `GET /ai/sessions/{sessionId}/messages` 加载消息，渲染到聊天区域，更新侧边栏高亮状态
    - 实现 `renderSessionList(sessions)` 函数：替换现有 `renderHistoryList()`，按会话渲染侧边栏列表项，每项显示 title 和格式化时间，当前活跃会话添加 `active` 样式类
    - _需求：4.1, 4.2, 4.4, 4.5, 5.1, 5.2, 5.4, 5.5, 6.1, 6.2, 6.4_

  - [x] 7.2 修改 sendMessage() 和页面加载逻辑
    - 修改 `sendMessage()` 函数：如果 `currentSessionId` 为 null，先调用 `createNewSession()` 获取新会话 ID；在 fetch 请求体中添加 `sessionId: currentSessionId`；发送成功后刷新会话列表
    - 修改 `clearChat()` 函数为调用 `createNewSession()`，实现"新对话"按钮创建新会话的功能
    - 修改页面加载逻辑：将 `loadChatHistory()` 替换为 `loadSessions()`，实现页面刷新时自动恢复最近会话
    - 处理系统中无会话时的空状态显示（侧边栏显示"暂无历史记录"，聊天区域显示空状态提示）
    - _需求：2.1, 2.2, 3.1, 3.3, 6.1, 6.2, 6.3, 6.4, 7.3_

- [x] 8. 最终检查点 - 确保所有代码完整且可运行
  - 确保所有后端和前端代码编译/语法无误，所有接口正确连接，如有问题请向用户确认。

## 备注

- 每个任务引用了具体的需求编号以确保可追溯性
- 检查点任务用于确保增量开发过程中的代码质量
- 前端保持纯 HTML/CSS/JS 技术栈，不引入额外框架
- `sessionId` 字段设为 nullable 以兼容历史数据（需求 7.1）
- 会话标题自动取第一条用户消息的前 30 个字符（需求 1.5）
