# Requirements Document

## Introduction

对话蒸馏功能用于在对话历史上下文过高时自动对对话进行总结压缩，以减少 Token 消耗。该功能通过检测 Token 阈值触发条件，保留最近的几轮对话，对较早的对话进行 AI 总结，并将摘要存储在数据库中。这确保了长对话的连续性，同时控制了 API 调用成本。

该功能集是一个透明的后台服务，对用户无感知，集成到现有的对话管理流程中。

## Glossary

- **Conversation_Distillation**: 对话蒸馏，指将对话历史进行总结压缩的过程
- **Session**: 会话，一个完整的对话上下文
- **Token_Threshold**: Token 阈值，触发蒸馏的 Token 数量上限
- **Preserve_Recent_Pairs**: 保留最近对话轮数，不被总结的最近对话数量
- **Summary**: 摘要，对较早对话内容的 AI 生成总结
- **Distilled_History**: 蒸馏后的历史，包含摘要和保留的最近对话
- **ChatMemoryService**: 现有的对话历史管理服务
- **ConversationDistillationService**: 新增的对话蒸馏服务

## Requirements

### Requirement 1: Token Threshold Detection

**User Story:** As a system, I want to detect when conversation history exceeds the token threshold, so that I can trigger distillation automatically.

#### Acceptance Criteria

1. WHEN the total token count of a session's history exceeds the configured threshold THEN the System SHALL mark the session as needing distillation
2. WHEN the total token count is at or below the threshold THEN the System SHALL NOT trigger distillation
3. THE System SHALL calculate token count using the existing ChatMemoryService.countTokens() method
4. THE System SHALL count tokens for both user messages and assistant replies

### Requirement 2: Recent Messages Preservation

**User Story:** As a user, I want the most recent conversation turns preserved without summarization, so that I can maintain immediate context.

#### Acceptance Criteria

1. WHEN distillation is triggered THEN the System SHALL preserve the most recent N pairs of messages (configurable, default 3 pairs)
2. THE preserved messages SHALL remain in their original form and order
3. IF the session has fewer messages than the preserve threshold THEN the System SHALL NOT perform distillation
4. THE System SHALL include both user and assistant messages in the preserved count (1 pair = 1 user + 1 assistant message)

### Requirement 3: Summary Generation

**User Story:** As a system, I want to generate a concise summary of older conversation history, so that key information is preserved while reducing token count.

#### Acceptance Criteria

1. WHEN distillation is triggered THEN the System SHALL call the DeepSeek API to generate a summary of older messages
2. THE summary SHALL preserve key information including: main topics discussed, decisions made, and context necessary for understanding recent messages
3. THE summary token count SHALL be less than the original message token count
4. THE summary token count SHALL not exceed the configured maxSummaryTokens (default 500)
5. IF the API call fails THEN the System SHALL log the error and return the original history without distillation
6. THE System SHALL use the configured model (default: deepseek-chat) for summarization

### Requirement 4: Summary Storage and Retrieval

**User Story:** As a system, I want to store and retrieve conversation summaries, so that I don't need to regenerate summaries for unchanged history.

#### Acceptance Criteria

1. WHEN a summary is generated THEN the System SHALL store it in the conversation_summaries table
2. THE System SHALL store: session_id, summary text, last_message_id, original_token_count, and summary_token_count
3. WHEN retrieving history for distillation THEN the System SHALL first check for an existing valid summary
4. IF an existing summary's last_message_id matches the current last message to summarize THEN the System SHALL use the cached summary
5. IF the conversation has new messages since the last summary THEN the System SHALL regenerate and update the summary
6. THE System SHALL maintain one summary per session (unique constraint on session_id)

### Requirement 5: Distilled History Construction

**User Story:** As a system, I want to construct a distilled history from summary and recent messages, so that I can provide context to the AI while controlling token usage.

#### Acceptance Criteria

1. WHEN distillation completes THEN the System SHALL return a list containing: one synthetic summary message followed by preserved recent messages
2. THE synthetic summary message SHALL have userMessage = "[对话历史摘要]" and assistantReply = summary text
3. THE order of messages SHALL be: summary message first, then recent messages in chronological order
4. THE total token count of distilled history SHALL be less than the original token count

### Requirement 6: Configuration Management

**User Story:** As a system administrator, I want to configure distillation parameters, so that I can adjust behavior based on resource constraints and use cases.

#### Acceptance Criteria

1. THE System SHALL support configuration of: tokenThreshold (default 4000), preserveRecentPairs (default 3), maxSummaryTokens (default 500), enabled (default true), summaryModel (default deepseek-chat)
2. WHERE the enabled flag is set to false THEN the System SHALL skip all distillation logic
3. WHEN configuration values are invalid (negative, zero, etc.) THEN the System SHALL use default values and log a warning
4. THE System SHALL support runtime configuration via application.yml and environment variables

### Requirement 7: Integration with Existing Services

**User Story:** As a developer, I want the distillation service to integrate seamlessly with existing services, so that minimal code changes are required.

#### Acceptance Criteria

1. THE System SHALL provide ConversationDistillationService interface with getHistoryWithDistillation(Long sessionId) method
2. WHEN RAGServiceImpl needs conversation history THEN it SHALL call ConversationDistillationService.getHistoryWithDistillation() instead of ChatMemoryService.getHistory()
3. THE System SHALL maintain backward compatibility with existing ChatMemoryService methods
4. THE System SHALL NOT modify the original ChatMessage records in the database

### Requirement 8: Error Handling and Resilience

**User Story:** As a system, I want graceful error handling for distillation failures, so that conversations are not disrupted.

#### Acceptance Criteria

1. IF the DeepSeek API is unavailable THEN the System SHALL log the error and return the original history without distillation
2. IF the database write for summary fails THEN the System SHALL continue with in-memory summary and log the error
3. IF the sessionId is null or invalid THEN the System SHALL return an empty list for history retrieval
4. THE System SHALL NOT throw unhandled exceptions that would disrupt the conversation flow
5. THE System SHALL log all errors with sufficient context (sessionId, message count, error details)

### Requirement 9: Performance and Efficiency

**User Story:** As a system, I want efficient distillation processing, so that conversation latency is minimized.

#### Acceptance Criteria

1. THE System SHALL use the existing ChatMemoryService.countTokens() method for consistency and efficiency
2. THE System SHALL cache summaries to avoid redundant API calls
3. THE System SHALL only trigger distillation when needed (lazy evaluation), not proactively
4. THE database query for summary lookup SHALL use indexed session_id column

### Requirement 10: Data Model Extension

**User Story:** As a developer, I want a new database table for storing summaries, so that summaries persist across sessions.

#### Acceptance Criteria

1. THE System SHALL create a conversation_summaries table with columns: id (primary key), session_id (foreign key, unique), summary (TEXT), last_message_id, original_token_count, summary_token_count, create_time, update_time
2. THE session_id SHALL have a foreign key constraint referencing conversation_sessions(id) with ON DELETE CASCADE
3. THE System SHALL create an index on session_id for efficient lookups
4. THE table SHALL use utf8mb4 character set for proper Unicode support
