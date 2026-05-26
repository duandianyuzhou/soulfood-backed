package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.auth.LoginResponse;
import com.food.soulfoodbackend.dto.auth.MockLoginRequest;
import com.food.soulfoodbackend.dto.auth.PasswordLoginRequest;
import com.food.soulfoodbackend.dto.auth.RegisterRequest;
import com.food.soulfoodbackend.dto.auth.UpdateProfileRequest;
import com.food.soulfoodbackend.dto.auth.UserProfileResponse;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SfUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final InviteService inviteService;
    private final UserAccountHelper userAccountHelper;
    private final JwtService jwtService;

    public LoginResponse mockLogin(MockLoginRequest request) {
        String openId = "mock:" + request.getDeviceId().trim();
        SfUser user = userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getOpenId, openId));
        if (user == null) {
            user = createUser(openId, null, null,
                    request.getNickname() != null && !request.getNickname().isBlank()
                            ? request.getNickname()
                            : "美食家" + request.getDeviceId().substring(Math.max(0, request.getDeviceId().length() - 4)));
        }
        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getUsername, username)) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        String nickname = request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim()
                : username;

        SfUser user = createUser("account:" + username, username,
                passwordEncoder.encode(request.getPassword()), nickname);
        userAccountHelper.ensureInviteCode(user);

        if (request.getInviteCode() != null && !request.getInviteCode().isBlank()) {
            inviteService.acceptInviteOnRegister(user.getId(), request.getInviteCode().trim());
        }

        return toLoginResponse(user);
    }

    public LoginResponse loginWithPassword(PasswordLoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        SfUser user = userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getUsername, username));
        if (user == null || user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已禁用");
        }
        return toLoginResponse(user);
    }

    public UserProfileResponse getProfile(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatarUrl(), user.getSignature());
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim().isEmpty() ? null : request.getAvatarUrl().trim());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature().trim().isEmpty() ? null : request.getSignature().trim());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return getProfile(userId);
    }

    private SfUser createUser(String openId, String username, String passwordHash, String nickname) {
        SfUser user = new SfUser();
        user.setOpenId(openId);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setStatus(1);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setDeleted(false);
        userMapper.insert(user);
        return user;
    }

    private LoginResponse toLoginResponse(SfUser user) {
        userAccountHelper.ensureInviteCode(user);
        String token = jwtService.createAccessToken(user.getId(), user.getUsername());
        return new LoginResponse(user.getId(), token, user.getNickname(), user.getAvatarUrl());
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
