package com.oa7.config;

import com.oa7.interceptor.RbacInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @name: chenle
 * @Date: 2021/12/24 16:44
 * @Author: IAO
 * @Description: 登录验证 + RBAC 权限拦截
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Bean
    public RbacInterceptor rbacInterceptor(){
        return new RbacInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor())
                .excludePathPatterns("/")
                .excludePathPatterns("/auth/login")
                .excludePathPatterns("/auth/register")
                .excludePathPatterns("/auth/logout")
                .excludePathPatterns("/static/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}
