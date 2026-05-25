package com.food.soulfoodbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.amap")
public class AmapProperties {

    /** 高德 Web 服务 Key，通过环境变量 AMAP_API_KEY 注入 */
    private String apiKey = "";

    private String baseUrl = "https://restapi.amap.com";

    /** 未传定位时的默认经度（成都天府广场附近） */
    private double defaultLng = 104.065735;

    /** 未传定位时的默认纬度 */
    private double defaultLat = 30.657462;

    /** 搜索半径（米） */
    private int searchRadius = 3000;

    /** 单次拉取条数 */
    private int pageSize = 20;
}
