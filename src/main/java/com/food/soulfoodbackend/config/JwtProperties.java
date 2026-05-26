package com.food.soulfoodbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 密钥，生产环境请通过环境变量 JWT_SECRET 注入 */
    private String secret = "decidemeal-dev-secret-change-in-production";

    /** Access Token 有效期（小时） */
    private int accessTokenExpireHours = 168;

    /** Refresh Token 有效期（小时） */
    private int refreshTokenExpireHours = 720;
}
