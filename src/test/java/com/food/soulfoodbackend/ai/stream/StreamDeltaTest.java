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

    @Test
    void stripsPriorAssistantReplayButKeepsNewAnswer() {
        String prior = "抱歉，需要你登录授权定位后才能搜附近的寿司店。";
        assertEquals("", StreamDelta.of("", "抱歉", prior));
        assertEquals("", StreamDelta.of("", prior, prior));
        assertEquals("附近有回转寿司可以选择。", StreamDelta.of("", prior + "附近有回转寿司可以选择。", prior));
        assertEquals("可以选择。", StreamDelta.of("附近有回转寿司", prior + "附近有回转寿司可以选择。", prior));
    }
}
