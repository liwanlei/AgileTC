package com.xiaoju.framework.entity.request.cases;

import com.xiaoju.framework.entity.request.ParamValidate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * AI 结果保存请求体
 *
 * @author agiletc
 * @date 2026/05/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiResultSaveReq implements ParamValidate {

    /**
     * 必填 用例ID
     */
    private Long caseId;

    /**
     * 必填 AI任务ID
     */
    private String taskId;

    /**
     * 必填 脑图JSON内容（可以是字符串或对象）
     */
    private Object caseContent;

    @Override
    public void validate() {
        if (caseId == null || caseId <= 0) {
            throw new IllegalArgumentException("用例id非法");
        }
        if (StringUtils.isEmpty(taskId)) {
            throw new IllegalArgumentException("AI任务ID为空");
        }
        if (StringUtils.isEmpty(caseContent)) {
            throw new IllegalArgumentException("AI生成的用例内容为空");
        }
    }
}
