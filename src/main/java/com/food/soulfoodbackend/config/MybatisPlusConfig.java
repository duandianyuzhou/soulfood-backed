package com.food.soulfoodbackend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.food.soulfoodbackend.mapper")
public class MybatisPlusConfig {
}
