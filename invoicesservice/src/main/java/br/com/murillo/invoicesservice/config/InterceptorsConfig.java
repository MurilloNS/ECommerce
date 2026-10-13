package br.com.murillo.invoicesservice.config;

import br.com.murillo.invoicesservice.invoices.interceptors.InvoiceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {
    private final InvoiceInterceptor invoiceInterceptor;

    @Autowired
    public InterceptorsConfig(InvoiceInterceptor invoiceInterceptor) {
        this.invoiceInterceptor = invoiceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.invoiceInterceptor).addPathPatterns("/api/invoices/**");
    }
}