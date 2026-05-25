package com.food.soulfoodbackend.dto.preference;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PreferenceResponse {

    private List<String> tags;
    private int spicyLevel;
    private int sweetLevel;
    private boolean noCoriander;
    private boolean noPeanut;
    private List<String> allergens;
    /** 拼接后的文本，供 Agent 请求直接使用 */
    private String preferenceText;
}
