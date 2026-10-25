package com.ryo.ryopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.ryo.ryopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true) //获取代理 暴露代理 AopContext.currentproxy()获取代理类
public class RyoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RyoPictureBackendApplication.class, args);
    }

}
