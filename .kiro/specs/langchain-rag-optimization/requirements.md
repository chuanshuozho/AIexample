# 需求文档

## 简介

本功能为现有 AI 聊天应用添加 LangChain 和 RAG（检索增强生成）优化能力，实现多轮对话记忆和知识库检索功能。用户可以上传文档构建知识库，AI 将基于检索到的相关内容回答问题，同时支持上下文连续对话。

## 词汇表

- **RAG**: Retrieval-Augmented Generation，检索增强生成，一种结合检索和生成的 AI 技术
- **ChatMemory**: 聊天记忆组件，用于存储和管理多轮对话上下文
- **KnowledgeBase**: 知识库，存储用户上传的文档及其向量嵌入
- **DocumentChunk**: 文档分片，将长文档切分为较小的文本块以便检索
- **Embedding**: 向量嵌入，将文本转换为高维向量表示
- **VectorStore**: 向量存储，用于存储和检索向量嵌入
- **SimilarityScore**: 相似度分数，衡量查询与文档分片的相似程度
- **SpringAI**: Spring 官方的 AI 集成框架
- **LangChain4j**: Java 版 LangChain 框架，提供 AI 应用开发工具链

## 需求

### 需求 1：多轮对话记忆

**用户故事：** 作为用户，我希望 AI 能记住之前的对话内容，以便进行连续的多轮对话。

#### 验收标准

1. WHEN 用户发送消息时，THE ChatMemory SHALL 自动加载当前会话的历史消息作为上下文
2. WHEN 历史消息超过配置的最大 token 数量时，THE ChatMemory SHALL 按时间顺序保留最近的消息
3. WHEN 用户创建新会话时，THE ChatMemory SHALL 初始化一个空的对话历史
4. WHEN 用户切换会话时，THE ChatMemory SHALL 加载对应会话的历史消息
5. THE ChatMemory SHALL 将对话历史存储在 MySQL 数据库中

### 需求 2：知识库文档上传

**用户故事：** 作为用户，我希望能够上传文档到知识库，以便 AI 可以基于这些文档回答问题。

#### 验收标准

1. WHEN 用户上传文档时，THE KnowledgeBase SHALL 接收并存储原始文档文件
2. WHEN 上传的文件大小超过 10MB 时，THE KnowledgeBase SHALL 返回错误提示
3. THE KnowledgeBase SHALL 支持 PDF、TXT、MD、DOCX 格式的文档上传
4. WHEN 文档上传成功后，THE KnowledgeBase SHALL 返回文档 ID 和处理状态
5. THE KnowledgeBase SHALL 记录每个文档的文件名、大小、上传时间和处理状态

### 需求 3：文档分片与向量化

**用户故事：** 作为系统，我需要将文档分片并向量化，以便支持高效的语义检索。

#### 验收标准

1. WHEN 文档上传完成后，THE DocumentProcessor SHALL 自动将文档切分为多个 DocumentChunk
2. THE DocumentProcessor SHALL 按配置的分片大小（默认 500 字符）和重叠大小（默认 50 字符）进行分片
3. WHEN 文档分片完成后，THE EmbeddingService SHALL 调用 Embedding 模型生成每个分片的向量嵌入
4. THE VectorStore SHALL 将向量嵌入存储在 MySQL 数据库中
5. WHEN 向量化过程失败时，THE DocumentProcessor SHALL 记录错误并标记文档处理状态为失败
6. THE VectorStore SHALL 存储每个分片的原始文本、向量嵌入、所属文档 ID 和分片序号

### 需求 4：知识库相似度检索

**用户故事：** 作为用户，我希望 AI 能够从知识库中检索相关内容来回答我的问题。

#### 验收标准

1. WHEN 用户发送问题时，THE RetrievalService SHALL 将问题转换为向量嵌入
2. THE RetrievalService SHALL 在 VectorStore 中执行相似度搜索
3. WHEN 检索到相关内容时，THE RetrievalService SHALL 返回相似度分数最高的前 K 个分片（默认 K=3）
4. THE RetrievalService SHALL 只返回相似度分数超过阈值（默认 0.7）的分片
5. WHEN 没有检索到相关内容时，THE RetrievalService SHALL 返回空结果列表

### 需求 5：RAG 增强对话

**用户故事：** 作为用户，我希望 AI 能够结合知识库内容回答问题，提供更准确和相关的回答。

#### 验收标准

1. WHEN 用户发送问题时，THE RAGService SHALL 先执行知识库检索获取相关内容
2. WHEN 检索到相关内容时，THE RAGService SHALL 将检索结果作为上下文注入到提示词中
3. THE RAGService SHALL 构建包含系统提示、检索上下文、对话历史和用户问题的完整提示
4. WHEN 没有检索到相关内容时，THE RAGService SHALL 使用常规对话模式回答
5. THE RAGService SHALL 在回答中标注信息来源（来自知识库或通用知识）

### 需求 6：知识库管理界面

**用户故事：** 作为用户，我希望有一个可视化的知识库管理界面，以便管理上传的文档。

#### 验收标准

1. WHEN 用户访问知识库页面时，THE Frontend SHALL 显示所有已上传文档的列表
2. THE Frontend SHALL 显示每个文档的文件名、大小、上传时间、分片数量和处理状态
3. WHEN 用户点击删除按钮时，THE Frontend SHALL 弹出确认对话框
4. WHEN 用户确认删除时，THE KnowledgeBase SHALL 删除文档及其所有分片和向量数据
5. THE Frontend SHALL 提供文档上传的拖拽区域和进度显示

### 需求 7：Spring AI 框架集成

**用户故事：** 作为开发者，我希望使用 Spring AI 框架标准化 AI 功能实现，以便提高代码可维护性和扩展性。

#### 验收标准

1. THE System SHALL 集成 Spring AI 框架（版本 1.0.0 或更高）
2. THE System SHALL 使用 Spring AI 的 ChatClient 接口进行对话
3. THE System SHALL 使用 Spring AI 的 EmbeddingModel 接口进行向量化
4. THE System SHALL 使用 Spring AI 的 VectorStore 接口进行向量存储
5. WHEN Spring Boot 版本不满足 Spring AI 要求时，THE System SHALL 升级到 Spring Boot 3.2.0 或更高版本
6. THE System SHALL 保持与现有 DeepSeek API 的兼容性

### 需求 8：向量存储 MySQL 实现

**用户故事：** 作为开发者，我希望使用现有 MySQL 数据库作为向量存储，以便减少基础设施复杂度。

#### 验收标准

1. THE VectorStore SHALL 在 MySQL 中创建向量存储表
2. THE VectorStore SHALL 使用 JSON 类型存储向量嵌入数据
3. WHEN 执行相似度搜索时，THE VectorStore SHALL 使用余弦相似度算法计算相似度分数
4. THE VectorStore SHALL 支持向量数据的增删改查操作
5. THE VectorStore SHALL 支持按文档 ID 批量删除向量数据

### 需求 9：Embedding 模型配置

**用户故事：** 作为开发者，我希望能够配置不同的 Embedding 模型，以便根据需求选择合适的向量化方案。

#### 验收标准

1. THE System SHALL 支持配置 DeepSeek Embedding API 作为默认向量化服务
2. THE System SHALL 支持配置 OpenAI Embedding API 作为备选向量化服务
3. THE System SHALL 支持配置本地 Embedding 模型（如 ONNX 格式）
4. WHEN Embedding API 调用失败时，THE System SHALL 记录错误并返回失败状态
5. THE EmbeddingService SHALL 缓存已计算的向量嵌入以避免重复计算

### 需求 10：API 接口设计

**用户故事：** 作为开发者，我希望有清晰的 REST API 接口，以便前后端分离开发和集成。

#### 验收标准

1. THE System SHALL 提供 POST /api/knowledge/documents 接口用于文档上传
2. THE System SHALL 提供 GET /api/knowledge/documents 接口用于获取文档列表
3. THE System SHALL 提供 DELETE /api/knowledge/documents/{id} 接口用于删除文档
4. THE System SHALL 提供 POST /api/chat/rag 接口用于 RAG 增强对话
5. THE System SHALL 提供 GET /api/knowledge/documents/{id}/chunks 接口用于查看文档分片
6. WHEN API 调用发生错误时，THE System SHALL 返回标准化的错误响应格式
