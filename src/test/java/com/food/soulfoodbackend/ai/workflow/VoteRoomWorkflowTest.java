package com.food.soulfoodbackend.ai.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteRoomWorkflowTest {

    @Test
    void extractsExplicitOptions() {
        assertEquals(List.of("火锅", "烤肉", "寿司"), VoteRoomWorkflow.extractOptions("火锅还是烤肉还是寿司"));
        assertEquals(List.of("火锅", "烧烤"), VoteRoomWorkflow.extractOptions("帮我开个房，火锅、烧烤一起吃"));
    }

    @Test
    void ignoresBareVoteIntent() {
        assertTrue(VoteRoomWorkflow.extractOptions("帮我开个投票组局").isEmpty());
    }

    @Test
    void defaultTopic() {
        assertEquals("中午吃什么", VoteRoomWorkflow.topicOf("中午一起吃"));
        assertEquals("今晚吃什么", VoteRoomWorkflow.topicOf("组局"));
    }
}
