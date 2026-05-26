package com.food.soulfoodbackend.dto.friend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendFriendMessageRequest {

    /** text（默认）| vote_share */
    private String messageType;

    @Size(max = 2000)
    private String content;

    @Valid
    private VoteSharePayloadDto voteShare;
}
