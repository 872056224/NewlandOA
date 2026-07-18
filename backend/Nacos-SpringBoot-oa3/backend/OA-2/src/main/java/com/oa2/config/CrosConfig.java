package com.oa2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CrosConfig implements WebMvcConfigurer {
    public void addCorsMappings(CorsRegistry registry) {
        //前后端分离CORS配置 (兼容Spring Boot 2.3.7)
        registry
                .addMapping("/**")
                .allowedHeaders("*")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173")  // 明确指定允许的前端域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)      // 允许发送凭证（如cookies, session等）
                .maxAge(3600);              // 预检请求缓存时间
    }
}
