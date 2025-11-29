package com.kyy.benchmarkvirtualthreadprj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bench")
public class BenchmarkController {

    private final BlockingJobService blockingJobService;

    /**
     * 플랫폼 스레드 기반 ThreadPool에서 N개의 2초 블로킹 작업 수행
     */
    @GetMapping("/platform")
    public String platform(
            @RequestParam(defaultValue = "2000") long sleep,
            @RequestParam(defaultValue = "200") int tasks
    ) {
        log.info(">>> /bench/platform called, tasks={}, sleep={}ms, thread={}",
                tasks, sleep, Thread.currentThread().getName());

        for (int i = 0; i < tasks; i++) {
            blockingJobService.runOnPlatform(sleep);
        }

        return "platform scheduled " + tasks;
    }

    /**
     * 버추얼 스레드 기반 Executor에서 N개의 2초 블로킹 작업 수행
     */
    @GetMapping("/virtual")
    public String virtual(
            @RequestParam(defaultValue = "2000") long sleep,
            @RequestParam(defaultValue = "200") int tasks
    ) {
        log.info(">>> /bench/virtual called, tasks={}, sleep={}ms, thread={}",
                tasks, sleep, Thread.currentThread().getName());

        for (int i = 0; i < tasks; i++) {
            blockingJobService.runOnVirtual(sleep);
        }

        return "virtual scheduled " + tasks;
    }
}