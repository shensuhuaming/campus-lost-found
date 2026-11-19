# 更新日志

本文件记录**面向使用者的变更**（功能、接口、部署方式、破坏性改动）。
每次迭代的详细过程（目标、验证命令、遗留问题）记录在 [`项目文档/迭代记录/`](项目文档/迭代记录/)。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循语义化版本。

## [Unreleased]

### 新增
- 失物信息 M2：`POST /api/items` 发布（≤3 图，图片白名单+存在性+未引用校验）、`PUT /api/items/{id}` 编辑（仅进行中，编辑后重新审核）、`PUT /api/items/{id}/close` 关闭（管理员必须填理由，写 `close_reason`，待核验认领自动拒绝）、`PUT /api/items/{id}/status` 寻物状态推进、`POST /api/upload/image` 图片上传（类型/大小/文件头三重校验）
- 浏览检索 M3：`GET /api/items` 列表（类型/分类/状态筛选、关键词搜索且标题命中优先、分页）、`GET /api/items/{id}` 详情（图片、发布者、留言前 10 条、我的认领状态、联系方式按登录态掩码）、`GET /api/categories` 公开分类
- 前端：首页列表（筛选/搜索/卡片/分页）、详情页（图片预览、本人操作）、发布/编辑页（3 图上传）、个人中心发布管理与操作列
- 单元测试新增 31 个（ItemService 20 / FileService 5 / ItemController 6），合计 73
- 个人中心 U1.4~U1.6：`GET/PUT /api/user/profile`、`PUT /api/user/password`、`GET /api/user/{items,claims,comments}`（统一分页结构，列表补齐分类名/封面/认领数/物品标题）
- 前端登录/注册页（`/login`）、个人中心页（`/profile`，5 个页签）、布局与导航；路由守卫按登录态拦截；axios 拦截器自动带 token、401 清登录态并跳转
- 用户认证模块 U1.1~U1.3：`POST /api/auth/register`、`POST /api/auth/login`（JWT，12 小时）、`POST /api/auth/logout`
- 公共基座：`Result`/`ResultCode`（21 个业务码 + HTTP 映射）、`BusinessException`、全局异常处理按错误码返回
- JWT 鉴权拦截器：校验签名/有效期 + 查库校验禁用状态，写入 ThreadLocal；`SecurityUtil` 提供当前用户
- `DataInitializer` 启动时幂等创建管理员（口令来自 `ADMIN_INIT_PASSWORD`）
- 单元测试 24 个：注册/登录/退出/拦截器/JWT/接口校验

### 变更（需注意）
- **破坏性**：`ApiResponse` 已替换为 `Result`，成功码 `200`（对齐《API接口详细规格》1.2），错误码为 1xxx~9999，HTTP 状态按类型返回
- **破坏性**：数据库名改为 `lost_found`、排序规则 `utf8mb4_general_ci`；`db/init/01-schema.sql` 直接同步《详细设计》的 `lost_found.sql`，删除重复的 `02-seed.sql`
- `lost_item` 新增 `close_reason`（关闭/下架理由，管理员必填），`phone` 放宽为 5~20 位联系方式
- 新增接口：`PUT /api/items/{id}/status`（寻物状态推进）、`GET /api/categories`（公开分类）
- 统计趋势默认改为 30 天（对齐 FR-20）；待审核数排除已关闭信息；消息改为事务提交后推送
- 编辑限制收紧为「仅进行中可编辑」；图片 URL 增加白名单、存在性与未引用校验
- compose 新增 `clf-uploads` 卷与 `ADMIN_INIT_PASSWORD`；nginx 反代 `/uploads`

### 文档
- 修复 P2 的 14 条文档一致性问题（O-15~O-28）：ER 留言联系改为 M:N 并补图示说明、需求书升 V1.1（路径/Node 20+/`close_reason`）、认领自己统一 3001、核验文案更正、关闭与核验时给被自动拒绝的申请人补通知、`message.type` 去默认值、`user`/`claim` 补 `updated_at`、删除 Redis 浏览量表述、Mapper 与 Controller 表述修正、`checkClaim` 签名统一、用例图引用改为真实路径
- 审查需求分析阶段全部材料（需求书、用例描述、ER 图与关系模式、状态转换图 2 张、类图）与概要设计说明书 V1.0，输出 [审查意见](项目文档/审查意见/2026-11-17-需求与概要设计审查.md)：6 个阻塞项（响应码冲突、寻物无完成路径、状态耦合、关闭理由缺字段、分类接口缺失、图片路径未定）、6 个待确认、12 个一致性项
- 《可行性分析报告》补第 7 章「可行性实测记录」，版本升 V1.1：把立项阶段的定性判断换成 v0.1.0 实测数据（构建耗时、镜像体积、已识别风险）
- 需求分析阶段 4 张图渲染为 PNG，便于评审查阅：`项目文档/截图/需求图/`
- 审查详细设计第一版（说明书 1890 行、API 规格 835 行、建库脚本、21 张图），输出 [详细设计审查](项目文档/审查意见/2026-11-19-详细设计审查.md)：7 个阻塞项、5 个待确认、14 个一致性项
- 新增 [待定与待解决问题总清单](项目文档/审查意见/2026-11-19-待定与待解决问题总清单.md)：合并三轮审查为 P0 8 条 / P1 6 条 / P2 14 条，附已关闭 12 条与评审会 8 个决策点
- 详细设计材料入库：`项目设计/详细设计/`（说明书、API 规格、`lost_found.sql`、21 张图）

### 计划中
- 用户模块：注册、登录（JWT）、个人中心、管理员禁用账号
- 失物信息模块：发布（含 3 图上传）、编辑、下架、审核
- 浏览检索：列表分页、分类/类型筛选、关键词搜索、详情
- 认领归还：认领申请、管理员核验、状态机流转、关闭
- 留言与站内消息
- 管理后台：分类管理、用户管理、数据统计

---

## [0.1.0] - 2026-11-12

首个可部署版本：**开发环境基座 + 需求解读**，无业务功能。

### 新增
- 后端骨架（Spring Boot 3.5.9 / JDK 17）：统一响应体 `ApiResponse`、全局异常处理、跨域配置、健康检查 `GET /api/system/health`（含数据库连通性）
- 前端骨架（Vue 3 + Element Plus + Vite 6 + TypeScript）：环境自检首页，展示后端与数据库状态、模块清单、环境速查
- 数据库基线：7 张表（user / category / lost_item / item_image / claim / comment / message）+ 6 个分类种子数据，`utf8mb4`，时区 +08:00
- Docker Compose 三环境编排：`mysql`（3307）、`backend`（8080）、`frontend`（80）；`docker compose --profile full up -d --build` 一键起全栈
- 后端 Dockerfile（多阶段构建）、前端 Dockerfile（Node 构建 → nginx 托管）、nginx 反向代理 `/api` 与 `/health`
- 容器内 Maven 使用阿里云镜像配置 `backend/maven-settings.xml`
- 单元测试基座：JUnit 5 + Mockito + MockMvc 切片测试（健康检查正常 / 数据库降级两条路径）
- 覆盖率：JaCoCo，`mvn verify` 生成 `backend/target/site/jacoco/index.html`
- CI：GitHub Actions（后端 `mvn verify` + 上传覆盖率、前端类型检查与构建）
- 工程规范：`.gitignore`、`.editorconfig`、`.env.example`、`docker/README.md`
- 文档：需求规格说明书（V1.0 归档）、需求理解与工程方案（含 12 个待设计确认问题 Q1~Q12）、进度快照、迭代记录

### 修复
- 工程目录含中文导致 `docker compose` 项目名为空 → 显式 `name: clf`
- 工程目录含中文导致 JaCoCo 写不出 exec 数据 → 数据文件改到 `${java.io.tmpdir}`
- `vite build` 因系统临时目录限制报 esbuild `Access is denied` → 构建时 `TEMP/TMP` 指向项目内 `.tmp`

### 备注
- 服务端不含任何业务接口，仅环境自检；业务代码待《详细设计 / 接口文档》评审后开始。
- 首次部署需 5 个基础镜像，Docker Hub 直连可能超时，可用镜像站拉取后 `docker tag` 回官方名（见 README）。

[Unreleased]: https://github.com/shensuhuaming/campus-lost-found/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/shensuhuaming/campus-lost-found/releases/tag/v0.1.0
