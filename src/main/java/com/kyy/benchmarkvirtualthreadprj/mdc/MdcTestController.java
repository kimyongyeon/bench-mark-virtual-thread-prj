package com.kyy.benchmarkvirtualthreadprj.mdc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MdcTestController {

    private final MdcTestService mdcTestService;

    @GetMapping("/test")
    public String test() {

        String txId = UUID.randomUUID().toString();
        MDC.put("txId", txId);

        log.info("Worker Thread: {}, txId={}",
                Thread.currentThread().getName(),
                MDC.get("txId"));

        mdcTestService.asyncLogic();  // ★ @Async 호출

        return "OK - txId=" + txId;
    }
}