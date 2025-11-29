package com.kyy.benchmarkvirtualthreadprj.mdc;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap(); // ★ Worker Thread의 MDC 보관

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap); // ★ Async Thread로 복사
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
