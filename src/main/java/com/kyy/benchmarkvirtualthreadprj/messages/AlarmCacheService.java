package com.kyy.benchmarkvirtualthreadprj.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AlarmCacheService {

    private final StringRedisTemplate redis;

    public String getAlarm(String userId) {
        return redis.opsForValue()
                .get("alarm:user:" + userId);
    }

    public void saveAlarm(String userId, String data) {
        redis.opsForValue()
                .set("alarm:user:" + userId, data, Duration.ofDays(1));
    }
}

