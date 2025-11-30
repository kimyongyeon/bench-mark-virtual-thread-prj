package com.kyy.benchmarkvirtualthreadprj.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AlarmScheduler {

    private final AlarmLegacyService legacyService;
    private final AlarmCacheService cacheService;
    private final StringRedisTemplate redis;

    @Scheduled(fixedDelay = 300000) // 5분
    public void refresh() {
        List<String> users = loadActiveUsers(); // Redis나 DB에서 조회

        for (String userId : users) {
            String newData = legacyService.fetchAlarmFromLegacy(userId);
            String oldData = cacheService.getAlarm(userId);

            if (!Objects.equals(newData, oldData)) {
                cacheService.saveAlarm(userId, newData);
                redis.convertAndSend(
                        "alarm:update:" + userId,
                        newData
                );
            }
        }
    }

    public List<String> loadActiveUsers() {
        return new ArrayList<>(redis.opsForSet().members("active-users"));
    }


}
