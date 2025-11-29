package com.kyy.benchmarkvirtualthreadprj.mdc;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MdcTestService {

    @Async("mdcAsyncExecutor")
    public void asyncLogic() {
        log.info("Async Thread: {}, txId={}",
                Thread.currentThread().getName(),
                MDC.get("txId")   // ★ 기대: Worker Thread와 같은 값
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }
}
