package com.example.booking_api.service;

import com.example.booking_api.entity.FcmToken;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.FcmTokenRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final FcmTokenRepository tokenRepo;
    private final UserRepository userRepo;

    @Transactional
    public void registerToken(String firebaseUid, String token, String device) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }

        User user = userRepo.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        tokenRepo.findByToken(token).ifPresentOrElse(
                existed -> {
                    // chuyển quyền sở hữu token nếu khác user
                    if (!Objects.equals(existed.getUser().getId(), user.getId())) {
                        existed.setUser(user);
                    }
                    if (device != null) existed.setDevice(device);
                    tokenRepo.save(existed);
                },
                () -> tokenRepo.save(
                        FcmToken.builder()
                                .user(user)
                                .token(token)
                                .device(device)
                                .build()
                )
        );
    }

    /**
     * Xóa token theo đúng chủ sở hữu hiện tại.
     *
     * @return số hàng bị xóa (0 hoặc 1)
     */
    @Transactional
    public long unregisterToken(String firebaseUid, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        User user = userRepo.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return tokenRepo.findByUser_IdAndToken(user.getId(), token)
                .map(ft -> {
                    tokenRepo.delete(ft);
                    return 1L;
                })
                .orElse(0L);
    }
}