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
        //添加登录验证拦截器
        registry.addInterceptor(loginInterceptor())
                .excludePathPatterns("/")
                .excludePathPatterns("/login")          // 员工登录接口
                .excludePathPatterns("/logout")         // 员工退出登录接口
                .excludePathPatterns("/ai/kb/reload")   // 管理端触发向量索引重建
                .excludePathPatterns("/static/**");
    }

    //需要告知系统，这是要被当成静态文件的！
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //第一个方法设置访问路径前缀，第二个方法设置资源路径
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}
