package org.site.honey_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcImagePathConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/assets/img/**")
                .addResourceLocations("file:/app/uploads/img/")
                .setCachePeriod(0);

        registry.addResourceHandler("/app/uploads/img/**")
                .addResourceLocations("file:/app/uploads/img/")
                .setCachePeriod(0);
    }
}

