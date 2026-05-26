package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.auth.BindPhoneRequest;
import com.food.soulfoodbackend.dto.auth.ChangePasswordRequest;
import com.food.soulfoodbackend.dto.auth.LoginResponse;
import com.food.soulfoodbackend.dto.auth.MockLoginRequest;
import com.food.soulfoodbackend.dto.auth.PasswordLoginRequest;
import com.food.soulfoodbackend.dto.auth.RegisterRequest;
import com.food.soulfoodbackend.dto.auth.ResetPasswordRequest;
import com.food.soulfoodbackend.dto.auth.TokenRefreshResponse;
import com.food.soulfoodbackend.dto.auth.UpdateProfileRequest;
import com.food.soulfoodbackend.dto.auth.UserProfileResponse;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final AvatarStorageService avatarStorageService;

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
        SfUser user = requireUser(userId);
        return toProfile(user);
    }

    public TokenRefreshResponse refreshToken(String refreshToken) {
        Long userId = jwtService.parseRefreshUserId(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录"));
        SfUser user = requireUser(userId);
        return new TokenRefreshResponse(
                jwtService.createAccessToken(user.getId(), user.getUsername()),
                jwtService.createRefreshToken(user.getId(), user.getUsername()));
    }

    @Transactional
    public UserProfileResponse bindPhone(Long userId, BindPhoneRequest request) {
        SfUser user = requireUser(userId);
        String phone = request.getPhone().trim();
        SfUser existing = userMapper.selectOne(new LambdaQueryWrapper<SfUser>()
                .eq(SfUser::getPhone, phone)
                .ne(SfUser::getId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被其他账号绑定");
        }
        user.setPhone(phone);
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Transactional
    public void resetPasswordByPhone(ResetPasswordRequest request) {
        String username = normalizeUsername(request.getUsername());
        SfUser user = userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该账号未绑定手机号，请先登录后绑定");
        }
        if (!user.getPhone().equals(request.getPhone().trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号与账号不匹配");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        SfUser user = requireUser(userId);
        String path = avatarStorageService.saveAvatar(userId, file);
        user.setAvatarUrl(path);
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SfUser user = requireUser(userId);
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前账号未设置密码");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        SfUser user = requireUser(userId);
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
        String username = user.getUsername() == null ? String.valueOf(user.getId()) : user.getUsername();
        String access = jwtService.createAccessToken(user.getId(), username);
        String refresh = jwtService.createRefreshToken(user.getId(), username);
        return new LoginResponse(user.getId(), access, refresh, user.getNickname(), user.getAvatarUrl());
    }

    private UserProfileResponse toProfile(SfUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getSignature(),
                maskPhone(user.getPhone()),
                inviteService.isBadgeUnlocked(user.getId()));
    }

    private SfUser requireUser(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
