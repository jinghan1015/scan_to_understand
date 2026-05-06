package msmx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 解析绝对路径
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String absolutePath = uploadPath.toString();

        System.out.println("===========================================");
        System.out.println("静态资源映射配置:");
        System.out.println("  配置路径: " + uploadDir);
        System.out.println("  绝对路径: " + absolutePath);
        System.out.println("  映射URL: /uploads/** -> file:" + absolutePath + File.separator);
        System.out.println("===========================================");

        // 确保目录存在
        File dir = new File(absolutePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            System.out.println("创建上传目录: " + (created ? "成功" : "失败"));
        }

        // 映射静态资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}