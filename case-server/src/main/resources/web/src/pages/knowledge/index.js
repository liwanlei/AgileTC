import React from 'react';
import { Table, Button, Card, Tag, message, Modal, Spin, Drawer } from 'antd';
import router from 'umi/router';
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
    request('/case/knowledge/bases', {
      method: 'GET'
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
    request(`/case/knowledge/bases/${kbId}`, {
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
            <Button icon="home" onClick={() => router.push(`/case/caseList/${this.props.match.params.productId}`)}>
              返回首页
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
