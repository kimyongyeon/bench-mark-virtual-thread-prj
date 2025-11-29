package com.kyy.benchmarkvirtualthreadprj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/bench")
public class BenchController {

    /**
     * PLATFORM THREAD BLOCKING
     * Tomcat worker thread에서 Thread.sleep 실행
     */
    @GetMapping("/platform-block")
    public String platformBlocking(
            @RequestParam(defaultValue = "2000") long sleep
    ) {
        String thread = Thread.currentThread().toString();
        log.info("[PLATFORM] start thread={}", thread);

        // Tomcat Worker Thread에서 직접 blocking
        try {
            Thread.sleep(sleep); // 외부 API 블로킹과 동일
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[PLATFORM] end thread={}", thread);
        return "platform block done";
    }

    /**
     * VIRTUAL THREAD BLOCKING
     * Virtual Thread 안에서 Thread.sleep 실행
     */
    @GetMapping("/virtual-block")
    public String virtualBlocking(
            @RequestParam(defaultValue = "2000") long sleep
    ) {
        String callerThread = Thread.currentThread().toString();
        log.info("[VIRTUAL] request received on thread={}", callerThread);

        // Virtual Thread 생성
        Thread.ofVirtual().start(() -> {
            String vThread = Thread.currentThread().toString();
            log.info("[VIRTUAL] start virtualThread={}", vThread);

            try {
                Thread.sleep(sleep); // blocking but OS thread 반납됨
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("[VIRTUAL] end virtualThread={}", vThread);
        });

        // Virtual Thread는 백그라운드에서 돌고,
        // HTTP 응답은 즉시 리턴
        return "virtual block scheduled";
    }
}