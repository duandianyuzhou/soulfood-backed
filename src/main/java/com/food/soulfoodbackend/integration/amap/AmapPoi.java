package com.food.soulfoodbackend.integration.amap;

import java.math.BigDecimal;

public record AmapPoi(
        String id,
        String name,
        String categoryLabel,
        String address,
        BigDecimal lng,
        BigDecimal lat,
        BigDecimal distanceKm,
        BigDecimal rating) {
}
