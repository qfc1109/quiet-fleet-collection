package com.qfc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final CsrfTokenInterceptor csrfTokenInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor, CsrfTokenInterceptor csrfTokenInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.csrfTokenInterceptor = csrfTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrfTokenInterceptor)
            .addPathPatterns("/api/**");
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**");
    }
}
