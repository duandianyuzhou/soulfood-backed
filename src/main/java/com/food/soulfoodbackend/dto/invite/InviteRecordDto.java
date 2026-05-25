package com.food.soulfoodbackend.dto.invite;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InviteRecordDto {

    private String nickname;
    private String registeredAt;
    private String status;
    private String statusLabel;
}
