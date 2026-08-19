package com.food.soulfoodbackend.ai.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentRouter {

    private final LlmIntentRouter llmIntentRouter;

    public RouteDecision route(String message, boolean hasImage, Double lat, Double lng) {
        boolean hasLocation = lat != null && lng != null;
        ChatIntent ruled = RuleIntentRouter.tryRoute(message, hasImage, hasLocation);
        if (ruled != null) {
            return RouteDecision.of(ruled, "rule");
        }
        RouteDecision llm = llmIntentRouter.classify(
                StringUtils.hasText(message) ? message : "",
                hasImage,
                hasLocation);
        log.info("intent llm message='{}' -> {} ({})", trim(message), llm.intent(), llm.confidence());
        return llm;
    }

    private static String trim(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 40 ? message.substring(0, 40) : message;
    }
}
