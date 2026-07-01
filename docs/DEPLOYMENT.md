# 部署指南

本文档说明在线社区论坛系统的**容器化一键部署**与**多实例扩展**方式。

## 能力概览

| 能力 | 实现 |
|------|------|
| 分布式 | Spring Cloud 微服务 + Nacos 注册发现 + OpenFeign + RocketMQ |
| 缓存 | Redis：帖子详情、热门榜、版块树、用户信息 |
| 限流 | Gateway 全局限流；发帖/回复/登录业务限流 |
| 容器部署 | Docker Compose 基础设施 + `--profile app` 应用栈 |
| 单元测试 | `forum-common`、`post-service` 核心工具类测试 |

## 一键启动（Docker）

### 仅基础设施（本地 IDEA 调试）

```bash
cd deploy
docker compose up -d
```

MySQL 映射宿主机 **3307** 端口，避免与本机 MySQL 冲突。

### 全栈（基础设施 + 全部微服务 + 前端）

```bash
cd deploy
docker compose --profile app up -d --build
```

首次构建需下载 Maven/npm 依赖，耗时较长。就绪后：

- 前端：http://localhost:8088
- API Gateway：http://localhost:8080/api/
- Nacos：http://localhost:8848/nacos/

容器内服务通过 Docker 网络互联，环境变量已预设：

| 变量 | 容器内值 |
|------|----------|
| MYSQL_HOST | mysql |
| MYSQL_PORT | 3306 |
| REDIS_HOST | redis |
| NACOS_ADDR | nacos:8848 |
| ROCKETMQ_NAMESRV | rocketmq-namesrv:9876 |

### 停止

```bash
docker compose --profile app down
# 保留数据卷；彻底清理加 -v
```

## 限流说明

| 层级 | 规则 | 配置 |
|------|------|------|
| Gateway | 每 IP/用户 120 次/分钟 | `forum.gateway.rate-limit.requests-per-minute` |
| 发帖 | 同一用户 5 秒内 1 次 | Redis `rate:post:{userId}` |
| 回复 | 同一用户 3 秒内 1 次 | Redis `rate:reply:{userId}` |
| 登录 | 同一手机号 10 次/分钟 | Redis `rate:login:{phone}:{window}` |

触发 Gateway 限流返回 HTTP **429**；业务限流返回业务错误提示。

## 缓存说明

| Key | 内容 | TTL |
|-----|------|-----|
| `cache:boards:tree` | 版块树 | 30 分钟（CRUD 时失效） |
| `cache:user:{id}` | 用户实体 | 15 分钟（资料/封禁变更时失效） |
| `post:detail:v2:{id}` | 帖子详情 | 10 分钟 |
| `hot:posts:*` | 热门帖 ZSET | 实时更新 |

## 多实例部署

Nacos 已支持同一服务的多实例注册，可按以下方式水平扩展：

### 1. 扩展无状态服务

以 `post-service` 为例，在多台主机或同一 Compose 中启动多个实例（端口不同）：

```bash
# 主机 A：实例 1（默认 8082）
java -jar post-service/target/post-service-1.0.0-SNAPSHOT.jar

# 主机 B：实例 2（改端口）
SERVER_PORT=8086 java -jar post-service-1.0.0-SNAPSHOT.jar \
  --spring.cloud.nacos.discovery.ip=192.168.1.20
```

Gateway 通过 `lb://post-service` 负载均衡到所有健康实例。

### 2. 建议扩展顺序

1. **post-service / user-service** — 读写压力最大，优先扩展
2. **api-gateway** — 入口层，可部署 2+ 实例 + 前置 Nginx/SLB
3. **notification-service** — MQ 消费端，可增加消费者实例（同 group 分摊消息）

### 3. 有状态组件

| 组件 | 多实例注意 |
|------|------------|
| MySQL | 主从或云 RDS，应用层读写分离需额外配置 |
| Redis | 单机够用时可 Sentinel/Cluster；缓存 key 设计已支持多实例 |
| RocketMQ | Broker 可集群；Producer/Consumer group 保持不变 |
| file-service | 上传目录需共享存储（NFS/OSS），或前置统一文件服务 |

### 4. Docker Compose 扩展示例

```bash
docker compose --profile app up -d --scale post-service=2
```

需去掉 `post-service` 的固定 `container_name` 与宿主机端口映射冲突（生产环境通常不映射业务端口，仅通过 Gateway 访问）。

## 运行测试

```bash
# 全项目测试
mvn test

# 仅公共模块
mvn test -pl forum-common

# 打包（含测试）
mvn clean package
```

## 数据库补丁

若从旧版本升级，按需执行：

```bash
docker exec -i forum-mysql mysql -uroot -proot123 < deploy/sql/06_feature_schema.sql
docker exec -i forum-mysql mysql -uroot -proot123 < deploy/sql/07_user_feature_schema.sql
```
