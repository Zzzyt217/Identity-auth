package com.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.test.mapper")  // 添加这行，扫描Mapper接口
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("==========================================");
        System.out.println("数字身份认证系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("==========================================");
    }
}