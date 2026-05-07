# Implementation Plan: Conversation Distillation

## Overview

实现对话蒸馏功能，包括：配置类、实体类、Repository、核心服务类，以及与现有服务的集成。采用增量开发方式，先建立基础设施，再实现核心逻辑，最后集成测试。

## Tasks

- [-] 1. 创建数据库表和实体类
  - [ ] 1.1 创建 ConversationSummary 实体类
    - 创建 `ConversationSummary.java` 实体类，包含所有必需字段
    - 添加 JPA 注解：@Entity, @Table, @Column 等
    - 实现 @PrePersist 和 @PreUpdate 生命周期方法
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
  
  - [ ]* 1.2 编写 ConversationSummary 实体类的属性测试
    - **Property 8: Token Counting Consistency**
    - **Validates: Requirements 1.3, 1.4**
  
  - [ ] 1.3 更新 schema.sql 添加 conversation_summaries 表
    - 添加建表 SQL 语句，包含所有列和约束
    - 添加外键约束和索引
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [-] 2. 创建配置类和 Repository
  - [ ] 2.1 创建 DistillationConfig 配置类
    - 使用 @ConfigurationProperties 注解
    - 定义所有配置字段及其默认值
    - 添加 getter/setter 方法
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [ ] 2.2 创建 ConversationSummaryRepository 接口
    - 继承 JpaRepository
    - 定义 findBySessionId、deleteBySessionId、existsBySessionId 方法
    - _Requirements: 4.1, 4.3, 4.6_

- [-] 3. 实现核心蒸馏服务
  - [ ] 3.1 创建 ConversationDistillationService 接口
    - 定义所有接口方法签名
    - 添加完整的 JavaDoc 注释
    - _Requirements: 7.1_
  
  - [ ] 3.2 实现 ConversationDistillationServiceImpl - Token 计算方法
    - 实现 countTotalTokens() 方法
    - 实现 needsDistillation() 方法
    - 复用 ChatMemoryService.countTokens() 方法
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 9.1_
  
  - [ ]* 3.3 编写 Token 计算方法的属性测试
    - **Property 8: Token Counting Consistency**
    - **Validates: Requirements 1.3, 1.4**
  
  - [ ] 3.4 实现 ConversationDistillationServiceImpl - 历史分割方法
    - 实现 splitHistory() 私有方法
    - 处理边界条件（消息数不足等）
    - _Requirements: 2.1, 2.3, 2.4_
  
  - [ ] 3.5 实现 ConversationDistillationServiceImpl - 摘要生成方法
    - 实现 getOrCreateSummary() 方法
    - 实现 callSummarizationAPI() 私有方法，调用 DeepSeek API
    - 实现 buildSummaryPrompt() 私有方法
    - 处理 API 调用失败的降级逻辑
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 8.1, 8.5_
  
  - [ ]* 3.6 编写摘要生成方法的属性测试
    - **Property 3: Summary Content Preservation**
    - **Property 7: Summary Token Bound**
    - **Property 9: Graceful Degradation on API Failure**
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5**
  
  - [ ] 3.7 实现 ConversationDistillationServiceImpl - 摘要存储方法
    - 实现 saveSummary() 私有方法
    - 处理数据库写入失败的情况
    - _Requirements: 4.1, 4.2, 4.5, 4.6, 8.2_
  
  - [ ] 3.8 实现 ConversationDistillationServiceImpl - 核心蒸馏方法
    - 实现 getHistoryWithDistillation() 方法
    - 实现 distillConversation() 方法
    - 构建蒸馏后的历史记录（摘要 + 最近消息）
    - _Requirements: 1.1, 2.1, 2.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4_
  
  - [ ]* 3.9 编写核心蒸馏方法的属性测试
    - **Property 1: Token Reduction Guarantee**
    - **Property 2: Recent Messages Preservation**
    - **Property 4: Idempotent Distillation**
    - **Property 5: Summary Freshness**
    - **Property 6: Threshold Trigger Condition**
    - **Property 10: Minimum Message Threshold**
    - **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 2.3, 3.3, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4**

- [ ] 4. 检查点 - 核心功能测试
  - 确保所有单元测试和属性测试通过，如有问题请询问用户。

- [x] 5. 集成到现有服务
  - [ ] 5.1 修改 RAGServiceImpl 集成蒸馏服务
    - 注入 ConversationDistillationService
    - 在 chatWithRAG() 方法中调用 getHistoryWithDistillation() 替代 getHistory()
    - _Requirements: 7.1, 7.2_
  
  - [ ] 5.2 编写集成测试
    - 测试完整的蒸馏流程
    - 测试与 RAGService 的集成
    - 使用 H2 内存数据库和 Mock 的 DeepSeek API
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [ ] 5.3 更新 application.yml 添加蒸馏配置
    - 添加 app.distillation 配置节
    - 设置合理的默认值
    - _Requirements: 6.1, 6.4_

- [ ] 6. 最终检查点 - 完整验证
  - 确保所有测试通过，功能完整可用，如有问题请询问用户。

## Notes

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 开发
- 每个任务都引用具体的需求，确保可追溯性
- 检查点确保增量验证
- 属性测试验证通用正确性属性
- 单元测试验证特定示例和边界情况
