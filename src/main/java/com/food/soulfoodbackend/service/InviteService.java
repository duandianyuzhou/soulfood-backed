package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfInvite;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.invite.InviteOverviewResponse;
import com.food.soulfoodbackend.dto.invite.InviteRecordDto;
import com.food.soulfoodbackend.mapper.SfInviteMapper;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final int TARGET_COUNT = 5;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SfInviteMapper inviteMapper;
    private final SfUserMapper userMapper;
    private final UserAccountHelper userAccountHelper;
    private final FriendService friendService;

    public InviteOverviewResponse overview(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userAccountHelper.ensureInviteCode(user);

        List<SfInvite> rows = inviteMapper.selectList(new LambdaQueryWrapper<SfInvite>()
                .eq(SfInvite::getInviterId, userId)
                .orderByDesc(SfInvite::getCreatedAt)
                .last("LIMIT 20"));

        List<InviteRecordDto> records = new ArrayList<>();
        int completed = 0;
        for (SfInvite row : rows) {
            SfUser invitee = row.getInviteeId() == null ? null : userMapper.selectById(row.getInviteeId());
            String nickname = invitee != null ? invitee.getNickname() : "好友";
            boolean done = "completed".equals(row.getStatus());
            if (done) {
                completed++;
            }
            records.add(new InviteRecordDto(
                    nickname,
                    row.getCreatedAt() == null ? "" : DISPLAY_TIME.format(row.getCreatedAt()),
                    row.getStatus(),
                    done ? "已加入小队" : "待完成注册"));
        }

        return new InviteOverviewResponse(
                user.getInviteCode(), completed, TARGET_COUNT, completed >= TARGET_COUNT, records);
    }

    public boolean isBadgeUnlocked(Long userId) {
        long completed = inviteMapper.selectCount(new LambdaQueryWrapper<SfInvite>()
                .eq(SfInvite::getInviterId, userId)
                .eq(SfInvite::getStatus, "completed"));
        return completed >= TARGET_COUNT;
    }

    @Transactional
    public void acceptInviteOnRegister(Long inviteeId, String inviteCode) {
        SfUser inviter = userMapper.selectOne(new LambdaQueryWrapper<SfUser>()
                .eq(SfUser::getInviteCode, inviteCode.toUpperCase()));
        if (inviter == null) {
            return;
        }
        if (inviter.getId().equals(inviteeId)) {
            return;
        }

        SfInvite existing = inviteMapper.selectOne(new LambdaQueryWrapper<SfInvite>()
                .eq(SfInvite::getInviterId, inviter.getId())
                .eq(SfInvite::getInviteeId, inviteeId)
                .last("LIMIT 1"));
        if (existing != null) {
            friendService.bindFriends(inviter.getId(), inviteeId, "invite");
            return;
        }

        SfInvite invite = new SfInvite();
        invite.setInviterId(inviter.getId());
        invite.setInviteCode(inviteCode.toUpperCase());
        invite.setInviteeId(inviteeId);
        invite.setStatus("completed");
        invite.setRewardStatus("none");
        invite.setCreatedAt(OffsetDateTime.now());
        invite.setCompletedAt(OffsetDateTime.now());
        inviteMapper.insert(invite);
        friendService.bindFriends(inviter.getId(), inviteeId, "invite");
    }
}
