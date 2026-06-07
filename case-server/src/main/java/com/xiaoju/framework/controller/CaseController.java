package com.xiaoju.framework.controller;

import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.cases.*;
import com.xiaoju.framework.entity.request.ws.WsSaveReq;
import com.xiaoju.framework.entity.response.controller.Response;
import com.xiaoju.framework.service.CaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用例相关接口
 *
 * @author didi
 * @date 2020/11/20
 */
@RestController
@CrossOrigin
@RequestMapping("/api/case")
public class CaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseController.class);

    @Resource
    CaseService caseService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    /**
     * 用例 - 根据文件夹id获取所有用例
     *
     * @param productLineId 业务线id
     * @param bizId 用例id
     * @param title 用例标题
     * @param creator 创建人前缀
     * @param requirementId 需求id
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param channel 1
     * @param pageNum 页码
     * @param pageSize 页面承载量
     * @return 分页接口
     */
    @GetMapping(value = "/list")
    public Response<?> getCaseList(@RequestParam @NotNull(message = "渠道为空")  Integer channel,
                                   @RequestParam @NotNull(message = "业务线id为空")  Long productLineId,
                                   @RequestParam @NotNull(message = "文件夹未选中")  String bizId,
                                   @RequestParam(required = false)  String title,
                                   @RequestParam(required = false)  String creator,
                                   @RequestParam(required = false)  String requirementId,
                                   @RequestParam(required = false)  String caseKeyWords,
                                   @RequestParam(required = false)  String beginTime,
                                   @RequestParam(required = false)  String endTime,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Response.success(caseService.getCaseList(
                new CaseQueryReq(0, title, creator, requirementId, beginTime,
                        endTime, channel, bizId, productLineId, caseKeyWords, pageNum, pageSize)));
    }

    /**
     * 列表 - 创建或者复制用例
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/create")
    public Response<?> createOrCopyCase(@RequestBody CaseCreateReq request) {
        request.validate();
        try {
            return Response.success(caseService.insertOrDuplicateCase(request));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("[Case Create]Create or duplicate test case failed. params={}, e={} ", request.toString(), e.getMessage());
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 列表 - 修改用例属性
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/edit")
    public Response<?> editCase(@RequestBody CaseEditReq request) {
        request.validate();
        try {
            return Response.success(caseService.updateCase(request));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("[Case Update]Update test case failed. params={} e={} ", request.toString(), e.getMessage());
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 列表 - 删除用例
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/delete")
    public Response<?> deleteCase(@RequestBody CaseDeleteReq request) {
        request.validate();
        try {
            return Response.success(caseService.deleteCase(request.getId()));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[Case Delete]Delete test case failed. params={} e={} ", request.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 列表 - 查看用例详情
     *
     * @param caseId 用例id
     * @return 响应体
     */
    @GetMapping(value = "/detail")
    public Response<?> getCaseDetail(@RequestParam @NotNull(message = "用例id为空") Long caseId) {
        try {
            return Response.success(caseService.getCaseDetail(caseId));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("[Case detail]View detail of test case failed. params={}, e={} ", caseId, e.getMessage());
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * 配合list 筛选时获取所有创建人的列表
     *
     * @param caseType 用例类型
     * @param productLineId 业务线id
     * @return 响应体
     */
    @GetMapping(value = "/listCreators")
    public Response<?> listCreators(@RequestParam @NotNull(message = "用例类型为空") Integer caseType,
                                    @RequestParam @NotNull(message = "业务线为空") Long productLineId) {
        return Response.success(caseService.listCreators(caseType, productLineId));
    }

    /**
     * 配合detail 修改圈选用例时统计的用例条目数据
     *
     * @param caseId 用例id
     * @param priority 优先级列表
     * @param resource 资源列表
     * @return 响应体
     */
    @GetMapping(value = "/countByCondition")
    public Response<?> getCountByCondition(@RequestParam @NotNull(message = "用例id为空") Long caseId,
                                           @RequestParam @NotNull(message = "圈选优先级为空") String[] priority,
                                           @RequestParam @NotNull(message = "圈选资源为空") String[] resource) {
        CaseConditionReq req = new CaseConditionReq(caseId, priority, resource);
        req.validate();
        return Response.success(caseService.getCountByCondition(req));
    }

    /**
     * 脑图 - 获取上方用例概览信息
     *
     * @param id 用例id
     * @return 概览信息
     */
    @GetMapping(value = "/getCaseInfo")
    public Response<?> getCaseGeneralInfo(@RequestParam @NotNull(message = "用例id为空") Long id) {
        return Response.success(caseService.getCaseGeneralInfo(id));
    }

    /**
     * 脑图 - 保存按钮 可能是case也可能是record
     *
     * @param req 请求体
     * @return 响应体
     */
    @PostMapping(value = "/update")
    public Response<?> updateWsCase(@RequestBody WsSaveReq req) {
        try {
            caseService.wsSave(req);
            return Response.success();
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("[Case Update]Update test case failed. params={} e={} ", req.toString(), e.getMessage());
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * AI - 保存AI生成的用例结果
     *
     * @param request 请求体
     * @return 响应体
     */
    @PostMapping(value = "/saveAiResult")
    public Response<?> saveAiResult(@RequestBody AiResultSaveReq request) {
        request.validate();
        try {
            caseService.saveAiResult(request);
            return Response.success();
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("[AI Save Result] Save AI result failed. params={}, e={} ", request.toString(), e.getMessage());
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }
    }

    /**
     * AI - 检查用例是否可点击
     *
     * @param caseIds 用例ID列表，逗号分隔
     * @return 状态列表
     */
    @GetMapping(value = "/checkStatus")
    public Response<?> checkStatus(@RequestParam String caseIds) {
        try {
            List<Long> idList = Arrays.stream(caseIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            return Response.success(caseService.checkStatus(idList));
        } catch (Exception e) {
            LOGGER.error("[Check Status] Check status failed. caseIds={}, e={} ", caseIds, e.getMessage());
            return Response.build(StatusCode.DATA_FORMAT_ERROR);
        }
    }


    /**
     * AI - 代理转发请求到AI服务
     */
    @PostMapping(value = "/aiRun")
    public void aiRun(
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String doc_url,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam Long caseId,
            HttpServletResponse response
    ) {
        try {
            URL url = new URL(aiServiceUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setReadTimeout(60000);
            conn.setConnectTimeout(10000);

            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream os = conn.getOutputStream();

            // 传递 caseId 给 AI 服务
            os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            os.write("Content-Disposition: form-data; name=\"caseId\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(String.valueOf(caseId).getBytes(StandardCharsets.UTF_8));
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));

            if (task != null && !task.isEmpty()) {
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write("Content-Disposition: form-data; name=\"task\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                os.write(task.getBytes(StandardCharsets.UTF_8));
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

            if (doc_url != null && !doc_url.isEmpty()) {
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write("Content-Disposition: form-data; name=\"doc_url\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                os.write(doc_url.getBytes(StandardCharsets.UTF_8));
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

            if (file != null && !file.isEmpty()) {
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(file.getBytes());
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            response.setStatus(conn.getResponseCode());
            response.setContentType(conn.getContentType());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()
            ));
            String line;
            OutputStream out = response.getOutputStream();
            while ((line = br.readLine()) != null) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
            br.close();
            conn.disconnect();

            LOGGER.info("[AI Proxy] Forwarded AI run request for caseId={}", caseId);
        } catch (Exception e) {
            LOGGER.error("[AI Proxy] Forward AI run request failed. caseId={}, e={}", caseId, e.getMessage());
            e.printStackTrace();
        }
    }

}
