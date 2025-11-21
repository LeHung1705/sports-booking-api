package com.example.booking_api.service;

import com.example.booking_api.repository.FcmTokenRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository tokenRepo;

    public void sendToTokens(List<String> tokens, String title, String body, Map<String,String> data) {
        if (tokens == null || tokens.isEmpty()) return;

        MulticastMessage msg = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Map.of() : data)
                .build();

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendMulticast(msg);

            IntStream.range(0, resp.getResponses().size()).forEach(i -> {
                var r = resp.getResponses().get(i);
                if (!r.isSuccessful()) {
                    var err = String.valueOf(r.getException().getMessagingErrorCode());
                    var bad = tokens.get(i);
                    if ("UNREGISTERED".equals(err) || "INVALID_ARGUMENT".equals(err)) {
                        tokenRepo.deleteByToken(bad); // 🧹 cleanup token chết
                    }
                }
            });
        } catch (Exception ignore) {}
    }
}
