package com.oa2.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oa2.config.AiLlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * OpenAI 兼容的 Chat Completions 客户端（RAG 生成式回答用）
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    @Autowired
    private AiLlmProperties properties;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty()
                && properties.getBaseUrl() != null && !properties.getBaseUrl().trim().isEmpty();
    }

    /**
     * 调用 LLM 生成回答
     *
     * @param systemPrompt 系统提示词（含检索到的知识上下文）
     * @param userQuestion 用户问题
     * @return 模型回答文本
     * @throws Exception 网络异常 / 响应格式异常，由调用方决定降级策略
     */
    public String complete(String systemPrompt, String userQuestion) throws Exception {
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userQuestion);
        messages.add(user);

        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 512);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey().trim());

        String url = properties.getBaseUrl().trim().replaceAll("/+$", "") + "/chat/completions";
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        String response = restTemplate.postForObject(url, request, String.class);
        JSONObject json = JSONObject.parseObject(response);
        JSONArray choices = json == null ? null : json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("LLM 响应缺少 choices 字段");
        }
        String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("LLM 返回了空回答");
        }
        log.debug("LLM 回答成功，长度: {}", content.length());
        return content.trim();
    }
}
