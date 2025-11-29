package com.kyy.benchmarkvirtualthreadprj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BlockingJobService {

    // 플랫폼 스레드 기반 비동기
    @Async("platformExecutor")
    public void runOnPlatform(long sleepMillis) {
        log.info("[PLATFORM] start, thread={}", Thread.currentThread().getName());
        try {
            Thread.sleep(sleepMillis); // 여기 자리에 RestTemplate 외부호출 들어간다고 보면 됨
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[PLATFORM] end, thread={}", Thread.currentThread().getName());
    }

    // 버추얼 스레드 기반 비동기
    @Async("virtualExecutor")
    public void runOnVirtual(long sleepMillis) {
        log.info("[VIRTUAL] start, thread={}", Thread.currentThread().getName());
        try {
            Thread.sleep(sleepMillis); // 여기 자리에 RestTemplate 외부호출 들어간다고 보면 됨
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[VIRTUAL] end, thread={}", Thread.currentThread().getName());
    }

    private void simulateBlocking(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis); // 여기 자리에 RestTemplate 외부호출 들어간다고 보면 됨
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}