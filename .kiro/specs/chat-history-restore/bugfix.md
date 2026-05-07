# Bugfix Requirements Document

## Introduction

用户报告聊天历史记录已正确保存到数据库，但刷新页面或重新打开后，聊天主区域不会恢复之前的对话内容。侧边栏能正确显示历史记录列表，但点击某条记录时只加载该单条消息（一问一答），而非完整的对话上下文。此 bug 导致用户每次刷新页面都会丢失聊天主区域的所有对话内容，严重影响使用体验。

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN 页面加载（刷新或重新打开）时，`loadChatHistory()` 被调用 THEN 系统只将历史记录渲染到侧边栏列表（`renderHistoryList()`），聊天主区域保持空状态（显示 `emptyState`），不恢复任何历史对话消息

1.2 WHEN 用户点击侧边栏中的某条历史记录时，`loadConversation(item)` 被调用 THEN 系统只加载该单条记录（一条用户消息 + 一条 AI 回复），而非该条记录所属的完整对话上下文

1.3 WHEN 数据库中存在多条聊天记录且页面被刷新 THEN 聊天主区域显示为空白初始状态，用户无法看到之前的任何对话内容

### Expected Behavior (Correct)

2.1 WHEN 页面加载（刷新或重新打开）时 THEN 系统 SHALL 从 `/ai/history` 获取所有历史消息，并将它们按时间顺序渲染到聊天主区域，恢复完整的对话内容

2.2 WHEN 用户点击侧边栏中的某条历史记录时 THEN 系统 SHALL 加载数据库中所有的聊天记录并渲染到聊天主区域（因为当前数据库没有会话分组概念，所有消息属于同一个对话流）

2.3 WHEN 数据库中存在多条聊天记录且页面被刷新 THEN 系统 SHALL 在聊天主区域显示所有历史对话消息（用户消息和 AI 回复交替显示），并隐藏空状态提示

### Unchanged Behavior (Regression Prevention)

3.1 WHEN 用户发送新消息时 THEN 系统 SHALL CONTINUE TO 将消息通过 `/ai/chat` 发送到后端，保存到数据库，并在聊天主区域显示用户消息和 AI 回复

3.2 WHEN 用户点击"新对话"按钮时 THEN 系统 SHALL CONTINUE TO 清空聊天主区域并显示空状态提示

3.3 WHEN 侧边栏历史记录列表加载时 THEN 系统 SHALL CONTINUE TO 按时间倒序显示所有历史记录摘要

3.4 WHEN 数据库中没有任何聊天记录且页面加载时 THEN 系统 SHALL CONTINUE TO 在聊天主区域显示空状态提示（"开始对话，体验智能客服"）

3.5 WHEN 后端 `/ai/history` 接口被调用时 THEN 系统 SHALL CONTINUE TO 返回按 `createTime` 升序排列的所有聊天记录
