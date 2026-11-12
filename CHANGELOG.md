# 更新日志

本文件记录**面向使用者的变更**（功能、接口、部署方式、破坏性改动）。
每次迭代的详细过程（目标、验证命令、遗留问题）记录在 [`项目文档/迭代记录/`](项目文档/迭代记录/)。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循语义化版本。

## [Unreleased]

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
