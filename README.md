# 校园失物招领系统 (Campus Lost & Found)

> 软件工程综合实践 · 7 人小组项目
> 覆盖「发布 → 检索 → 认领 → 归还」全流程的校园失物招领 Web 系统

**当前状态：环境基座已完成，业务功能待《详细设计 / 接口文档》评审后实现。**

---

## 一、技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端 | Spring Boot + Java | 3.5.9 / **JDK 17** |
| 构建 | Maven | 3.9.x |
| 前端 | Vue 3 + Element Plus + Vite + TypeScript | Vue 3.5 / Vite 6 |
| 数据库 | MySQL | 8.0 |
| 容器 | Docker Compose | v2 |
| 测试 | JUnit 5 + Mockito + MockMvc + JaCoCo | — |

---

## 二、快速开始

### 1. 测试同学 / 一键部署（推荐）

```bash
# 1) 准备环境变量（首次）
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
#    按需修改 .env（数据库密码、端口等）

# 2) 启动全栈
docker compose --profile full up -d --build

# 3) 访问
#    前端页面   http://localhost
#    接口自检   http://localhost/health
#    后端直连   http://localhost:8080/api/system/health
```

> 首次构建需要下载 Maven / npm 依赖与基础镜像，耗时较长属正常现象。
>
> **镜像拉取慢/失败**（本机实测 Docker Hub 直连会超时）：可先用镜像站拉取再打回官方标签，例如
> ```bash
> docker pull docker.m.daocloud.io/library/nginx:1.27-alpine
> docker tag  docker.m.daocloud.io/library/nginx:1.27-alpine nginx:1.27-alpine
> ```
> 需要的基础镜像：`mysql:8.0`、`eclipse-temurin:17-jre`、`maven:3.9-eclipse-temurin-17`、`node:22-alpine`、`nginx:1.27-alpine`。

停止与重置：

```bash
docker compose --profile full stop          # 停止，保留数据
docker compose --profile full down -v       # 彻底重置（删除数据卷，重新执行 db/init）
```

### 2. 后端开发（IntelliJ IDEA 2025.3）

1. 打开 `backend/pom.xml`（或 `backend` 目录）作为 Maven 项目；
2. **Project SDK 选 JDK 17**（本机路径：`G:\develop\jdks\jdk17\ms-17.0.18`）；
   ⚠️ 本机 `JAVA_HOME` 默认指向 Java 21，务必在 IDEA 里显式指定 17。
3. 先只起数据库：`docker compose up -d mysql`；
4. 运行 `LostFoundApplication`，访问 `http://localhost:8080/api/system/health`；
   正常返回：`{"code":0,...,"data":{"status":"UP","database":{"status":"UP"}}}`。
   本机默认连接 `localhost:3307`（用户 `clf` / 密码 `change-me`，见 `.env`）。

> **Maven 注意**：本机 `Maven 安装目录\conf\settings.xml` 把 `localRepository` 指向了
> `G:\develop\apache-maven-3.9.12-bin\...\mvn_repo`，该目录对当前用户只读，会导致构建报
> `AccessDeniedException`。临时办法：命令行加 `-Dmaven.repo.local=%USERPROFILE%\.m2\repository`；
> 建议在 IDEA 的 Maven 设置里把 "Local repository" 改到 `C:\Users\<你>\.m2\repository`。

### 3. 前端开发（VS Code）

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173，/api 已代理到 localhost:8080
```

---

## 三、目录结构

```
campus-lost-found/
├─ backend/                  # Spring Boot 后端
│  ├─ src/main/java/com/campus/lostfound/
│  │  ├─ controller/         # 接口层（当前仅 SystemController 自检）
│  │  ├─ service/            # 业务层（待实现）
│  │  ├─ mapper/             # 持久层（待实现）
│  │  ├─ entity/ dto/        # 实体与传输对象（待设计确认）
│  │  ├─ common/             # 统一响应体、全局异常处理
│  │  └─ config/             # 跨域等配置
│  ├─ src/test/java/         # 单元测试 / 切片测试
│  └─ Dockerfile
├─ frontend/                 # Vue 3 前端
│  ├─ src/views/             # 页面（当前为环境自检首页）
│  ├─ src/api/               # axios 封装与接口定义
│  └─ Dockerfile / nginx.conf
├─ db/init/                  # 建表与种子数据（MySQL 容器首次启动自动执行）
├─ docker/README.md          # 容器环境说明与常见问题
├─ docker-compose.yml        # 三套环境编排
├─ 需求文档/                  # 需求规格说明书、可行性分析
└─ 项目文档/                  # 需求理解、设计、测试文档
```

---

## 四、开发规范

### Git 分支

| 分支 | 用途 |
|---|---|
| `main` | 可发布版本，禁止直接 push |
| `feature/<模块>-<简述>` | 功能开发，如 `feature/claim-apply` |
| `fix/<问题>` | 缺陷修复 |
| `docs/<主题>` | 文档 |

提交信息格式：`feat(item): 发布信息接口` / `fix(claim): 修复重复申请`
合并方式：PR + 至少 1 人 Review，`main` 每次合并对应一个可用版本并打 tag。

### 单元测试约定

- 每个 Service 方法至少覆盖：正常路径、边界值、异常路径；
- 状态机与认领核验分支 **必须 100% 覆盖**；
- `mvn -B verify` 会生成 JaCoCo 报告：`backend/target/site/jacoco/index.html`；
- 测试类命名：`<被测类>Test`，方法名用中文 `@DisplayName` 说明业务含义。

### 接口约定（待接口契约评审定稿）

统一响应体：

```json
{ "code": 0, "message": "success", "data": {} }
```

`code=0` 成功；建议分段：`1xxxx` 通用/参数、`2xxxx` 用户、`3xxxx` 物品、`4xxxx` 认领、`5xxxx` 留言消息、`9xxxx` 系统。

---

## 五、当前版本包含什么

✅ 已就绪：

- Docker Compose 编排（MySQL 3307 / 后端 8080 / 前端 80），三套环境（dev / test / demo）；
- MySQL 建表基线脚本（7 张表 + 6 个分类种子数据），字段来自需求 V1.0 数据字典；
- Spring Boot 骨架：统一响应体、全局异常、跨域、健康检查；
- Vue 3 + Element Plus 骨架：首页环境自检面板，展示后端连通性与模块清单；
- JUnit 5 切片测试示例、JaCoCo 覆盖率插件、GitHub Actions CI；
- `.gitignore`、`.editorconfig`、环境变量模板。

⏳ 待设计文档确认后实现：

- 用户模块（注册/登录/JWT/个人中心/账号管理）；
- 失物信息模块（发布/编辑/下架/图片上传/审核）；
- 浏览检索、认领归还（状态机）、留言消息、管理后台与统计；
- 持久层选型（MyBatis-Plus / JPA）与完整 DDL 定稿。

> 需求疑点（12 个待澄清问题）见 [项目文档/需求理解与工程方案.md](项目文档/需求理解与工程方案.md) §8。

---

## 六、文档索引

| 文档 | 说明 |
|---|---|
| [需求文档/需求规格说明书.md](需求文档/需求规格说明书.md) | 需求分析组 V1.0 |
| [项目文档/需求理解与工程方案.md](项目文档/需求理解与工程方案.md) | 编码/测试视角的需求解读、待澄清问题 |
| [项目文档/进度快照.md](项目文档/进度快照.md) | 暂停点与恢复步骤（含本机已知坑） |
| [docker/README.md](docker/README.md) | 容器环境、端口、排错 |
