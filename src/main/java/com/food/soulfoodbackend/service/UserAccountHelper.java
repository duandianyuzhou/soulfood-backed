package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class UserAccountHelper {

    private final SfUserMapper userMapper;

    public void ensureInviteCode(SfUser user) {
        if (user.getInviteCode() != null && !user.getInviteCode().isBlank()) {
            return;
        }
        String code;
        do {
            code = "DM" + (100000 + ThreadLocalRandom.current().nextInt(900000));
        } while (userMapper.selectOne(new LambdaQueryWrapper<SfUser>().eq(SfUser::getInviteCode, code)) != null);
        user.setInviteCode(code);
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
    }
}
