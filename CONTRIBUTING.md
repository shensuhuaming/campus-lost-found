# 协作规范

> 7 人并行开发，靠规范避免互相踩。违反成本最低的是命名和记录，最容易出问题的是数据库脚本和部署配置。

## 1 分支

| 分支 | 用途 | 说明 |
|---|---|---|
| `main` | 可发布版本 | 受保护，禁止直接 push，只接受 PR 合并 |
| `feature/<模块>-<简述>` | 功能开发 | 例：`feature/user-register`、`feature/claim-apply` |
| `fix/<问题>` | 缺陷修复 | 例：`fix/claim-duplicate` |
| `docs/<主题>` | 文档 | 例：`docs/api-spec` |

- 分支从最新 `main` 切出，生命周期不超过一个迭代；
- 合并前先 `git rebase main`，保持线性历史；
- 合并后删除远端分支。

## 2 提交信息

```
<type>(<scope>): <subject>
```

| type | 含义 |
|---|---|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `test` | 测试相关 |
| `docs` | 文档 |
| `refactor` | 重构（不改外部行为） |
| `chore` | 构建/配置/依赖 |
| `perf` | 性能优化 |

- scope 用模块名：`user` / `item` / `search` / `claim` / `message` / `admin` / `env` / `db`；
- subject 用中文，不超过 50 字，不以句号结尾；
- 正文写"为什么"和"影响面"，一个提交只做一件事。

## 3 每次改动都要留痕

**这是团队硬性约定**：任何合并进 `main` 的改动，必须能在三层记录里查到。

| 层 | 文件 | 谁维护 | 记什么 |
|---|---|---|---|
| 提交级 | git commit / PR | 提交人 | 单点改动、评审意见 |
| 迭代级 | `项目文档/迭代记录/` | 迭代负责人 | 目标、变更清单、验证命令与结果、遗留问题、回滚方式 |
| 版本级 | `CHANGELOG.md` | 合并人 | 使用者视角：新增/修改/修复/破坏性变更 |

PR 描述直接套用 `.github/PULL_REQUEST_TEMPLATE.md`；没有更新迭代记录的 PR 不合并。

## 4 本地提交前必做

```bash
# 后端：单元测试 + 覆盖率（工程路径含中文，注意 Maven 本地仓库参数）
cd backend
mvn -Dmaven.repo.local=%USERPROFILE%\.m2\repository verify

# 前端：类型检查 + 构建（系统临时目录受限时先设置 TEMP）
cd frontend
npm run build
```

CI（`.github/workflows/ci.yml`）会在 PR 上再跑一遍，**CI 不绿不合并**。

## 5 数据库脚本

- 表结构改动写进 `db/init/01-schema.sql`，种子数据写进 `02-seed.sql`；
- 这些脚本**只在数据卷首次创建时执行**，改了必须：
  ```bash
  docker compose --profile full down -v && docker compose --profile full up -d --build
  ```
- 已在用的库不要在脚本里写 `DROP TABLE`；正式上线后改用 migration 目录（待设计定）。

## 6 代码约定

- 后端分层：`controller` → `service` → `mapper`，Controller 不写业务逻辑；
- 接口统一返回 `ApiResponse<T>`，错误码按段分配（见 `README.md` 接口约定）；
- 关键业务逻辑（状态流转、并发、权限）必须有注释说明"为什么"；
- 前端接口统一走 `src/api/` 下的封装，不直接在组件里写 axios；
- 时间统一用 `OffsetDateTime` + ISO-8601 字符串传输。

## 7 评审要点

- 是否覆盖了边界值（长度、数量、权限、并发）；
- 是否有对应的单元测试，且断言的是业务结果不是实现细节；
- 数据库变更是否可回滚、是否需要重置数据卷；
- 是否更新了迭代记录与 CHANGELOG。
