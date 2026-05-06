package msmx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class MsmxApplication {

    public static void main(String[] args) {
        // 确保 uploads 目录存在
        String uploadDir = System.getProperty("user.dir") + "/uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("创建 uploads 目录: " + uploadDir);
        }
        SpringApplication.run(MsmxApplication.class, args);
    }

}
