# 知识库集成设计文档

## 概述

在 AgileTC 测试用例管理平台中集成第三方知识库 API（AITestCraft），支持从测试用例逆向归纳产品需求知识点，并在 AI 用例生成时利用已有知识库提供上下文参考。

## 需求

1. **知识库总结入口** — 用户可从列表页或详情页选择用例，提交知识提取任务
2. **知识库更新入口** — 支持对已有知识库进行增量刷新
3. **知识库独立页面** — 查看知识库列表、详情、变更内容
4. **AI 用例生成关联知识库** — 生成用例时可选择已有知识库，将 `knowledge_base_id` 传递给 AI 服务

## 架构设计

### 后端代理模式

采用后端代理转发方式，与现有 `aiRun` 代理模式保持一致：

```
前端 → /api/case/knowledge/* → KnowledgeController → http://127.0.0.1:8001/api/knowledge/*
```

### 数据流

```
知识提取:
用户勾选用例 → 前端提交 /api/case/knowledge/extract → 后端转发到 /api/knowledge/extract
→ 轮询任务状态 → 获取 knowledge_base_id

知识库刷新:
用户点击刷新 → 前端提交 PUT /api/case/knowledge/bases/{kb_id}/refresh → 后端转发

AI 用例生成:
用户填写需求 + 选择知识库 → 前端提交 /api/case/aiRun (含 knowledge_base_id)
→ 后端转发到 /run (含 knowledge_base_id) → AI 服务生成用例
```

## 后端设计

### 1. 配置文件

`application-dev.properties` 新增：

```properties
# 知识库服务配置
knowledge.service.url=http://127.0.0.1:8001
```

### 2. KnowledgeController

新建 `KnowledgeController.java`，代理转发所有知识库请求：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/case/knowledge/extract` | 提交知识提取任务 |
| GET | `/api/case/knowledge/tasks/{task_id}` | 查询任务状态 |
| GET | `/api/case/knowledge/tasks/{task_id}/result` | 获取任务结果 |
| GET | `/api/case/knowledge/bases` | 查询知识库列表 |
| GET | `/api/case/knowledge/bases/{kb_id}` | 查询知识库详情 |
| PUT | `/api/case/knowledge/bases/{kb_id}/refresh` | 刷新知识库 |

实现方式：使用 `HttpURLConnection` 代理转发请求，与 `CaseController.aiRun` 类似。

### 3. CaseController.aiRun 改动

在现有 `aiRun` 方法中增加 `knowledge_base_id` 参数：

```java
@RequestParam(required = false) String knowledge_base_id
```

转发时将此参数添加到 multipart/form-data 中。

## 前端设计

### 1. 知识库页面

新建 `src/pages/knowledge/index.js`：

- 知识库列表展示（标题、模块、知识点数量、创建时间）
- 点击查看详情（Markdown 内容、知识点列表、变更列表）
- 刷新按钮（增量更新）
- 知识提取任务提交入口

### 2. 路由配置

`.umirc.js` 新增路由：

```javascript
{
  path: '/knowledge/:productId',
  component: '../pages/knowledge',
}
```

### 3. caseModal.js 改动

AI 生成模式下新增知识库选择器：

- 调用 `/api/case/knowledge/bases` 获取知识库列表
- 下拉选择器，用户可选择已有知识库
- 提交时将 `knowledge_base_id` 附加到 FormData

### 4. 列表页改动

- 新增「知识提取」按钮（需勾选至少一个用例）
- 点击后调用 `/api/case/knowledge/extract` 提交任务
- 任务提交后轮询状态，完成后跳转至知识库详情页

### 5. 详情页改动

- 新增「知识提取」按钮
- 点击后提交当前用例 ID 进行知识提取

## 错误处理

- 代理转发失败时返回 500 错误
- 知识库 API 返回 404 时前端提示「资源不存在」
- 任务提交失败时前端提示「知识提取任务提交失败」
- 网络超时时返回友好错误提示

## 配置说明

知识库服务地址通过配置文件灵活调整：

```properties
knowledge.service.url=http://127.0.0.1:8001
```
