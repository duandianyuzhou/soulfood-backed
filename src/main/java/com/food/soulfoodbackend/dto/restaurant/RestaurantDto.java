package com.food.soulfoodbackend.dto.restaurant;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RestaurantDto {

    private Long id;
    private String name;
    private String category;
    private BigDecimal rating;
    private BigDecimal distanceKm;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String phone;
    private String photoUrl;
    private boolean wanted;
    private boolean favorited;
}
