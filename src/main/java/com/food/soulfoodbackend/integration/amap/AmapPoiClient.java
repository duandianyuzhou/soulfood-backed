package com.food.soulfoodbackend.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.config.AmapProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmapPoiClient {

    private final AmapProperties properties;
    private final AmapCategoryResolver categoryResolver;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public List<AmapPoi> searchNearby(double lng, double lat, String category, String keyword) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.INTERNAL, "未配置高德地图 API Key，请在 application-local.yaml 设置 app.amap.api-key");
        }

        String location = formatCoordinate(lng) + "," + formatCoordinate(lat);
        String keywords = categoryResolver.resolveKeywords(category, keyword);
        String types = categoryResolver.resolveTypes();

        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUrl() + "/v3/place/around")
                .queryParam("key", properties.getApiKey())
                .queryParam("location", location)
                .queryParam("keywords", keywords)
                .queryParam("types", types)
                .queryParam("radius", properties.getSearchRadius())
                .queryParam("offset", properties.getPageSize())
                .queryParam("page", 1)
                .queryParam("sortrule", "distance")
                .queryParam("extensions", "all")
                .build(true)
                .toUri();

        String body;
        try {
            body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            log.warn("Amap around search failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL, "高德地图请求失败，请稍后重试");
        }

        return parseResponse(body);
    }

    private List<AmapPoi> parseResponse(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.INTERNAL, "高德地图返回空响应");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText();
            if (!"1".equals(status)) {
                String info = root.path("info").asText("未知错误");
                log.warn("Amap API error: status={}, info={}", status, info);
                throw new BusinessException(ErrorCode.INTERNAL, "高德地图查询失败：" + info);
            }
            JsonNode pois = root.path("pois");
            if (!pois.isArray() || pois.isEmpty()) {
                return List.of();
            }
            List<AmapPoi> result = new ArrayList<>();
            for (JsonNode poi : pois) {
                AmapPoi mapped = mapPoi(poi);
                if (mapped != null) {
                    result.add(mapped);
                }
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to parse Amap response: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL, "解析高德地图数据失败");
        }
    }

    private AmapPoi mapPoi(JsonNode poi) {
        String id = poi.path("id").asText(null);
        String name = poi.path("name").asText(null);
        if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) {
            return null;
        }

        BigDecimal[] coordinates = parseLocation(poi.path("location").asText(null));
        if (coordinates == null) {
            return null;
        }

        String distanceText = poi.path("distance").asText("");
        BigDecimal distanceKm = parseDistanceKm(distanceText);
        String categoryLabel = categoryResolver.labelFromType(poi.path("type").asText(""));
        String address = poi.path("address").asText("");
        if (!StringUtils.hasText(address)) {
            address = poi.path("pname").asText("") + poi.path("cityname").asText("") + poi.path("adname").asText("");
        }

        return new AmapPoi(
                id,
                name,
                categoryLabel,
                address,
                coordinates[0],
                coordinates[1],
                distanceKm,
                parseRating(poi.path("biz_ext").path("rating")));
    }

    private BigDecimal parseRating(JsonNode ratingNode) {
        if (ratingNode.isMissingNode() || ratingNode.isNull()) {
            return null;
        }
        if (ratingNode.isArray() && ratingNode.isEmpty()) {
            return null;
        }
        String raw = ratingNode.asText(null);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw).setScale(1, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDistanceKm(String distanceMeters) {
        if (!StringUtils.hasText(distanceMeters)) {
            return null;
        }
        try {
            return new BigDecimal(distanceMeters)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal[] parseLocation(String location) {
        if (!StringUtils.hasText(location) || !location.contains(",")) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new BigDecimal[] {
                    new BigDecimal(parts[0].trim()),
                    new BigDecimal(parts[1].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatCoordinate(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).toPlainString();
    }
}
