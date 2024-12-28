package com.ruppyrup.productsservice.config;

import com.ruppyrup.productsservice.products.interceptors.ProductInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {

    private final ProductInterceptor productInterceptor;

    public InterceptorsConfig(ProductInterceptor productInterceptor) {
        this.productInterceptor = productInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(productInterceptor)
                .addPathPatterns("/api/products/**");
    }
}
