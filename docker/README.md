# Docker 环境说明

## 三个环境

| 环境 | 起什么 | 后端跑在哪 | 前端跑在哪 |
|---|---|---|---|
| 后端开发（IDEA） | 只有 `mysql` 容器 | IDEA 里跑 `LostFoundApplication` | — |
| 前端开发（VS Code） | 只有 `mysql` 容器 | 同上 | `npm run dev`（5173） |
| 测试 / 演示 | `mysql` + `backend` + `frontend` | 容器内 8080 | 容器内 nginx 80 |

## 常用命令

```bash
# 只起数据库（本地开发用）
docker compose up -d mysql

# 全栈部署（测试同学验收用）
cp .env.example .env          # 首次：改成自己的密码
docker compose --profile full up -d --build

# 查看状态 / 日志
docker compose ps
docker compose logs -f backend

# 停止（保留数据）
docker compose --profile full stop

# 彻底重置（删除数据卷，重新执行 db/init 脚本）
docker compose --profile full down -v
```

## 端口

| 服务 | 宿主机 | 容器内 | 说明 |
|---|---|---|---|
| frontend | 80 | 80 | nginx 托管前端，并代理 `/api`、`/uploads` 到 backend |
| backend | 8080 | 8080 | Spring Boot |
| mysql | **3307** | 3306 | 本机已有 MySQL 占用 3306，故避开；库名 `lost_found` |

## 数据库初始化

`db/init/01-schema.sql` 同步自《详细设计》的 `lost_found.sql`（7 张表 + 6 条分类种子），
只在 **数据卷首次创建时** 执行一次。改了表结构记得：

```bash
docker compose --profile full down -v && docker compose --profile full up -d --build
```

## 常见问题

- **80 端口被占用**：改 `.env` 里的 `FRONTEND_PORT`。
- **拉不到镜像**：本机 Docker Hub 直连可能超时，可配置镜像加速器（Docker Desktop → Settings → Docker Engine 加 `registry-mirrors`）。
- **后端健康检查一直不通过**：先看 `docker compose logs backend`，多为数据库连接或依赖下载失败。
- **首次构建很慢**：Maven 在本机 `~/.m2` 无缓存时要下载全部依赖；可先在本机 `mvn -B test` 跑一遍再构建镜像。
- **默认管理员**：`admin` / `Admin@123`（`ADMIN_INIT_PASSWORD` 可覆盖），登录后请改密。
- **图片不显示**：确认 nginx 已反代 `/uploads`，且后端卷 `clf-uploads` 已挂载到 `/app/uploads`。
- **升级后库是空的**：多半是本地 `.env` 里还留着旧的 `MYSQL_DATABASE=campus_lost_found`（`.env` 不随仓库更新）。改成 `MYSQL_DATABASE=lost_found` 后执行 `docker compose --profile full down -v && up -d`。
