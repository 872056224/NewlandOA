package com.oa2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客服 LLM 增强配置（可选）
 *
 * enabled=false 时走纯向量检索问答（默认，零外部依赖）；
 * enabled=true 且配置了 api-key 时，走 RAG 模式：
 * 向量检索召回知识 -> 注入 Prompt -> 调用 OpenAI 兼容接口生成回答。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class AiLlmProperties {

    private boolean enabled = false;

    /** OpenAI 兼容接口地址，如 https://api.deepseek.com/v1 */
    private String baseUrl = "";

    private String apiKey = "";

    private String model = "deepseek-chat";

    private int timeoutMs = 30000;
}
