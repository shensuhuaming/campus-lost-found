# 校园失物招领系统 接口详细规格

> 版本 V1.0　日期 2026-11　对应文档：《详细设计说明书》第 6 章
> 本文档细化《概要设计说明书》4.2 节列出的 32 个接口，并补充 1 个认领核验列表查询接口（GET /api/claims），共 33 个，供编码与接口测试直接使用。

## 1 通用约定

### 1.1 基础信息

| 项目 | 约定 |
|---|---|
| 服务地址 | 开发环境 `http://localhost:8080`，前端通过 Vite 代理 `/api` 转发 |
| 接口前缀 | 所有业务接口统一以 `/api` 开头 |
| 数据格式 | 请求与响应均为 `application/json;charset=UTF-8`；上传接口为 `multipart/form-data` |
| 鉴权方式 | 请求头 `Authorization: Bearer <token>`，token 为 HS256 签名的 JWT，有效期 12 小时 |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（字符串）；`happenTime` 也可传 `yyyy-MM-dd'T'HH:mm` |
| 字段命名 | JSON 字段采用小驼峰（camelCase） |
| 文件访问 | 图片以相对路径 `/uploads/yyyyMM/uuid.png` 返回，前端拼接服务地址访问 |

### 1.2 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 200 表示成功；其余为业务错误码（见 1.4） |
| message | string | 成功为 `success`，失败为可直接展示给用户的中文提示 |
| data | object/array/null | 业务数据，无返回值时为 `null` |

HTTP 状态码与业务码配合使用：成功一律返回 HTTP 200（`code=200`），失败按错误类型返回 400/401/403/404/409/500，同时 `code` 给出可区分的业务错误码。

### 1.3 分页约定

请求参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码，从 1 开始，小于 1 时按 1 处理 |
| size | int | 否 | 12 | 每页条数，最大 50，超过按 50 处理 |

响应 `data` 结构：

```json
{
  "total": 268,
  "page": 1,
  "size": 12,
  "pages": 23,
  "records": []
}
```

### 1.4 错误码总表

| 业务码 | HTTP | 含义 | 典型场景 |
|---|---|---|---|
| 200 | 200 | 成功 | — |
| 1001 | 400 | 参数校验失败 | 标题超长、手机号格式错误、必填项为空 |
| 1002 | 400 | 学号已存在 | 注册时学号重复 |
| 1003 | 400 | 账号或密码错误 | 登录失败（不区分账号不存在与密码错误） |
| 1004 | 403 | 账号已被禁用 | 登录或访问受保护接口时 status=1 |
| 1005 | 400 | 原密码错误 | 修改密码 |
| 2001 | 401 | 未登录或登录已过期 | 缺少 token、token 失效 |
| 2002 | 403 | 无访问权限 | 非本人修改他人信息、非管理员访问后台接口 |
| 2003 | 409 | 当前状态不允许该操作 | 对已完成物品发起认领、重复关闭 |
| 2004 | 404 | 资源不存在 | 信息/用户/分类 ID 不存在 |
| 3001 | 400 | 不能认领自己发布的信息 | 认领申请 |
| 3002 | 409 | 该认领申请已处理 | 重复核验 |
| 3003 | 400 | 已提交过认领申请 | 同一用户对同一物品重复申请 |
| 4001 | 400 | 上传文件为空 | 图片上传 |
| 4002 | 400 | 图片超过 2MB | 图片上传 |
| 4003 | 400 | 仅支持 jpg/png 格式 | 图片上传 |
| 4004 | 400 | 最多上传 3 张图片 | 图片上传 |
| 5001 | 400 | 分类名称已存在 | 分类新增/修改 |
| 5002 | 409 | 分类下存在失物信息，禁止删除 | 分类删除 |
| 5003 | 404 | 分类不存在 | 分类修改/删除 |
| 9999 | 500 | 服务器内部错误 | 未捕获异常，日志记录 traceId |

### 1.5 枚举字典

| 枚举 | 取值 |
|---|---|
| 用户角色 role | 0-学生　1-管理员 |
| 用户状态 status | 0-正常　1-禁用 |
| 失物类型 type | 0-寻物　1-拾物 |
| 审核状态 auditStatus | 0-待审核　1-通过　2-驳回 |
| 物品状态 itemStatus | 0-进行中　1-处理中　2-已完成　3-已关闭 |
| 认领状态 claimStatus | 0-待核验　1-通过　2-拒绝 |
| 消息类型 messageType | 1-审核结果　2-留言提醒　3-认领申请提醒　4-认领结果　5-系统通知 |

### 1.6 权限矩阵

| 接口分组 | 游客 | 学生 | 管理员 |
|---|---|---|---|
| 注册、登录 | ✔ | ✔ | ✔ |
| 浏览列表、查看详情、查看留言 | ✔ | ✔ | ✔ |
| 个人资料、修改密码、我的发布/认领/留言 | ✘ | ✔ | ✔ |
| 发布、编辑、关闭信息、图片上传 | ✘ | ✔ | ✔（可关闭任意信息） |
| 发起认领、发表留言 | ✘ | ✔ | ✔ |
| 我的消息 | ✘ | ✔ | ✔ |
| 后台全部接口 | ✘ | ✘ | ✔ |

---

## 2 认证与用户接口（9 个）

### 2.1 学生注册

`POST /api/auth/register`　权限：公开

请求体：

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| studentNo | string | 是 | 6~12 位数字，未被注册 |
| nickname | string | 是 | 2~12 个字符，不含空白 |
| phone | string | 是 | 匹配 `^1[3-9]\d{9}$` |
| password | string | 是 | 6~20 位，至少包含字母与数字 |
| confirmPassword | string | 是 | 必须与 password 一致 |

```json
{
  "studentNo": "2022304512",
  "nickname": "小李同学",
  "phone": "13800006621",
  "password": "abc123456",
  "confirmPassword": "abc123456"
}
```

响应：

```json
{ "code": 200, "message": "success", "data": { "userId": 12 } }
```

业务规则：密码使用 BCrypt（cost=10）加密后入库；`role=0`、`status=0`；注册成功后不自动登录，前端跳转登录页。
错误码：1001 参数不合法、1002 学号已存在。

### 2.2 登录

`POST /api/auth/login`　权限：公开

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| studentNo | string | 是 | 学号/工号 |
| password | string | 是 | 明文密码 |

```json
{ "studentNo": "2022304512", "password": "abc123456" }
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 43200,
    "user": {
      "id": 12,
      "studentNo": "2022304512",
      "nickname": "小李同学",
      "phone": "13800006621",
      "role": 0,
      "status": 0,
      "createdAt": "2026-03-01 09:12:00"
    }
  }
}
```

业务规则：先按学号查用户，用户不存在与密码错误统一返回 1003（防账号枚举）；`status=1` 返回 1004；JWT 载荷 `{uid, role, iat, exp}`，密钥来自 `application.yml` 的 `jwt.secret`。
错误码：1001、1003、1004。

### 2.3 退出登录

`POST /api/auth/logout`　权限：登录用户

请求体：无。响应：`data` 为 `null`。

业务规则：JWT 无状态，服务端仅记录退出日志，实际由前端清除本地 token；若配置了 Redis 黑名单，则将当前 token 写入黑名单至其过期。

### 2.4 获取个人资料

`GET /api/user/profile`　权限：登录用户

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 12, "studentNo": "2022304512", "nickname": "小李同学",
    "phone": "13800006621", "role": 0, "createdAt": "2026-03-01 09:12:00",
    "stat": { "published": 6, "claiming": 3, "claimingPending": 1, "comments": 8 }
  }
}
```

### 2.5 修改个人资料

`PUT /api/user/profile`　权限：登录用户

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| nickname | string | 是 | 2~12 个字符 |
| phone | string | 是 | 11 位手机号 |

```json
{ "nickname": "小李", "phone": "13900001111" }
```

响应：`data` 为更新后的用户对象（同 2.4，不含 stat）。规则：学号、角色、状态不可自行修改。

### 2.6 修改密码

`PUT /api/user/password`　权限：登录用户

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 6~20 位，至少含字母与数字，且不得与原密码相同 |
| confirmPassword | string | 是 | 必须与 newPassword 一致 |

```json
{ "oldPassword": "abc123456", "newPassword": "xyz987654", "confirmPassword": "xyz987654" }
```

响应：`data` 为 `null`。错误码：1001、1005。

### 2.7 我的发布

`GET /api/user/items`　权限：登录用户

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |
| auditStatus | int | 否 | 按审核状态筛选 |
| itemStatus | int | 否 | 按物品状态筛选 |

响应 `records` 元素：

```json
{
  "id": 101, "type": 1, "title": "黑色钱包（含校园卡）",
  "categoryName": "证件", "coverImage": "/uploads/202611/a1.png",
  "auditStatus": 1, "auditStatusText": "已通过",
  "rejectReason": null,
  "itemStatus": 1, "itemStatusText": "处理中",
  "claimCount": 2, "createdAt": "2026-11-12 16:02:00"
}
```

### 2.8 我的认领

`GET /api/user/claims`　权限：登录用户

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |
| status | int | 否 | 0-待核验 1-通过 2-拒绝 |

响应 `records` 元素：

```json
{
  "id": 55, "itemId": 101, "itemTitle": "黑色钱包（含校园卡）",
  "itemStatus": 2, "detailDesc": "内有一卡通，姓名与我一致",
  "status": 1, "statusText": "已通过",
  "reply": "身份已核实，请到图书馆服务台领取",
  "createdAt": "2026-11-13 09:04:00"
}
```

### 2.9 我的留言

`GET /api/user/comments`　权限：登录用户

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |

响应 `records` 元素：

```json
{
  "id": 301, "itemId": 101, "itemTitle": "黑色钱包（含校园卡）",
  "content": "已联系失主，谢谢楼主！", "createdAt": "2026-11-13 10:15:00"
}
```

---

## 3 失物信息接口（4 个）

### 3.1 发布信息

`POST /api/items`　权限：登录用户

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| type | int | 是 | 0-寻物 1-拾物 |
| title | string | 是 | 1~30 字，去除首尾空格后非空 |
| description | string | 否 | ≤500 字 |
| categoryId | long | 是 | 必须存在 |
| happenTime | string | 是 | 不晚于当前时间 + 1 天 |
| happenPlace | string | 是 | 1~100 字 |
| contact | string | 是 | 1~50 字（手机号或微信/QQ） |
| imageUrls | string[] | 否 | ≤3 个，元素须为 `/uploads/` 开头且为本系统返回的路径 |

```json
{
  "type": 1,
  "title": "黑色钱包（含校园卡）",
  "description": "图书馆三楼靠窗座位捡到黑色短款钱包，内有一卡通、少量现金，已交至图书馆服务台。",
  "categoryId": 1,
  "happenTime": "2026-11-12 14:30:00",
  "happenPlace": "图书馆三楼自习区",
  "contact": "13800006621",
  "imageUrls": ["/uploads/202611/a1.png", "/uploads/202611/a2.png"]
}
```

响应：

```json
{ "code": 200, "message": "success", "data": { "id": 101, "auditStatus": 0 } }
```

业务规则：入库时 `audit_status=0`、`item_status=0`；图片按数组顺序写入 `item_image.sort`（0,1,2）；同一请求在事务内完成主表与图片表写入。

### 3.2 编辑信息

`PUT /api/items/{id}`　权限：发布者本人

请求体：同 3.1（`imageUrls` 为提交后**完整**的图片列表，服务端先删后插）。

响应：`data` 为 `{ "id": 101, "auditStatus": 0 }`。

业务规则：
- 仅 `audit_status` 为 0 或 2、且 `item_status` 为 0（进行中）时可编辑；
- 已通过审核的信息编辑后 `audit_status` 重置为 0，需重新审核，同时给发布者推送 system 消息（type=1）；
- `item_status` 为 1/2/3 时返回 2003；
- 非发布者返回 2002；信息不存在返回 2004。

### 3.3 关闭信息（下架）

`PUT /api/items/{id}/close`　权限：发布者本人或管理员

请求体：无。响应：`data` 为 `null`。

业务规则：物品状态须为 0（进行中）或 1（处理中），更新为 3（已关闭）；由处理中关闭时，将同物品所有 `status=0` 的认领申请置为 2（拒绝）并推送消息（type=4）；已关闭/已完成重复调用返回 2003（幂等提示"该信息已关闭"）。

### 3.4 图片上传

`POST /api/upload/image`　权限：登录用户

`multipart/form-data`，字段名 `file`。

校验顺序（命中即返回）：文件非空（4001）→ 大小 ≤ 2MB（4002）→ 扩展名 ∈ {jpg,jpeg,png} 且 MIME ∈ {image/jpeg,image/png}（4003）→ 当前用户本次会话已上传数 < 3（4004）。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": { "url": "/uploads/202611/9f2c1b7e4d.png", "size": 184320, "originalName": "wallet.png" }
}
```

业务规则：存储路径 `{user.dir}/uploads/yyyyMM/{uuid}.{ext}`，目录不存在时先创建；文件名使用 UUID，不使用原始文件名（防路径穿越与重名）；返回相对 URL。

---

## 4 浏览与检索接口（2 个）

### 4.1 失物信息列表

`GET /api/items`　权限：公开

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page / size | int | 否 | 1 / 12 | 分页 |
| type | int | 否 | — | 0-寻物 1-拾物，不传为全部 |
| categoryId | long | 否 | — | 分类筛选 |
| keyword | string | 否 | — | 标题或描述模糊匹配，≤50 字 |
| itemStatus | int | 否 | — | 0/1/2，不传为全部 |
| sort | string | 否 | `time` | `time`-按发布时间倒序；`relevance`-标题命中优先 |

固定条件：`audit_status = 1 AND item_status <> 3`（未通过审核与已关闭的信息不出现在前台）。

响应 `records` 元素：

```json
{
  "id": 101, "type": 1, "typeText": "拾物",
  "title": "黑色钱包（含校园卡）",
  "summary": "图书馆三楼靠窗座位捡到黑色短款钱包…",
  "categoryId": 1, "categoryName": "证件",
  "coverImage": "/uploads/202611/a1.png",
  "happenPlace": "图书馆三楼自习区",
  "itemStatus": 1, "itemStatusText": "处理中",
  "publisherNickname": "小李同学",
  "claimCount": 2,
  "createdAt": "2026-11-12 16:02:00"
}
```

业务规则：`summary` 取描述前 60 字；`coverImage` 取 `sort` 最小的图片，无图为 `null`；`keyword` 存在时按 `title LIKE %kw% OR description LIKE %kw%` 查询，标题命中记录优先排序。

### 4.2 信息详情

`GET /api/items/{id}`　权限：公开（`contact` 字段仅登录用户可见）

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 101, "type": 1, "typeText": "拾物",
    "title": "黑色钱包（含校园卡）",
    "description": "图书馆三楼靠窗座位捡到黑色短款钱包…",
    "categoryId": 1, "categoryName": "证件",
    "happenTime": "2026-11-12 14:30:00",
    "happenPlace": "图书馆三楼自习区",
    "contact": "138****6621",
    "auditStatus": 1, "auditStatusText": "已通过",
    "itemStatus": 1, "itemStatusText": "处理中",
    "publisher": { "id": 12, "nickname": "小李同学", "studentNo": "2022****12" },
    "images": ["/uploads/202611/a1.png", "/uploads/202611/a2.png"],
    "claimCount": 2,
    "commentCount": 3,
    "myClaimStatus": null,
    "createdAt": "2026-11-12 16:02:00"
  }
}
```

业务规则：
- 未登录时 `contact` 返回掩码（中间 4 位用 `*` 代替），`myClaimStatus` 为 `null`；
- 未通过审核（`audit_status<>1`）的信息仅发布者与管理员可见，其他角色返回 2004；
- `myClaimStatus` 为当前登录用户对该物品的认领状态（0/1/2），未申请为 `null`，用于前端按钮置灰与提示。
---

## 5 认领接口（4 个）

### 5.1 发起认领申请

`POST /api/claims`　权限：登录用户

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| itemId | long | 是 | 物品必须存在、已通过审核、非本人发布 |
| detailDesc | string | 是 | 1~300 字，需说明可核验的凭据 |

```json
{ "itemId": 101, "detailDesc": "内有一卡通，姓名与我一致，可提供学号核验" }
```

响应：

```json
{ "code": 200, "message": "success", "data": { "claimId": 55, "itemStatus": 1 } }
```

业务规则（按顺序校验，命中即返回）：

1. 物品不存在或 `audit_status<>1` → 2004；
2. `item_status` 为 2/3 → 2003；
3. 发布者本人 → 3001；
4. 已有申请记录（`uk_claim_item_applicant` 命中）→ 3003；
5. 校验通过：插入 `claim(status=0)`；若物品为进行中则更新为处理中（1）；
6. 给发布者插入消息（type=3），内容含申请人昵称与物品标题。

并发说明：第 4 步唯一索引兜底，捕获 `DuplicateKeyException` 后同样返回 3003。

### 5.2 认领核验列表

`GET /api/claims`　权限：管理员

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |
| status | int | 否 | 0-待核验 1-通过 2-拒绝，默认 0 |
| keyword | string | 否 | 按物品标题或申请人昵称模糊匹配 |

响应 `records` 元素：

```json
{
  "id": 55, "itemId": 101, "itemTitle": "黑色钱包（含校园卡）",
  "applicant": { "id": 13, "nickname": "王同学", "studentNo": "2022304601" },
  "detailDesc": "内有一卡通，姓名与我一致",
  "status": 0, "statusText": "待核验",
  "createdAt": "2026-11-13 09:04:00"
}
```

### 5.3 核验通过

`PUT /api/claims/{id}/approve`　权限：管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reply | string | 否 | 回复说明，≤200 字，如领取地点与时间 |

```json
{ "reply": "身份已核实，请到图书馆服务台领取" }
```

响应：

```json
{ "code": 200, "message": "success", "data": { "claimId": 55, "itemStatus": 2 } }
```

业务规则（单事务，`@Transactional(rollbackFor = Exception.class)`）：

1. 查询认领申请，不存在 → 2004；`status<>0` → 3002；
2. 条件更新物品状态：`UPDATE lost_item SET item_status = 2 WHERE id = ? AND item_status = 1`；
3. 影响行数 ≠ 1 → 抛出业务异常（2003），事务回滚；
4. 更新 `claim`：`status=1`、`admin_id`、`reply`；
5. 同物品其余待核验申请置为 2（拒绝），`reply` 写"该物品已被他人认领"；
6. 向申请人插入消息（type=4，通过），向其余被拒申请人插入消息（type=4，拒绝）；
7. 返回最新物品状态。

### 5.4 核验拒绝

`PUT /api/claims/{id}/reject`　权限：管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reply | string | 是 | 拒绝原因，1~200 字，将展示给申请人 |

```json
{ "reply": "描述与物品特征不符，请补充可核验信息后重新申请" }
```

响应：`data` 为 `{ "claimId": 55, "itemStatus": 0 }`。

业务规则（单事务）：申请不存在 → 2004；`status<>0` → 3002；更新 `claim.status=2`、`admin_id`、`reply`；若物品仍为处理中（1）则退回进行中（0）；向申请人插入消息（type=4）。

---

## 6 留言与消息接口（5 个）

### 6.1 发表留言

`POST /api/comments`　权限：登录用户

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| itemId | long | 是 | 物品存在且已通过审核 |
| content | string | 是 | 1~200 字，去除首尾空格后非空 |

```json
{ "itemId": 101, "content": "这个钱包好像是隔壁宿舍同学的，帮忙转告一下。" }
```

响应：

```json
{ "code": 200, "message": "success", "data": { "id": 301, "createdAt": "2026-11-13 10:15:00" } }
```

业务规则：给物品发布者插入消息（type=2，内容含留言者昵称与留言摘要）；自己给自己留言不推送消息。

### 6.2 删除留言

`DELETE /api/comments/{id}`　权限：留言本人或管理员

响应：`data` 为 `null`。规则：非本人且非管理员 → 2002；不存在 → 2004。

### 6.3 消息列表

`GET /api/messages`　权限：登录用户

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页，默认每页 10 |
| isRead | int | 否 | 0-未读 1-已读，不传为全部 |
| type | int | 否 | 消息类型 1~5 |

响应 `records` 元素：

```json
{
  "id": 900, "type": 3, "typeText": "认领申请",
  "content": "王同学申请认领您发布的《黑色钱包（含校园卡）》，请及时核验。",
  "itemId": 101,
  "fromUserId": 13, "fromNickname": "王同学",
  "isRead": 0,
  "createdAt": "2026-11-13 09:04:00"
}
```

业务规则：仅返回 `to_user_id = 当前用户` 的记录，按时间倒序；`from_user_id=0` 时 `fromNickname` 为"系统"。

### 6.4 标记单条已读

`PUT /api/messages/{id}/read`　权限：登录用户（本人消息）

响应：`data` 为 `{ "id": 900, "isRead": 1 }`。规则：消息不属于当前用户 → 2002；不存在 → 2004；已读重复调用幂等返回成功。

### 6.5 未读消息数

`GET /api/messages/unread-count`　权限：登录用户

响应：

```json
{ "code": 200, "message": "success", "data": { "count": 3 } }
```

业务规则：前端在导航栏每 60 秒轮询一次，用于角标显示。

---

## 7 管理后台接口（9 个）

### 7.1 待审核信息列表

`GET /api/admin/items/audit`　权限：管理员

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |
| auditStatus | int | 否 | 默认 0（待审核），可传 1/2 查看历史 |
| keyword | string | 否 | 标题或发布者昵称模糊匹配 |

响应 `records` 元素：

```json
{
  "id": 101, "title": "黑色钱包（含校园卡）",
  "type": 1, "typeText": "拾物",
  "categoryId": 1, "categoryName": "证件",
  "publisherNickname": "小李同学",
  "description": "图书馆三楼靠窗座位捡到黑色短款钱包…",
  "images": ["/uploads/202611/a1.png"],
  "auditStatus": 0, "auditStatusText": "待审核",
  "createdAt": "2026-11-12 16:02:00"
}
```

### 7.2 审核信息

`PUT /api/admin/items/{id}/audit`　权限：管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| auditStatus | int | 是 | 1-通过 2-驳回 |
| rejectReason | string | 条件必填 | `auditStatus=2` 时必填，1~100 字 |

```json
{ "auditStatus": 2, "rejectReason": "描述过于简单，请补充物品特征与拾获经过" }
```

响应：

```json
{ "code": 200, "message": "success", "data": { "id": 101, "auditStatus": 2, "itemStatus": 0 } }
```

业务规则：仅 `audit_status=0` 的信息可审核，重复审核返回 2003；通过时写 `audit_status=1`、清空 `reject_reason`；驳回时写 `audit_status=2` 与原因，物品状态保持进行中；两种结果均向发布者插入消息（type=1）。

### 7.3 分类列表

`GET /api/admin/categories`　权限：管理员（前台下拉框使用同一数据，可公开调用 `GET /api/categories` 返回不含统计的精简列表）

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 1, "name": "证件", "sort": 1, "itemCount": 86 },
    { "id": 2, "name": "电子产品", "sort": 2, "itemCount": 64 }
  ]
}
```

### 7.4 新增分类

`POST /api/admin/categories`　权限：管理员

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| name | string | 是 | 1~20 字，不与现有分类重名 |
| sort | int | 否 | 默认 0，升序排列 |

响应：`data` 为新建分类对象（含 id）。错误码：1001、5001。

### 7.5 修改分类

`PUT /api/admin/categories/{id}`　权限：管理员

请求体同 7.4。规则：分类不存在 → 5003；重名（排除自身）→ 5001。响应为更新后的分类对象。

### 7.6 删除分类

`DELETE /api/admin/categories/{id}`　权限：管理员

响应：`data` 为 `null`。规则：分类不存在 → 5003；存在任何关联失物信息（含已关闭）→ 5002，并提示"该分类下仍有 N 条失物信息，无法删除"。

### 7.7 用户列表

`GET /api/admin/users`　权限：管理员

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | 分页 |
| keyword | string | 否 | 学号或昵称模糊匹配 |
| role | int | 否 | 0-学生 1-管理员 |
| status | int | 否 | 0-正常 1-禁用 |

响应 `records` 元素：

```json
{
  "id": 12, "studentNo": "2022304512", "nickname": "小李同学",
  "phone": "13800006621", "role": 0, "roleText": "学生",
  "status": 0, "statusText": "正常",
  "itemCount": 6, "claimCount": 3,
  "createdAt": "2026-03-01 09:12:00"
}
```

### 7.8 启用/禁用用户

`PUT /api/admin/users/{id}/status`　权限：管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | int | 是 | 0-正常 1-禁用 |

```json
{ "status": 1 }
```

响应：`data` 为 `{ "id": 12, "status": 1 }`。

业务规则：被禁用账号登录返回 1004，已签发的 token 在拦截器中校验 `status` 时即刻失效；不允许禁用自己；不允许禁用其他管理员账号（返回 2002）。

### 7.9 数据统计

`GET /api/admin/stats`　权限：管理员

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| days | int | 否 | 趋势统计天数，默认 7，范围 7~30 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalItems": 268,
    "pendingAudit": 12,
    "completedClaims": 146,
    "returnRate": 72.4,
    "categoryStats": [
      { "categoryId": 1, "categoryName": "证件", "itemCount": 86, "completedCount": 62, "returnRate": 72.1 }
    ],
    "trend": [
      { "date": "2026-11-06", "count": 12 },
      { "date": "2026-11-07", "count": 18 }
    ]
  }
}
```

指标口径：
- `totalItems`：`lost_item` 全表记录数（含各状态）；
- `pendingAudit`：`audit_status = 0` 的记录数；
- `completedClaims`：`claim.status = 1` 的记录数；
- `returnRate`：`item_status = 2` 的拾物数 ÷ `audit_status = 1` 的拾物总数 × 100，保留 1 位小数，分母为 0 时返回 0；
- `trend`：按 `DATE(created_at)` 分组统计区间内每日发布量，无数据的日期补 0，日期升序。

---

## 8 接口与程序单元、需求对照

| 序号 | 接口 | 程序单元 | 关联需求 |
|---|---|---|---|
| 1 | POST /api/auth/register | U1.1 | FR-01 |
| 2 | POST /api/auth/login | U1.2 | FR-02 |
| 3 | POST /api/auth/logout | U1.3 | FR-02 |
| 4 | GET /api/user/profile | U1.4 | FR-03 |
| 5 | PUT /api/user/profile | U1.4 | FR-03 |
| 6 | PUT /api/user/password | U1.5 | FR-03 |
| 7 | GET /api/user/items | U1.6 | FR-03、FR-06 |
| 8 | GET /api/user/claims | U1.6 | FR-03、FR-13 |
| 9 | GET /api/user/comments | U1.6 | FR-03、FR-16 |
| 10 | POST /api/items | U2.1 | FR-05 |
| 11 | PUT /api/items/{id} | U2.3 | FR-06 |
| 12 | PUT /api/items/{id}/close | U4.5 | FR-15 |
| 13 | POST /api/upload/image | U2.2 | FR-07 |
| 14 | GET /api/items | U3.1、U3.2 | FR-08、FR-09、FR-10 |
| 15 | GET /api/items/{id} | U3.3 | FR-11 |
| 16 | POST /api/claims | U4.2 | FR-13 |
| 17 | GET /api/claims | U4.3、U4.4 | FR-14 |
| 18 | PUT /api/claims/{id}/approve | U4.3、U4.1 | FR-14、FR-12 |
| 19 | PUT /api/claims/{id}/reject | U4.4、U4.1 | FR-14、FR-12 |
| 20 | POST /api/comments | U5.1 | FR-16 |
| 21 | DELETE /api/comments/{id} | U5.2 | FR-16 |
| 22 | GET /api/messages | U5.4 | FR-17 |
| 23 | PUT /api/messages/{id}/read | U5.4 | FR-17 |
| 24 | GET /api/messages/unread-count | U5.4 | FR-17 |
| 25 | GET /api/admin/items/audit | U6.1 | FR-18 |
| 26 | PUT /api/admin/items/{id}/audit | U6.1 | FR-18 |
| 27 | GET /api/admin/categories | U6.2 | FR-19 |
| 28 | POST /api/admin/categories | U6.2 | FR-19 |
| 29 | PUT /api/admin/categories/{id} | U6.2 | FR-19 |
| 30 | DELETE /api/admin/categories/{id} | U6.2 | FR-19 |
| 31 | GET /api/admin/users | U6.3、U1.7 | FR-04 |
| 32 | PUT /api/admin/users/{id}/status | U1.7 | FR-04 |
| 33 | GET /api/admin/stats | U6.4 | FR-20 |