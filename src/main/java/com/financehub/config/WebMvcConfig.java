package com.financehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${fh.uploads.root:uploads}")
    private String uploadsRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Path.of(uploadsRoot).toAbsolutePath().normalize();
        String location = "file:" + base.toString().replace('\\', '/') + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
