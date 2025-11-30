package com.kyy.benchmarkvirtualthreadprj.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AlarmLegacyService {

    @Async("legacyExecutor")
    public CompletableFuture<String> asyncFetch(String userId) {
        return CompletableFuture.completedFuture(fetchAlarmFromLegacy(userId));
    }

    public String fetchAlarmFromLegacy(String userId) {
        // TODO: 기간계 SOAP/REST 호출
        // JSON or XML 파싱 후 JSON String으로 반환
        try { Thread.sleep(1500); } catch (Exception ignore) {}
        return """
        {
            "userId":"%s",
            "items":[{"type":"UW","msg":"UW 심사 완료"}]
        }
        """.formatted(userId);
    }
}

