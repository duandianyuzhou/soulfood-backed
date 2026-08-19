package com.food.soulfoodbackend.ai.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamDeltaTest {

    @Test
    void keepsTrueDeltas() {
        assertEquals("你", StreamDelta.of("", "你"));
        assertEquals("好", StreamDelta.of("你", "好"));
    }

    @Test
    void stripsCumulativeReplay() {
        assertEquals("好", StreamDelta.of("你", "你好"));
        assertEquals("", StreamDelta.of("今天吃清淡的", "今天吃清淡的"));
    }
}
