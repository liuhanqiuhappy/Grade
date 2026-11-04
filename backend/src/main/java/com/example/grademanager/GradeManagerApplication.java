package com.example.grademanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.grademanager.**.mapper")
public class GradeManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradeManagerApplication.class, args);
    }
}
