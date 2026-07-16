package com.oa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 网关跨域配置
 */
@Configuration
public class CorsConfig {
    /**
     *  允许所有来源（addAllowedOriginPattern("*")）
     *  允许所有 HTTP 方法（addAllowedMethod("*")）
     *  允许所有请求头（addAllowedHeader("*")）
     *  允许携带凭据（setAllowCredentials(true)）
     *  针对所有路径 /** 应用该配置
     *  最终返回一个基于该配置的 CorsWebFilter 实例
     */
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
} 