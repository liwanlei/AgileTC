# 知识库集成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 AgileTC 测试用例管理平台中集成知识库 API，支持知识提取、知识库查看、刷新，以及 AI 用例生成时关联知识库。

**Architecture:** 采用后端代理转发模式，新建 KnowledgeController 代理所有知识库请求到 http://127.0.0.1:8001，前端新增知识库页面和 AI 生成弹窗中的知识库选择器。

**Tech Stack:** Java Spring Boot (后端), React + Antd + umi (前端), HttpURLConnection (代理转发)

---

### Task 1: 后端配置 — 添加知识库服务 URL 配置

**Files:**
- Modify: `src/main/resources/application-dev.properties`

- [ ] **Step 1: 在 application-dev.properties 末尾添加知识库服务配置**

在现有 `ai.service.url` 配置下方添加：

```properties
# 知识库服务配置
knowledge.service.url=http://127.0.0.1:8001
```

- [ ] **Step 2: 验证配置文件格式正确**

确认没有多余空格或换行问题。

---

### Task 2: 后端 — 创建 KnowledgeController 代理控制器

**Files:**
- Create: `src/main/java/com/xiaoju/framework/controller/KnowledgeController.java`

- [ ] **Step 1: 创建 KnowledgeController.java**

```java
package com.xiaoju.framework.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/api/case/knowledge")
public class KnowledgeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeController.class);

    @Value("${knowledge.service.url}")
    private String knowledgeServiceUrl;

    /**
     * 代理转发请求到知识库服务
     * 支持 GET/POST/PUT 方法，路径为 /{subPath}
     */
    @RequestMapping(value = "/{subPath}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public void proxyKnowledgeRequest(
            @PathVariable String subPath,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String targetUrl = knowledgeServiceUrl + "/api/knowledge/" + subPath;
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                targetUrl += "?" + queryString;
            }

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod());
            conn.setDoOutput(true);
            conn.setReadTimeout(120000);
            conn.setConnectTimeout(10000);

            // 转发 Content-Type
            String contentType = request.getContentType();
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }

            // 转发请求体（POST/PUT）
            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                try (InputStream inputStream = request.getInputStream();
                     OutputStream os = conn.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
            }

            // 设置响应状态码
            response.setStatus(conn.getResponseCode());
            String responseContentType = conn.getContentType();
            if (responseContentType != null) {
                response.setContentType(responseContentType);
            }

            // 转发响应体
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()
            ));
                 PrintWriter writer = response.getWriter()) {
                String line;
                while ((line = br.readLine()) != null) {
                    writer.write(line);
                }
                writer.flush();
            }

        } catch (Exception e) {
            LOGGER.error("[Knowledge Proxy] Proxy request failed. subPath={}, e={} ", subPath, e.getMessage());
            response.setStatus(500);
            response.setContentType("application/json");
            try (PrintWriter writer = response.getWriter()) {
                writer.write("{\"error\": \"知识库服务请求失败\"}");
                writer.flush();
            } catch (IOException ex) {
                LOGGER.error("[Knowledge Proxy] Write error response failed", ex);
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

检查 import 路径是否正确，特别是 `Response`、`StatusCode` 等类的包路径。参考 `CaseController.java` 中的 import。

---

### Task 3: 后端 — CaseController.aiRun 增加 knowledge_base_id 参数

**Files:**
- Modify: `src/main/java/com/xiaoju/framework/controller/CaseController.java`

- [ ] **Step 1: 在 aiRun 方法签名中添加 knowledge_base_id 参数**

将方法签名从：

```java
@PostMapping(value = "/aiRun")
public void aiRun(
        @RequestParam(required = false) String task,
        @RequestParam(required = false) String doc_url,
        @RequestParam(required = false) MultipartFile file,
        @RequestParam Long caseId,
        HttpServletResponse response
) {
```

改为：

```java
@PostMapping(value = "/aiRun")
public void aiRun(
        @RequestParam(required = false) String task,
        @RequestParam(required = false) String doc_url,
        @RequestParam(required = false) MultipartFile file,
        @RequestParam(required = false) String knowledge_base_id,
        @RequestParam Long caseId,
        HttpServletResponse response
) {
```

- [ ] **Step 2: 在转发请求时添加 knowledge_base_id 参数**

在 `// 传递 caseId 给 AI 服务` 代码块之后、task 参数之前，添加：

```java
// 传递 knowledge_base_id 给 AI 服务（可选）
if (knowledge_base_id != null && !knowledge_base_id.isEmpty()) {
    os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    os.write("Content-Disposition: form-data; name=\"knowledge_base_id\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
    os.write(knowledge_base_id.getBytes(StandardCharsets.UTF_8));
    os.write("\r\n".getBytes(StandardCharsets.UTF_8));
}
```

- [ ] **Step 3: 验证修改位置正确**

确认代码插入在 `OutputStream os = conn.getOutputStream();` 之后，且在 `if (task != null ...)` 之前。

---

### Task 4: 前端 — 创建知识库页面

**Files:**
- Create: `src/main/resources/web/src/pages/knowledge/index.js`
- Create: `src/main/resources/web/src/pages/knowledge/index.scss`

- [ ] **Step 1: 创建知识库页面组件 index.js**

```javascript
import React from 'react';
import { Table, Button, Card, Tag, message, Modal, Spin, Drawer } from 'antd';
import request from '@/utils/axios';
import './index.scss';

class KnowledgePage extends React.Component {
  state = {
    loading: false,
    knowledgeBases: [],
    detailLoading: false,
    detailVisible: false,
    currentDetail: null,
  };

  componentDidMount() {
    this.fetchKnowledgeBases();
  }

  fetchKnowledgeBases = () => {
    this.setState({ loading: true });
    request('/api/case/knowledge/bases', {
      method: 'GET',
      params: { status: 'all' },
    })
      .then(res => {
        this.setState({ knowledgeBases: res.items || [], loading: false });
      })
      .catch(() => {
        message.error('获取知识库列表失败');
        this.setState({ loading: false });
      });
  };

  fetchDetail = kbId => {
    this.setState({ detailLoading: true, detailVisible: true });
    request(`/api/case/knowledge/bases/${kbId}`, {
      method: 'GET',
    })
      .then(res => {
        this.setState({ currentDetail: res, detailLoading: false });
      })
      .catch(() => {
        message.error('获取知识库详情失败');
        this.setState({ detailLoading: false });
      });
  };

  handleRefresh = record => {
    Modal.confirm({
      title: '确认刷新',
      content: `确定要刷新知识库「${record.title}」吗？这将检测用例变更并增量更新。`,
      onOk: () => {
        request(`/api/case/knowledge/bases/${record.id}/refresh`, {
          method: 'PUT',
          body: {},
        })
          .then(res => {
            message.success(
              res.updated_count > 0
                ? `知识库已更新，变更 ${res.updated_count} 条知识点`
                : '未检测到变更',
            );
            this.fetchKnowledgeBases();
          })
          .catch(() => {
            message.error('刷新知识库失败');
          });
      },
    });
  };

  handleExtract = () => {
    message.info('请在用例列表选择用例后点击「知识提取」按钮');
  };

  columns = [
    {
      title: '知识库标题',
      dataIndex: 'title',
      key: 'title',
      width: 300,
    },
    {
      title: '模块',
      dataIndex: 'modules',
      key: 'modules',
      render: modules =>
        modules && modules.length > 0 ? (
          modules.map(m => <Tag key={m}>{m}</Tag>)
        ) : (
          <Tag>无</Tag>
        ),
    },
    {
      title: '知识点数量',
      dataIndex: 'item_count',
      key: 'item_count',
      width: 120,
    },
    {
      title: '创建时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 200,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (text, record) => (
        <div>
          <Button type="link" size="small" onClick={() => this.fetchDetail(record.id)}>
            查看详情
          </Button>
          <Button type="link" size="small" onClick={() => this.handleRefresh(record)}>
            刷新
          </Button>
        </div>
      ),
    },
  ];

  renderDetail = () => {
    const { currentDetail, detailLoading } = this.state;
    if (!currentDetail) return null;

    return (
      <div className="knowledge-detail">
        <Spin spinning={detailLoading}>
          <h3>{currentDetail.title}</h3>
          {currentDetail.description && <p>{currentDetail.description}</p>}

          <h4>知识点 ({currentDetail.items?.length || 0})</h4>
          <Table
            dataSource={currentDetail.items || []}
            columns={[
              { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
              { title: '类型', dataIndex: 'item_type', key: 'item_type', width: 100 },
              { title: '内容', dataIndex: 'content', key: 'content' },
              {
                title: '优先级',
                dataIndex: 'priority',
                key: 'priority',
                width: 80,
                render: p => <Tag color={p === 'P0' ? 'red' : p === 'P1' ? 'orange' : 'blue'}>{p}</Tag>,
              },
            ]}
            rowKey="id"
            pagination={{ pageSize: 10 }}
            size="small"
          />

          {currentDetail.changes && currentDetail.changes.length > 0 && (
            <>
              <h4>变更 ({currentDetail.changes.length})</h4>
              <Table
                dataSource={currentDetail.changes}
                columns={[
                  { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
                  { title: '新内容', dataIndex: 'content', key: 'content' },
                  { title: '旧内容', dataIndex: 'old_content', key: 'old_content' },
                  { title: '变更原因', dataIndex: 'change_reason', key: 'change_reason' },
                ]}
                rowKey="id"
                pagination={{ pageSize: 10 }}
                size="small"
              />
            </>
          )}
        </Spin>
      </div>
    );
  };

  render() {
    const { loading, knowledgeBases, detailVisible } = this.state;

    return (
      <div className="knowledge-page">
        <Card
          title="知识库管理"
          extra={
            <Button type="primary" onClick={this.handleExtract}>
              知识提取
            </Button>
          }
        >
          <Table
            columns={this.columns}
            dataSource={knowledgeBases}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 20 }}
          />
        </Card>

        <Drawer
          title="知识库详情"
          width={800}
          visible={detailVisible}
          onClose={() => this.setState({ detailVisible: false, currentDetail: null })}
        >
          {this.renderDetail()}
        </Drawer>
      </div>
    );
  }
}

export default KnowledgePage;
```

- [ ] **Step 2: 创建样式文件 index.scss**

```scss
.knowledge-page {
  padding: 20px;

  .knowledge-detail {
    h3 {
      margin-bottom: 12px;
    }

    h4 {
      margin-top: 24px;
      margin-bottom: 12px;
    }

    p {
      color: #666;
      margin-bottom: 16px;
    }
  }
}
```

---

### Task 5: 前端 — 添加知识库路由

**Files:**
- Modify: `src/main/resources/web/.umirc.js`

- [ ] **Step 1: 在 routes 数组中添加知识库路由**

在 `/history/:caseId` 路由之后添加：

```javascript
{
  path: '/knowledge/:productId',
  component: './knowledge/index.js',
},
```

完整路由数组应包含此新路由。

---

### Task 6: 前端 — caseModal.js 增加知识库选择器

**Files:**
- Modify: `src/main/resources/web/src/components/case/caselist/caseModal.js`

- [ ] **Step 1: 在 constructor 的 state 中添加 knowledgeBaseList 和 selectedKnowledgeBaseId**

在 `aiLoading: false,` 之后添加：

```javascript
knowledgeBaseList: [],
selectedKnowledgeBaseId: '',
```

- [ ] **Step 2: 在 componentDidMount 中加载知识库列表**

在 `this.getCardTree();` 之后添加：

```javascript
this.fetchKnowledgeBases();
```

- [ ] **Step 3: 添加 fetchKnowledgeBases 方法**

在 `getRequirementsById` 方法之后添加：

```javascript
fetchKnowledgeBases = () => {
  request('/api/case/knowledge/bases', {
    method: 'GET',
    params: { status: 'active' },
  })
    .then(res => {
      this.setState({ knowledgeBaseList: res.items || [] });
    })
    .catch(err => {
      console.error('Fetch knowledge bases error:', err);
    });
};
```

- [ ] **Step 4: 在 handleAiGenerate 方法中传递 knowledge_base_id**

找到构建 FormData 的代码：

```javascript
const formData = new FormData();
if (aiTask) formData.append('task', aiTask);
if (aiDocUrl) formData.append('doc_url', aiDocUrl);
if (aiFile) formData.append('file', aiFile);
formData.append('caseId', caseId);
```

在 `formData.append('caseId', caseId);` 之后添加：

```javascript
if (this.state.selectedKnowledgeBaseId) {
  formData.append('knowledge_base_id', this.state.selectedKnowledgeBaseId);
}
```

- [ ] **Step 5: 在 AI 生成模式的表单中添加知识库选择器**

在 AI 模式的「上传文档」Row 之后（`</div>` 关闭标签之后、`</div>` 关闭 `ai-generate-mode` 之前），添加：

```javascript
<Form.Item {...formItemLayout} label="关联知识库：">
  <Select
    placeholder="选择已有知识库（可选）"
    allowClear
    value={this.state.selectedKnowledgeBaseId}
    onChange={value => this.setState({ selectedKnowledgeBaseId: value })}
    style={{ width: '100%' }}
  >
    {this.state.knowledgeBaseList.map(kb => (
      <Select.Option key={kb.id} value={kb.id}>
        {kb.title} ({kb.item_count} 个知识点)
      </Select.Option>
    ))}
  </Select>
</Form.Item>
```

- [ ] **Step 6: 在文件顶部 import Select 组件**

将 antd 的 import 中添加 `Select`：

```javascript
import {
  Upload,
  Form,
  message,
  Modal,
  Input,
  Icon,
  Row,
  Col,
  TreeSelect,
  Radio,
  Select,
} from 'antd';
```

---

### Task 7: 前端 — 列表页增加知识提取按钮

**Files:**
- Modify: `src/main/resources/web/src/components/case/caselist/index.js`
- Modify: `src/main/resources/web/src/components/case/caselist/list.js`

- [ ] **Step 1: 在 index.js 的 constructor state 中添加 selectedCaseIds 和 extractLoading**

确认文件顶部已 import `router from 'umi/router'`（已有）。

在 `pollingTimer: null,` 之后添加：

```javascript
selectedCaseIds: [],
extractLoading: false,
```

- [ ] **Step 2: 在 index.js 中添加知识提取处理方法**

在 `stopPolling` 方法之后、`filterHandler` 方法之前添加：

```javascript
handleKnowledgeExtract = () => {
  const { selectedCaseIds } = this.state;
  if (!selectedCaseIds || selectedCaseIds.length === 0) {
    message.warning('请先选择至少一个用例');
    return;
  }

  this.setState({ extractLoading: true });
  request('/api/case/knowledge/extract', {
    method: 'POST',
    body: { case_ids: selectedCaseIds.map(String) },
  })
    .then(res => {
      message.success('知识提取任务已提交');
      this.setState({ extractLoading: false, selectedCaseIds: [] });
      this.pollKnowledgeTask(res.task_id);
    })
    .catch(() => {
      message.error('知识提取任务提交失败');
      this.setState({ extractLoading: false });
    });
};

pollKnowledgeTask = taskId => {
  const poll = () => {
    request(/case/knowledge/tasks/${taskId}`, {
      method: 'GET',
    })
      .then(res => {
        if (res.status === 'success') {
          message.success('知识提取完成');
          const { productLineId } = this.state;
          router.push(`/knowledge/${productLineId}`);
        } else if (res.status === 'failed') {
          message.error('知识提取失败');
        } else {
          setTimeout(poll, 3000);
        }
      })
      .catch(() => {
        setTimeout(poll, 3000);
      });
  };
  setTimeout(poll, 3000);
};

handleCaseSelectChange = ids => {
  this.setState({ selectedCaseIds: ids });
};
```

- [ ] **Step 3: 在 index.js 的 render 中添加知识提取按钮**

找到「新建用例集」按钮所在的 `<Col xs={6} className="text-right">` 区域，在其内部、`<Button type="primary" ...> 新建用例集</Button>` 之前添加：

```javascript
<Button
  type="primary"
  icon="database"
  loading={this.state.extractLoading}
  onClick={this.handleKnowledgeExtract}
  style={{ marginRight: 16 }}
>
  知识提取
</Button>
```

- [ ] **Step 4: 将 selectedCaseIds 和 onSelectChange 传递给 List 组件**

在 `<List>` 组件的 props 中添加：

```javascript
<List
  ...existing props...
  selectedCaseIds={this.state.selectedCaseIds}
  onCaseSelectChange={this.handleCaseSelectChange}
></List>
```

- [ ] **Step 5: 在 list.js 中添加行选择功能**

在 `list.js` 的 constructor state 中添加：

```javascript
selectedRowKeys: [],
```

在 `componentWillReceiveProps` 中同步 props：

```javascript
if (this.props.selectedCaseIds !== nextProps.selectedCaseIds) {
  this.setState({ selectedRowKeys: nextProps.selectedCaseIds || [] });
}
```

在 `render()` 方法中，`<Table>` 组件添加 `rowSelection` prop。在 `<Table` 标签内添加：

```javascript
rowSelection={{
  selectedRowKeys: this.state.selectedRowKeys,
  onChange: selectedRowKeys => {
    this.setState({ selectedRowKeys });
    if (this.props.onCaseSelectChange) {
      this.props.onCaseSelectChange(selectedRowKeys);
    }
  },
}}
```

---

### Task 8: 验证与测试

**Files:**
- All modified files

- [ ] **Step 1: 后端编译验证**

确保所有 Java 文件编译通过，无语法错误。

- [ ] **Step 2: 前端启动验证**

启动前端开发服务器，确认：
- 知识库页面可访问 (`/knowledge/:productId`)
- AI 用例生成弹窗中显示知识库选择器
- 列表页显示知识提取按钮

- [ ] **Step 3: 端到端流程验证**

1. 提交知识提取任务 → 轮询完成 → 跳转知识库页面
2. 查看知识库详情 → 显示知识点和变更
3. 刷新知识库 → 显示更新结果
4. AI 用例生成时选择知识库 → 提交成功
