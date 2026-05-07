# 需求文档：对话会话分组功能

## 简介

为 AI 聊天应用添加类似 ChatGPT 的对话会话（Conversation Session）分组功能。当前系统中所有聊天消息扁平存储在 `chat_messages` 表中，没有会话分组概念。本功能将引入会话实体，使消息按会话归组，侧边栏按会话列表展示，用户可在不同会话间切换，并支持创建新会话。

## 术语表

- **Conversation_Session**：一次完整的对话会话，包含一组按时间排序的聊天消息。每个会话有唯一标识、标题和创建时间。
- **Chat_Message**：一条聊天消息记录，包含用户消息和 AI 回复，归属于某个 Conversation_Session。
- **Session_List**：侧边栏中按时间倒序排列的会话列表，每项显示会话标题和时间。
- **Chat_Area**：主内容区域，用于展示当前选中会话的消息流。
- **Session_Title**：会话标题，默认取该会话中第一条用户消息的前 30 个字符作为摘要。
- **Active_Session**：当前用户正在查看和交互的会话。
- **Frontend_App**：基于纯 HTML/CSS/JS 的单页前端应用（index.html）。
- **Backend_API**：基于 Spring Boot 的后端 REST API 服务。

## 需求

### 需求 1：会话实体与数据模型

**用户故事：** 作为开发者，我希望系统有一个独立的会话数据模型，以便将聊天消息按会话分组存储。

#### 验收标准

1. THE Backend_API SHALL 提供一个 Conversation_Session 实体，包含以下字段：唯一标识（id）、会话标题（title）、创建时间（createTime）、最后更新时间（updateTime）。
2. THE Chat_Message 实体 SHALL 包含一个指向 Conversation_Session 的外键字段（sessionId），用于标识该消息所属的会话。
3. WHEN Conversation_Session 被创建时，THE Backend_API SHALL 将 createTime 和 updateTime 设置为当前时间。
4. WHEN 一条新的 Chat_Message 被保存到某个 Conversation_Session 时，THE Backend_API SHALL 更新该 Conversation_Session 的 updateTime 为当前时间。
5. WHEN Conversation_Session 的第一条 Chat_Message 被保存时，THE Backend_API SHALL 将该会话的 title 设置为该用户消息的前 30 个字符。

### 需求 2：创建新会话

**用户故事：** 作为用户，我希望点击"新对话"按钮时能创建一个全新的会话，以便开始一个独立的对话主题。

#### 验收标准

1. WHEN 用户点击"新对话"按钮时，THE Frontend_App SHALL 调用后端接口创建一个新的 Conversation_Session。
2. WHEN 新的 Conversation_Session 创建成功时，THE Frontend_App SHALL 清空 Chat_Area 中的消息，显示空状态提示，并将新会话设为 Active_Session。
3. WHEN 新的 Conversation_Session 创建成功时，THE Backend_API SHALL 返回新创建的 Conversation_Session 对象（包含 id、title、createTime、updateTime）。
4. THE Backend_API SHALL 提供一个 POST /ai/sessions 接口用于创建新的 Conversation_Session。

### 需求 3：在会话中发送消息

**用户故事：** 作为用户，我希望在当前会话中发送消息时，消息能正确归属到该会话，以便保持对话的连贯性。

#### 验收标准

1. WHEN 用户在 Chat_Area 中发送消息时，THE Frontend_App SHALL 将 Active_Session 的 sessionId 包含在请求中发送给 Backend_API。
2. WHEN Backend_API 收到带有 sessionId 的聊天请求时，THE Backend_API SHALL 将该 Chat_Message 关联到对应的 Conversation_Session。
3. IF 用户在没有 Active_Session 的情况下发送消息，THEN THE Frontend_App SHALL 先自动创建一个新的 Conversation_Session，再将消息发送到该会话中。
4. THE Backend_API SHALL 修改 POST /ai/chat 接口，使其接受 sessionId 参数，并将消息保存到对应的 Conversation_Session 中。
5. WHEN 消息发送成功后，THE Backend_API SHALL 返回包含 Chat_Message 和所属 sessionId 的响应。

### 需求 4：侧边栏会话列表展示

**用户故事：** 作为用户，我希望侧边栏按会话分组显示历史记录，以便快速浏览和定位不同的对话主题。

#### 验收标准

1. THE Session_List SHALL 按 updateTime 倒序排列显示所有 Conversation_Session。
2. THE Session_List 中每个会话项 SHALL 显示 Session_Title 和格式化的时间信息。
3. THE Backend_API SHALL 提供一个 GET /ai/sessions 接口，返回按 updateTime 倒序排列的所有 Conversation_Session 列表。
4. WHEN 新消息被发送到某个 Conversation_Session 后，THE Frontend_App SHALL 刷新 Session_List 以反映最新的排序和标题。
5. WHEN 系统中没有任何 Conversation_Session 时，THE Session_List SHALL 显示"暂无历史记录"的空状态提示。

### 需求 5：切换会话

**用户故事：** 作为用户，我希望点击侧边栏中的某个会话时，能加载该会话的所有消息到聊天主区域，以便查看和继续之前的对话。

#### 验收标准

1. WHEN 用户点击 Session_List 中的某个会话项时，THE Frontend_App SHALL 将该会话设为 Active_Session，并从 Backend_API 加载该会话的所有 Chat_Message。
2. WHEN 会话消息加载完成后，THE Chat_Area SHALL 按 createTime 升序显示该会话的所有消息。
3. THE Backend_API SHALL 提供一个 GET /ai/sessions/{sessionId}/messages 接口，返回指定会话的所有 Chat_Message，按 createTime 升序排列。
4. WHEN 用户切换到另一个会话时，THE Chat_Area SHALL 清空当前显示的消息，并替换为新选中会话的消息。
5. WHEN 用户切换会话时，THE Session_List SHALL 高亮显示当前 Active_Session 对应的会话项。

### 需求 6：页面刷新恢复会话

**用户故事：** 作为用户，我希望页面刷新后能自动恢复到最近一个会话，以便不丢失当前的对话上下文。

#### 验收标准

1. WHEN 页面加载时，THE Frontend_App SHALL 从 Backend_API 获取最近更新的 Conversation_Session（updateTime 最大的会话）。
2. WHEN 最近的 Conversation_Session 获取成功后，THE Frontend_App SHALL 将其设为 Active_Session，并加载该会话的所有消息到 Chat_Area。
3. IF 系统中没有任何 Conversation_Session，THEN THE Frontend_App SHALL 显示空状态提示，等待用户创建新会话或发送第一条消息。
4. WHEN 页面加载时，THE Frontend_App SHALL 同时加载 Session_List 并高亮显示当前恢复的 Active_Session。

### 需求 7：历史数据兼容

**用户故事：** 作为开发者，我希望已有的聊天消息数据能平滑迁移到新的会话模型中，以便不丢失历史数据。

#### 验收标准

1. WHEN 数据库结构升级时，THE Backend_API SHALL 为 Chat_Message 表的 sessionId 字段设置可为空（nullable），以兼容已有的无会话消息数据。
2. THE Backend_API SHALL 确保已有的无 sessionId 的 Chat_Message 记录在系统升级后仍可正常查询。
3. WHEN 前端加载 Session_List 时，THE Frontend_App SHALL 仅显示有关联 Conversation_Session 的会话，不显示无会话归属的历史消息。
