# ZeroCode - 零代码平台（后端）

## 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.9+
- Maven 3.8+

## 快速启动

1. 复制配置文件并填入你自己的密钥：
```bash
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
```

2. 编辑 `application.yaml`，填入你的数据库、Redis、RabbitMQ、邮箱、阿里云等信息。

3. 创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS ZeroCodeGenerateProject CHARACTER SET utf8mb4;
```

4. 启动：
```bash
mvn spring-boot:run
```

服务运行在 `http://localhost:8080`

<img width="2353" height="1226" alt="image" src="https://github.com/user-attachments/assets/e883ffdc-d60a-4821-a4db-ae3790bd9833" />
这是首页信息，可以查看热门项目以及排行榜，并且优先根据点赞量进行向量化，用于 AI 生成项目时作为参考
