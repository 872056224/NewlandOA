package com.oa2.config;

import com.oa2.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Bean
    public LoginInterceptor loginInterceptor(){
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        System.out.println("添加登录验证拦截器");
        //添加登录验证拦截器
        registry.addInterceptor(loginInterceptor())
                .excludePathPatterns("/")
                .excludePathPatterns("/login")          // 员工登录接口
                .excludePathPatterns("/logout")         // 员工退出登录接口
                .excludePathPatterns("/location/**")    // 位置服务接口
                .excludePathPatterns("/ai/kb/reload")   // 管理端触发向量索引重建
                .excludePathPatterns("/static/**");
    }
}
