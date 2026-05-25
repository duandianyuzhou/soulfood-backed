package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfFriend;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.friend.AddFriendRequest;
import com.food.soulfoodbackend.dto.friend.FriendItemDto;
import com.food.soulfoodbackend.mapper.SfFriendMapper;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SfFriendMapper friendMapper;
    private final SfUserMapper userMapper;

    public List<FriendItemDto> listFriends(Long userId) {
        List<SfFriend> rows = friendMapper.selectList(new LambdaQueryWrapper<SfFriend>()
                .eq(SfFriend::getUserId, userId)
                .orderByDesc(SfFriend::getCreatedAt));

        List<FriendItemDto> result = new ArrayList<>();
        for (SfFriend row : rows) {
            SfUser friend = userMapper.selectById(row.getFriendUserId());
            if (friend == null) {
                continue;
            }
            result.add(new FriendItemDto(
                    friend.getId(),
                    friend.getNickname(),
                    friend.getAvatarUrl(),
                    row.getSource(),
                    row.getCreatedAt() == null ? "" : DISPLAY_TIME.format(row.getCreatedAt())
            ));
        }
        return result;
    }

    @Transactional
    public FriendItemDto addFriendByInviteCode(Long userId, AddFriendRequest request) {
        String code = normalizeInviteCode(request.getInviteCode());
        SfUser target = userMapper.selectOne(new LambdaQueryWrapper<SfUser>()
                .eq(SfUser::getInviteCode, code));
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码无效");
        }
        if (target.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能添加自己为好友");
        }
        bindFriends(userId, target.getId(), "manual");
        return new FriendItemDto(
                target.getId(),
                target.getNickname(),
                target.getAvatarUrl(),
                "manual",
                DISPLAY_TIME.format(OffsetDateTime.now())
        );
    }

    @Transactional
    public void bindFriends(Long userId, Long friendUserId, String source) {
        if (userId.equals(friendUserId)) {
            return;
        }
        insertIfAbsent(userId, friendUserId, source);
        insertIfAbsent(friendUserId, userId, source);
    }

    public List<Long> listFriendUserIds(Long userId) {
        return friendMapper.selectList(new LambdaQueryWrapper<SfFriend>()
                        .eq(SfFriend::getUserId, userId)
                        .select(SfFriend::getFriendUserId))
                .stream()
                .map(SfFriend::getFriendUserId)
                .toList();
    }

    private void insertIfAbsent(Long userId, Long friendUserId, String source) {
        Long count = friendMapper.selectCount(new LambdaQueryWrapper<SfFriend>()
                .eq(SfFriend::getUserId, userId)
                .eq(SfFriend::getFriendUserId, friendUserId));
        if (count != null && count > 0) {
            return;
        }
        SfFriend row = new SfFriend();
        row.setUserId(userId);
        row.setFriendUserId(friendUserId);
        row.setSource(source);
        row.setCreatedAt(OffsetDateTime.now());
        friendMapper.insert(row);
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
