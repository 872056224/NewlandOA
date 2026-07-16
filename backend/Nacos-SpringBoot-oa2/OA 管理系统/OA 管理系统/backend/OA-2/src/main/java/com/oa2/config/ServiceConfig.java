package com.oa2.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
public class ServiceConfig {
    
    // 通过配置属性决定使用哪个 SignService 实现
    // 在 application.yml 中设置 sign.storage.type=elasticsearch 来启用 ES 存储
} 