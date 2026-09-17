package br.com.murillo.productsservice.config;

import br.com.murillo.productsservice.products.interceptors.ProductInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {
    private final ProductInterceptor productsInterceptor;

    @Autowired
    public InterceptorsConfig(ProductInterceptor productsInterceptor) {
        this.productsInterceptor = productsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.productsInterceptor).addPathPatterns("/api/products/**");
    }
}