package com.oa.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OA AI 智能客服微服务
 *
 * 技术栈：Spring Boot 3.4 + Spring AI 1.0 + Ollama 本地大模型
 * 端口：8083，context-path：/api/v1/ai，经网关 /api/v1/ai/** 访问
 */
@SpringBootApplication
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
