package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final UploadProperties uploadProperties;

    public String saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片");
        }
        if (file.getSize() > uploadProperties.getMaxAvatarBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG/PNG/WebP");
        }

        String ext = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        try {
            Path dir = Paths.get(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(userId + "." + ext);
            file.transferTo(target);
            return "/uploads/avatars/" + userId + "." + ext;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL, "头像保存失败");
        }
    }
}
