# 在线社区论坛系统

基于实习计划技术栈的微服务论坛：**Spring Boot 3 + Spring Cloud Gateway + Nacos + MyBatis + MySQL + Redis + RocketMQ**，前端 **Vue 2 + Element UI**。

## 架构

| 服务 | 端口 | 说明 |
|------|------|------|
| api-gateway | 8080 | 统一入口、JWT 鉴权 |
| user-service | 8081 | 注册登录、个人中心 |
| post-service | 8082 | 版块、帖子、回复、点赞、搜索 |
| notification-service | 8083 | 通知（RocketMQ 消费） |
| file-service | 8084 | 图片上传 |
| admin-service | 8085 | 管理后台聚合 API |
| frontend | 8088 | Vue 开发服务器 |

基础设施：MySQL 3306、Redis 6379、Nacos 8848、RocketMQ NameServer 9876

## 默认账号

| 手机号 | 密码 | 昵称 | 角色 |
|--------|------|------|------|
| 13800000001 | admin123 | 管理员 | 管理员 |
| 13800000002 | demo123 | 演示用户 | 普通用户 |

登录请使用**手机号**，昵称仅用于社区内展示。首次启动 `user-service` 时会自动创建上述账号。

## 快速启动

### 1. 启动基础设施（必须先做）

```bash
cd deploy
docker compose up -d
```

**等待约 1–2 分钟**，确认 Nacos 就绪后再启动微服务：

```bash
# 应能打开 Nacos 控制台
open http://localhost:8848/nacos/

# 或检查 gRPC 端口（Nacos 2.x 必需）
nc -zv 127.0.0.1 8848
nc -zv 127.0.0.1 9848
```

若跳过此步，日志会出现 `Connection refused: /127.0.0.1:9848`，所有微服务注册 Nacos 失败并退出。

**若拉取镜像超时**（`context deadline exceeded`）：`docker-compose.yml` 已使用 DaoCloud 镜像源。仍失败时，打开 Docker Desktop → Settings → Docker Engine，加入：

```json
"registry-mirrors": ["https://docker.m.daocloud.io", "https://docker.1ms.run"]
```

Apply & Restart 后重试 `docker compose up -d`。

**Apple Silicon (M1/M2/M3) 用户**：若报 `no matching manifest for linux/arm64`，Nacos/RocketMQ 已配置为 `platform: linux/amd64`，需确保 Docker Desktop 已开启 **Use Rosetta for x86/amd64 emulation**（Settings → General）。

### 2. 编译后端

```bash
# 含单元测试
mvn clean package

# 跳过测试（快速打包）
mvn clean package -DskipTests
```

### 2b. 一键 Docker 全栈（可选）

基础设施就绪后，可构建并启动全部微服务与前端：

```bash
cd deploy
docker compose --profile app up -d --build
```

访问 http://localhost:8088 。详见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

### 3. 启动各微服务（IDEA 或 Maven）

IDEA 中运行各模块 `*Application` 主类，或使用：

```bash
mvn spring-boot:run -pl api-gateway
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl post-service
mvn spring-boot:run -pl notification-service
mvn spring-boot:run -pl file-service
mvn spring-boot:run -pl admin-service
```

也可打包后运行 fat jar：

```bash
mvn clean package -DskipTests
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
# ... 其余服务同理
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run serve
```

访问 http://localhost:8088

## 功能清单

- 用户：注册、登录、JWT、个人中心、@用户搜索
- 帖子：版块、发帖/编辑/删除、列表分页、详情、搜索、热门（Redis ZSET）
- 回复：楼中楼、点赞（RocketMQ 异步计数）
- 通知：回复提醒、@提醒、点赞提醒
- 文件：图片上传（本地存储，经 Gateway 访问）
- 管理后台：数据统计、版块 CRUD、删帖、用户封禁
- **缓存**：版块树、用户信息、帖子详情（Redis）
- **限流**：Gateway 全局限流、发帖/回复/登录频率控制
- **单元测试**：`forum-common` 工具类、`post-service` 内容规范化
- **容器化**：`docker compose --profile app` 一键部署

## 工程能力

| 能力 | 状态 |
|------|------|
| 分布式微服务 | Nacos + Gateway + Feign + RocketMQ |
| Redis 缓存 | 帖子/热门/版块/用户 |
| 限流 | Gateway + 业务层 Redis |
| Docker 部署 | 基础设施 + app profile 全栈 |
| 单元测试 | `mvn test` |

详细部署与多实例扩展见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## API 入口

所有请求经 Gateway：`http://localhost:8080/api/**`

Postman 集合见 [docs/api.postman_collection.json](docs/api.postman_collection.json)

## 环境变量

| 变量 | 默认值 |
|------|--------|
| NACOS_ADDR | 127.0.0.1:8848 |
| MYSQL_HOST | 127.0.0.1 |
| MYSQL_PASSWORD | root123 |
| REDIS_HOST | 127.0.0.1 |
| ROCKETMQ_NAMESRV | 127.0.0.1:9876 |
| MYSQL_PORT | 3307（宿主机连 Docker MySQL；容器内为 3306） |

## 技术栈对照（实习计划）

- Java / Maven / Git / Docker / Postman
- Spring Boot、Spring MVC、Spring Cloud、Nacos
- MyBatis、MySQL、Redis
- RocketMQ
- Vue.js、Element UI、JavaScript
