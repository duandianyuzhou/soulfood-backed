package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.auth.LoginResponse;
import com.food.soulfoodbackend.dto.auth.MockLoginRequest;
import com.food.soulfoodbackend.dto.auth.UserProfileResponse;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SfUserMapper userMapper;

    public LoginResponse mockLogin(MockLoginRequest request) {
        String openId = "mock:" + request.getDeviceId().trim();
        SfUser user = userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getOpenId, openId));
        if (user == null) {
            user = new SfUser();
            user.setOpenId(openId);
            user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                    ? request.getNickname()
                    : "美食家" + request.getDeviceId().substring(Math.max(0, request.getDeviceId().length() - 4)));
            user.setStatus(1);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            user.setDeleted(false);
            userMapper.insert(user);
        }
        return new LoginResponse(user.getId(), String.valueOf(user.getId()), user.getNickname(), user.getAvatarUrl());
    }

    public UserProfileResponse getProfile(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return new UserProfileResponse(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getSignature());
    }
}
