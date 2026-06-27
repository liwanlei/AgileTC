# AITestCraft 知识提取 API 文档

## 概述

AITestCraft 知识提取 API 提供从测试用例逆向归纳产品需求知识点的功能，支持多个用例汇总需求，为后续用例设计提供知识库支持。

**核心特性：**
- 从测试用例提取需求知识点
- 支持增量更新（检测用例变更）
- 基于用例ID的冲突解决机制
- Markdown + 结构化 JSON 双格式存储

## 接口列表

| HTTP方法 | 路径 | 功能描述 |
|----------|------|----------|
| POST | `/api/knowledge/extract` | 提交知识提取任务 |
| GET | `/api/knowledge/tasks/{task_id}` | 查询任务状态 |
| GET | `/api/knowledge/tasks/{task_id}/result` | 获取任务结果 |
| GET | `/api/knowledge/bases` | 查询知识库列表 |
| GET | `/api/knowledge/bases/{kb_id}` | 查询知识库详情 |
| PUT | `/api/knowledge/bases/{kb_id}/refresh` | 刷新知识库（增量更新） |

---

## 接口详细说明

### 1. 提交知识提取任务

**请求**

```http
POST /api/knowledge/extract
Content-Type: application/json

{
  "case_ids": ["TC001", "TC002", "TC003"]
}
```

**参数说明**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| case_ids | array[string] | 是 | 测试用例ID列表 |

**成功响应** (200 OK)

```json
{
  "task_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**失败响应** (400 Bad Request)

```json
{
  "detail": "用例 ID 列表不能为空"
}
```

---

### 2. 查询任务状态

**请求**

```http
GET /api/knowledge/tasks/{task_id}
```

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| task_id | string | 任务ID |

**成功响应** (200 OK)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "task": "{\"case_ids\": [\"TC001\", \"TC002\"]}",
  "status": "success",
  "result": "{\"knowledge_base_id\": \"kb_550e8400\"}",
  "task_type": "knowledge",
  "created_at": "2026-06-13T10:30:00+00:00",
  "updated_at": "2026-06-13T10:35:00+00:00"
}
```

**状态说明**

| 状态 | 说明 |
|------|------|
| pending | 任务等待执行 |
| running | 任务执行中 |
| success | 任务执行成功 |
| failed | 任务执行失败 |

**失败响应** (404 Not Found)

```json
{
  "detail": "任务不存在"
}
```

---

### 3. 获取任务结果

**请求**

```http
GET /api/knowledge/tasks/{task_id}/result
```

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| task_id | string | 任务ID |

**成功响应** (200 OK)

```json
{
  "knowledge_base_id": "kb_550e8400"
}
```

**失败响应** (404 Not Found)

```json
{
  "detail": "任务不存在或无结果"
}
```

---

### 4. 查询知识库列表

**请求**

```http
GET /api/knowledge/bases?status=active
```

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| status | string | 否 | active | 状态筛选：active / all |

**成功响应** (200 OK)

```json
{
  "total": 3,
  "items": [
    {
      "id": "kb_550e8400",
      "title": "需求知识点 (5个用例)",
      "modules": ["登录模块", "用户管理"],
      "item_count": 15,
      "created_at": "2026-06-13T10:30:00+00:00"
    }
  ]
}
```

---

### 5. 查询知识库详情

**请求**

```http
GET /api/knowledge/bases/{kb_id}
```

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| kb_id | string | 知识库ID |

**成功响应** (200 OK)

```json
{
  "id": "kb_550e8400",
  "title": "需求知识点 (5个用例)",
  "description": "",
  "markdown_content": "# 需求知识库\\n\\n## 登录模块\\n\\n...",
  "status": "active",
  "created_at": "2026-06-13T10:30:00+00:00",
  "updated_at": "2026-06-13T10:35:00+00:00",
  "items": [
    {
      "id": "kb_550e8400_login_xxx",
      "module": "登录模块",
      "item_type": "功能需求",
      "content": "用户可以通过手机号+验证码进行登录",
      "priority": "P0",
      "source_case_ids": ["TC001"],
      "status": "active",
      "created_at": "2026-06-13T10:32:00+00:00"
    }
  ],
  "changes": [
    {
      "id": "kb_550e8400_payment_yyy",
      "module": "支付模块",
      "item_type": "功能需求",
      "content": "支持微信支付",
      "priority": "P1",
      "source_case_ids": ["TC005"],
      "status": "changed",
      "old_content": "支持支付宝支付",
      "change_reason": "用例 TC005 更新",
      "created_at": "2026-06-13T10:34:00+00:00"
    }
  ]
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 知识库ID |
| title | string | 知识库标题 |
| description | string | 描述 |
| markdown_content | string | Markdown格式内容 |
| status | string | 状态：active |
| created_at | string | 创建时间 |
| updated_at | string | 更新时间 |
| items | array | 活跃的知识点列表 |
| changes | array | 变更的知识点列表 |

**知识点字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 知识点ID |
| module | string | 所属模块 |
| item_type | string | 类型：功能需求/业务规则/边界条件等 |
| content | string | 内容 |
| priority | string | 优先级：P0/P1/P2 |
| source_case_ids | array | 来源用例ID |
| status | string | 状态：active/changed |
| old_content | string | 变更前内容（仅status=changed时有值） |
| change_reason | string | 变更原因（仅status=changed时有值） |

**失败响应** (404 Not Found)

```json
{
  "detail": "知识库不存在"
}
```

---

### 6. 刷新知识库（增量更新）

**请求**

```http
PUT /api/knowledge/bases/{kb_id}/refresh
Content-Type: application/json

{
  "case_ids": ["TC001", "TC003"]
}
```

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| kb_id | string | 知识库ID |

**请求体参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| case_ids | array[string] | 否 | 指定要检查的用例ID，不指定则检查所有源用例 |

**成功响应** (200 OK)

```json
{
  "success": true,
  "message": "知识库已更新",
  "updated_count": 2,
  "changed_cases": ["TC001", "TC003"]
}
```

**无变更响应** (200 OK)

```json
{
  "success": true,
  "message": "未检测到变更的用例",
  "updated_count": 0,
  "changed_cases": []
}
```

**失败响应** (404 Not Found)

```json
{
  "detail": "知识库不存在"
}
```

---

## 使用流程示例

### 完整流程：提取知识 → 查询结果 → 使用知识库

```bash
# 1. 提交知识提取任务
curl -X POST http://localhost:8000/api/knowledge/extract \
  -H "Content-Type: application/json" \
  -d '{"case_ids": ["TC001", "TC002", "TC003"]}'

# 响应: {"task_id": "550e8400-e29b-41d4-a716-446655440000"}

# 2. 查询任务状态
curl http://localhost:8000/api/knowledge/tasks/550e8400-e29b-41d4-a716-446655440000

# 3. 获取任务结果
curl http://localhost:8000/api/knowledge/tasks/550e8400-e29b-41d4-a716-446655440000/result

# 响应: {"knowledge_base_id": "kb_550e8400"}

# 4. 查询知识库详情
curl http://localhost:8000/api/knowledge/bases/kb_550e8400

# 5. 后续：将知识库ID传入用例设计接口
curl -X POST http://localhost:8000/run \
  -F "task=新需求描述" \
  -F "knowledge_base_id=kb_550e8400"
```

---

## 冲突解决机制

当同一模块同一场景描述出现矛盾时，系统采用以下策略：

1. **版本判断**：比较用例ID的数值大小
2. **覆盖规则**：用例ID大的覆盖用例ID小的
3. **变更记录**：被覆盖的旧知识点标记为 `status=changed`，保留 `old_content` 和 `change_reason`

**示例**：
- TC001（旧）："用户登录需要输入验证码"
- TC005（新）："用户登录需要输入图形验证码"

结果：
- TC005 的知识点：`status=active`，内容为"用户登录需要输入图形验证码"
- TC001 的知识点：`status=changed`，`old_content="用户登录需要输入验证码"`

---

## 错误码说明

| HTTP状态码 | 说明 |
|-----------|------|
| 400 | 请求参数错误 |
| 404 | 资源不存在（任务/知识库） |
| 500 | 服务器内部错误 |

---

## 注意事项

1. **异步执行**：知识提取任务为异步执行，提交后需轮询任务状态获取结果
2. **用例ID格式**：建议使用统一格式（如 TCxxx），便于冲突解决时比较大小
3. **增量更新**：使用 `/refresh` 接口检测用例变更，避免全量重新提取
4. **知识库关联**：知识库可通过 `knowledge_base_id` 参数关联到用例设计接口，为新需求提供上下文参考