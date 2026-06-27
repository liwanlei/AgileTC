/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import router from 'umi/router';
import request from '@/utils/axios';
import { Row, Button, Col, Icon, Form, message } from 'antd';
import './index.scss';
import _ from 'lodash';
import CaseModal from './caseModal.js';
import List from './list.js';
import Filter from './filter.js';
import OeFilter from './oefilter';
import FileTree from './tree';
class CaseLists extends React.Component {
  static propTypes = {
    form: PropTypes.any,
    productId: PropTypes.any,
    updateCallBack: PropTypes.any,
    users: PropTypes.any,
  };
  constructor(props) {
    super(props);
    this.state = {
      list: [],
      total: 0, // 数据条数
      record: {},
      title: '',
      visible: false,
      iterationList: [], // 需求列表
      showFilterBox: false, // 展示筛选框
      productMember: [], // 所有人
      currCase: null, // 当前选中case
      showAddRecord: false, // 展开添加记录弹框
      envList: [], // 执行记录环境列表
      options: { projectLs: [], requirementLs: [] },
      requirement: null,
      filterStatus: 'filter-hide',
      filterVisble: false,
      loading: true,
      current: 1,
      productLineId: '',
      treeData: [],
      levelId: '',
      levelText: '',
      searchValue: '',
      autoExpandParent: true,
      dataList: [],
      caseIds: ['root'],
      isSelect: true,
      isSibling: true,
      isAdd: true,
      isReName: true,
      treeSelect: [],
      treeData: [],
      pollingCaseIds: [],
      pollingTimer: null,
      selectedCaseIds: [],
      extractLoading: false,
    };
  }
  componentWillReceiveProps(nextProps) {
    if (
      this.props.match.params.productLineId !=
      nextProps.match.params.productLineId
    ) {
      this.setState(
        {
          productLineId: nextProps.match.params.productLineId,
        },
        () => {
          this.getCaseList(1, '', '', '', []);
          this.getProductMumber();
        },
      );
    }
  }
  getTreeList = isManual => {
    const { productLineId, caseIds } = this.state;
    const { doneApiPrefix } = this.props;
    return request(`${doneApiPrefix}/dir/list`, {
      method: 'GET',
      params: {
        productLineId,
        channel: 1,
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState(
          {
            treeData: res.data.children,
            caseIds:
              this.state.treeSelect.length > 0
                ? this.state.treeSelect.toString()
                : caseIds,
          },
          () => {
            if (!isManual) this.getCaseList(1, '', '', '', []);
          },
        );
      } else {
        message.error(res.msg);
      }
      return null;
    });
  };
  getCaseList = (
    current,
    nameFilter,
    createrFilter,
    iterationFilter,
    choiseDate = [],
    caseKeyWords = '',
  ) => {
    const { caseIds } = this.state;
    request(`${this.props.doneApiPrefix}/case/list`, {
      method: 'GET',
      params: {
        pageSize: 10,
        pageNum: current,
        productLineId: this.state.productLineId,
        caseType: 0,
        title: nameFilter || '',
        creator: createrFilter || '',
        channel: 1,
        requirementId: iterationFilter || '',
        beginTime: choiseDate.length > 0 ? `${choiseDate[0]} 00:00:00` : '',
        endTime: choiseDate.length > 0 ? `${choiseDate[1]}  23:59:59` : '',
        bizId: caseIds ? caseIds : 'root',
        caseKeyWords: caseKeyWords || '',
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState({
          list: res.data.dataSources,
          total: res.data.total,
          current,
          nameFilter,
          createrFilter,
          iterationFilter,
          choiseDate,
          caseKeyWords,
        });
      } else {
        message.error(res.msg);
      }
      this.setState({ loading: false });
      return null;
    });
  };

  initCaseModalInfo = () => {
    let { requirementLs } = this.state;
    let requirement = null;
    this.setState({
      options: {
        requirement,
        requirementLs,
      },
    });
  };
  getProductMumber = () => {
    let url = `${this.props.doneApiPrefix}/case/listCreators`;
    request(url, {
      method: 'GET',
      params: { productLineId: this.state.productLineId, caseType: 0 },
    }).then(res => {
      if (res.code === 200) {
        this.setState({
          productMember: res.data,
        });
      }
    });
  };
  handleTask = (val, record, project, requirement, current) => {
    this.setState(
      {
        visible: true,
        title: val,
        currCase: record,
        project,
        requirement,
        current,
      },
      () => {
        this.props.form.resetFields();
      },
    );
  };
  onShowFilterBoxClick = () => {
    let showFilterBox = !this.state.showFilterBox;
    this.setState({
      showFilterBox,
      iterationFilter: '',
      nameFilter: '',
      choiseDate: [],
      createrFilter: '',
      caseKeyWords: '',
    });
  };
  onClose = vis => {
    this.setState({ visible: vis });
  };

  componentDidMount() {
    this.setState(
      {
        productLineId: this.props.match.params.productLineId,
      },
      () => {
        this.getProductMumber();
        this.getTreeList();
      },
    );
  }

  componentWillUnmount() {
    this.stopPolling();
  }

  componentDidUpdate(prevProps, prevState) {
    const hasPendingCases = this.state.list.some(item => item.isClickable === 0);
    const hadPendingCases = prevState.list.some(item => item.isClickable === 0);
    if (hasPendingCases && !hadPendingCases) {
      this.startPolling();
    } else if (!hasPendingCases && hadPendingCases) {
      this.stopPolling();
    }
  }

  startPolling = () => {
    if (this.state.pollingTimer) return;

    const timer = setInterval(() => {
      const ONE_DAY_MS = 24 * 60 * 60 * 1000;
      const now = Date.now();
      const pendingIds = this.state.list
        .filter(item => item.isClickable === 0 && item.gmtCreated && (now - new Date(item.gmtCreated).getTime()) < ONE_DAY_MS)
        .map(item => item.id);

      if (pendingIds.length === 0) {
        this.stopPolling();
        return;
      }

      request(`${this.props.doneApiPrefix}/case/checkStatus`, {
        method: 'GET',
        params: { caseIds: pendingIds.join(',') },
      }).then(res => {
        if (res.code !== 200 || !res.data) return;

        let hasUnfinished = false;
        const statusMap = {};
        res.data.forEach(item => {
          statusMap[item.caseId] = item.isClickable;
          if (!item.isClickable) hasUnfinished = true;
        });

        if (!hasUnfinished) {
          this.stopPolling();
        }

        const updatedList = this.state.list.map(item => {
          if (statusMap[item.id] !== undefined && statusMap[item.id] !== (item.isClickable === 1)) {
            return { ...item, isClickable: statusMap[item.id] ? 1 : 0 };
          }
          return item;
        });

        this.setState({ list: updatedList });
      });
    }, 5000);

    this.setState({ pollingTimer: timer });
  };

  stopPolling = () => {
    if (this.state.pollingTimer) {
      clearInterval(this.state.pollingTimer);
      this.setState({ pollingTimer: null });
    }
  };

  handleKnowledgeExtract = () => {
    const { selectedCaseIds } = this.state;
    if (!selectedCaseIds || selectedCaseIds.length === 0) {
      message.warning('请先选择至少一个用例');
      return;
    }

    this.setState({ extractLoading: true });
    request('/case/knowledge/extract', {
      method: 'POST',
      body: selectedCaseIds.map(String),
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
      request(`/case/knowledge/tasks/${taskId}`, {
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

  filterHandler = () => {
    this.setState({ filterStatus: 'filter-show', filterVisble: true });
  };

  closeFilter = () => {
    this.setState({ filterStatus: 'filter-hide', filterVisble: false });
  };

  render() {
    const {
      requirement,
      list,
      total,
      productMember,
      filterVisble,
      filterStatus,
      nameFilter,
      createrFilter,
      iterationFilter,
      choiseDate,
      treeData,
      caseIds,
      caseKeyWords,
    } = this.state;
    const { match, doneApiPrefix } = this.props;
    const { productLineId } = match.params;
    return (
      <div className="all-content">
        <FileTree
          productLineId={Number(productLineId)}
          doneApiPrefix={doneApiPrefix}
          getCaseList={caseIds => {
            this.setState({ caseIds }, () => {
              this.getCaseList(1, '', '', '');
            });
          }}
          getTreeList={this.getTreeList}
          treeData={treeData}
        />
        <div className="min-hig-content">
          <div className="site-drawer-render-in-current-wrapper">
            <Row className="m-b-10">
              <Col span={18}>
                <div style={{ margin: '10px' }}>
                  快速筛选：<a>全部({total})</a>
                </div>
              </Col>
              <Col xs={6} className="text-right">
                <Button
                  style={{ marginRight: 16 }}
                  onClick={this.filterHandler}
                >
                  <Icon type="filter" /> 筛选
                </Button>
                <Button
                  type="primary"
                  onClick={() => {
                    this.handleTask('add');
                    this.setState({
                      currCase: null,
                      visible: true,
                      project: null,
                      requirement: null,
                    });
                  }}
                >
                  <Icon type="plus" /> 新建用例集
                </Button>
                <Button
                                  icon="book"
                                  style={{ marginLeft: 16 }}
                                  onClick={() => router.push(`/knowledge/${this.state.productLineId}`)}
                                >
                                  知识库管理
                                </Button>
                <Button
                  type="primary"
                  icon="database"
                  loading={this.state.extractLoading}
                  onClick={this.handleKnowledgeExtract}
                  style={{ marginLeft: 16 }}
                >
                  提取用例知识库
                </Button>

              </Col>
            </Row>
            <hr
              style={{ border: '0', backgroundColor: '#e8e8e8', height: '1px' }}
            />
            {this.state.showFilterBox && (
              <Filter
                getCaseList={this.getCaseList}
                productMember={productMember}
              />
            )}
            <List
              productId={productLineId}
              options={this.state.options}
              list={list}
              total={total}
              handleTask={this.handleTask}
              getCaseList={this.getCaseList}
              getTreeList={this.getTreeList}
              type={this.props.type}
              loading={this.state.loading}
              baseUrl={this.props.baseUrl}
              oeApiPrefix={this.props.oeApiPrefix}
              doneApiPrefix={this.props.doneApiPrefix}
              current={this.state.current}
              nameFilter={nameFilter}
              caseKeyWords={caseKeyWords}
              createrFilter={createrFilter}
              iterationFilter={iterationFilter}
              choiseDate={choiseDate}
              selectedCaseIds={this.state.selectedCaseIds}
              onCaseSelectChange={this.handleCaseSelectChange}
            ></List>

            {(filterVisble && (
              <OeFilter
                onCancel={this.closeFilter}
                getCaseList={this.getCaseList}
                productMember={productMember}
                filterStatus={filterStatus}
                closeFilter={this.closeFilter}
                visible={filterVisble}
                oeApiPrefix={this.props.oeApiPrefix}
                productId={productLineId}
              />
            )) ||
              null}
          </div>

          {this.state.visible && (
            <CaseModal
              productId={productLineId}
              data={this.state.currCase}
              title={this.state.title}
              requirement={requirement}
              options={this.state.options}
              show={this.state.visible}
              onClose={this.onClose}
              oeApiPrefix={this.props.oeApiPrefix}
              doneApiPrefix={this.props.doneApiPrefix}
              baseUrl={this.props.baseUrl}
              onUpdate={() => {
                // this.getCaseList(this.state.current || 1, '', '', '', []);
                this.getTreeList();
                this.setState({ currCase: null, visible: false });
              }}
              type={this.props.type}
              caseIds={caseIds}
            />
          )}
        </div>
      </div>
    );
  }
}
export default Form.create()(CaseLists);
