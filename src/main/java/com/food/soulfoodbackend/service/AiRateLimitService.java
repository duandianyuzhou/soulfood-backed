package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiRateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    private final Map<Long, Window> windows = new ConcurrentHashMap<>();

    public void checkAllowed(Long userId) {
        if (userId == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        Window window = windows.compute(userId, (id, existing) -> {
            if (existing == null || existing.minuteStart.isBefore(now.minusMinutes(1))) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });
        if (window.count > MAX_REQUESTS_PER_MINUTE) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "AI 请求过于频繁，请稍后再试");
        }
    }

    private static final class Window {
        private final OffsetDateTime minuteStart;
        private int count;

        private Window(OffsetDateTime minuteStart, int count) {
            this.minuteStart = minuteStart;
            this.count = count;
        }
    }
}
