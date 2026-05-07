# Docker 部署指南

## 快速开始

### 1. 配置环境变量

复制 `.env.example` 为 `.env` 并修改配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件，设置你的 DeepSeek API Key：

```env
DEEPSEEK_API_KEY=your-actual-api-key-here
```

### 2. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 停止服务
docker-compose down
```

### 3. 访问应用

- 主应用: http://localhost:8081
- 知识库管理: http://localhost:8081/knowledge.html
- 健康检查: http://localhost:8081/actuator/health

## 服务说明

### MySQL 数据库
- 端口: 3306
- 数据库名: ai_chat
- 用户名: root
- 密码: 由 `MYSQL_ROOT_PASSWORD` 环境变量控制

### Spring Boot 应用
- 端口: 8081
- 内存限制: 256MB-512MB

## 常用命令

```bash
# 重新构建应用
docker-compose build app

# 重启应用
docker-compose restart app

# 查看应用日志
docker-compose logs -f app

# 进入应用容器
docker-compose exec app sh

# 进入 MySQL 容器
docker-compose exec mysql mysql -u root -p

# 清理所有数据（包括数据库）
docker-compose down -v
```

## 数据持久化

数据存储在 Docker volumes 中：
- `mysql_data`: MySQL 数据库数据
- `uploads_data`: 上传的文档文件

## 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | root123 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `APP_KNOWLEDGE_MAX_FILE_SIZE` | 最大文件大小 | 10485760 (10MB) |
| `APP_KNOWLEDGE_CHUNK_SIZE` | 文档分片大小 | 500 |
| `APP_KNOWLEDGE_CHUNK_OVERLAP` | 分片重叠大小 | 50 |
| `APP_KNOWLEDGE_RETRIEVAL_TOP_K` | 检索返回数量 | 3 |
| `APP_KNOWLEDGE_RETRIEVAL_SIMILARITY_THRESHOLD` | 相似度阈值 | 0.7 |

## 生产环境建议

1. 修改默认密码
2. 配置 HTTPS
3. 设置资源限制
4. 配置日志收集
5. 定期备份数据库

### 资源限制配置

在 `docker-compose.yml` 中添加：

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```
