package com.kyy.benchmarkvirtualthreadprj.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AlarmSseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AlarmCacheService cacheService;
    private final AlarmLegacyService legacyService;
    private final StringRedisTemplate redis;

    public SseEmitter connect(String userId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5분 유지
        emitters.put(userId, emitter);

        redis.opsForSet().add("logged-in-users", userId);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            redis.opsForSet().remove("logged-in-users", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            redis.opsForSet().remove("logged-in-users", userId);

        });

        // 1. Redis 캐시 조회
        String cached = cacheService.getAlarm(userId);
        if (cached != null) {
            sendToClient(userId, cached, "alarm-init");
        }

        // 2. 기간계 조회는 비동기 처리
        CompletableFuture.runAsync(() -> {
            String latest = legacyService.fetchAlarmFromLegacy(userId);
            cacheService.saveAlarm(userId, latest);
            sendToClient(userId, latest, "alarm-init");
        });

        return emitter;
    }

    public void sendAllClient(String message) {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("broadcast")
                                .data(message)
                );
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(key);
            }
        });
    }

    public void sendToClient(String userId, String data, String event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(event)
                            .data(data)
            );
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }

    /** ⭐ Controller에서 직접 호출할 disconnect API */
    public void disconnect(String userId) {
        cleanup(userId);
    }

    /** 내부 정리 메소드 */
    private void cleanup(String userId) {
        SseEmitter em = emitters.remove(userId);
        if (em != null) {
            try {
                em.complete();
            } catch (Exception ignored) {}
        }
        redis.opsForSet().remove("logged-in-users", userId);
    }
}
