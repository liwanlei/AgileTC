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
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UrlPathHelper;

@RestController
@RequestMapping("/api/case/knowledge")
public class KnowledgeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeController.class);

    @Value("${knowledge.service.url}")
    private String knowledgeServiceUrl;

    /**
     * 代理转发请求到知识库服务
     * 支持 GET/POST/PUT/DELETE 方法，支持多级路径
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public void proxyKnowledgeRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 提取 /api/case/knowledge 之后的完整路径
            String subPath = new UrlPathHelper().getPathWithinApplication(request);
            subPath = subPath.replaceFirst("^/api/case/knowledge", "");

            String targetUrl = knowledgeServiceUrl + "/api/knowledge" + subPath;
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
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8
            ));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBody.append(line);
                    writer.write(line);
                }
                writer.flush();
            }

            LOGGER.info("[Knowledge Proxy] subPath={}, status={}, response={}", subPath, conn.getResponseCode(), responseBody.toString());

        } catch (Exception e) {
            LOGGER.error("[Knowledge Proxy] Proxy request failed. e={} ", e.getMessage());
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
