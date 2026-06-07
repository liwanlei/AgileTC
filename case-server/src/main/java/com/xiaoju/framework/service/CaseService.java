package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.request.cases.AiResultSaveReq;
import com.xiaoju.framework.entity.request.cases.CaseConditionReq;
import com.xiaoju.framework.entity.request.cases.CaseCreateReq;
import com.xiaoju.framework.entity.request.cases.CaseEditReq;
import com.xiaoju.framework.entity.request.cases.CaseQueryReq;
import com.xiaoju.framework.entity.request.ws.WsSaveReq;
import com.xiaoju.framework.entity.response.PersonResp;
import com.xiaoju.framework.entity.response.cases.CaseConditionResp;
import com.xiaoju.framework.entity.response.cases.CaseDetailResp;
import com.xiaoju.framework.entity.response.cases.CaseGeneralInfoResp;
import com.xiaoju.framework.entity.response.cases.CaseListResp;
import com.xiaoju.framework.entity.response.controller.PageModule;
import com.xiaoju.framework.entity.response.dir.DirTreeResp;

import java.util.List;
import java.util.Map;

/**
 * 用例接口
 *
 * @author didi
 * @date 2020/9/7
 */
public interface CaseService {

    /**
     * 获取case列表
     *
     * @param request 请求体
     * @return 用例集列表
     */
    PageModule<CaseListResp> getCaseList(CaseQueryReq request);

    /**
     * 根据文件夹id获取用例集列表
     *
     * @param caseId 用例id
     * @return 用例集详情
     */
    CaseDetailResp getCaseDetail(Long caseId);

    /**
     * 用例新建或者复制
     *
     * @param request 请求体
     * @return 创建的测试用例的caseId
     */
    Long insertOrDuplicateCase(CaseCreateReq request);

    /**
     * 更新用例
     *
     * @param request 请求体
     * @return 更新后的节点
     */
    DirTreeResp updateCase(CaseEditReq request);

    /**
     * 删除用例
     *
     * @param caseId 用例id
     * @return 被删除的用例的主键id
     */
    DirTreeResp deleteCase(Long caseId);

    /**
     * 获取根据用例种类和业务线获取用例的创建人map
     *
     * @param caseType 用例种类
     * @param lineId (本质上就是productLineId) 业务线id
     * @return 某条业务线，特定用例种类下的用例创建人
     */
    List<PersonResp> listCreators(Integer caseType, Long lineId);

    /**
     * 遍历一份用例，获取其中满足几个条件的count
     *
     * @param req 优先级、资源
     * @return 同级个数
     */
    CaseConditionResp getCountByCondition(CaseConditionReq req);

    /**
     * 点开用例后查看id 标题 关联需求的基本信息
     *
     * @param caseId 用例id
     * @return 概览信息
     */
    CaseGeneralInfoResp getCaseGeneralInfo(Long caseId);

    /**
     * websocket页面点击保存按钮
     *
     * @param req 请求体
     */
    void wsSave(WsSaveReq req);

    /**
     * 保存AI生成的用例内容并更新状态为可点击
     *
     * @param request AI结果保存请求
     */
    void saveAiResult(AiResultSaveReq request);

    /**
     * 批量查询用例的可点击状态
     *
     * @param caseIds 用例ID列表
     * @return 状态列表
     */
    List<Map<String, Object>> checkStatus(List<Long> caseIds);
}
