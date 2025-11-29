# ✔ 결론: @Async에서도 MDC 전파 가능하다

방법은 크게 **3가지**다.

# 🔥 방법 1 — TaskDecorator 사용 (Spring 공식 권장, 가장 깔끔함)

Spring Boot 2.1+ / Spring 5+ 는
`TaskDecorator`를 통해 **MDC를 자동으로 Async Thread로 복사**할 수 있다.

### 1) Decorator 생성

```java
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

### 2) Async 용 TaskExecutor에 적용

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");

        executor.setTaskDecorator(new MdcTaskDecorator());  // ★ 요기!

        executor.initialize();
        return executor;
    }
}
```

### 3) @Async 에 붙이기

```java
@Async("asyncExecutor")
public void asyncCall() {
    log.info("MDC txId = {}", MDC.get("txId"));
}
```

### 결과

**Worker Thread MDC → Async Thread MDC 그대로 복사됨!**

---

# 🔥 방법 2 — Spring Cloud Sleuth(분산 Trace) 사용하는 경우

Sleuth를 쓰면 **자동으로 MDTracing + Baggage + MDC propagation** 된다.

하지만 Sleuth는 Spring Cloud 기반이므로
MSA 환경이 아니라면 오버스펙일 수 있음.

→ 너처럼 RestTemplate + Sync/Async 혼재 환경에서도 동작은 잘함.

그러나 단점:

* 전체 트레이싱 시그널이 붙으므로 로그가 복잡해짐
* MSA 전체에 적용되는 무거운 패턴

그래서 “MDC만 필요”한 경우 **방법 1(TaskDecorator)이 더 정답**.

---

# 🔥 방법 3 — @Async + DelegatingSecurityContextAsyncTaskExecutor

Security Context Propagation이지만, MDC는 직접 안됨.

대신 이걸 커스텀해서 MDC까지 포함 가능.

하지만 공식적으론 사용률 ↓
→ “TaskDecorator 쓰는 게 정답”.

---

# ✔ 지금 네가 하고 있는 CompletableFuture supplier 방식 vs @Async + TaskDecorator

| 항목     | CompletableFuture supplier 방식 | @Async + TaskDecorator |
| ------ | ----------------------------- | ---------------------- |
| MDC 전달 | 수동으로 넘김 (불편)                  | 자동 복사 (권장)             |
| 코드량    | 많음                            | 매우 적음                  |
| 유지보수   | 개발자 실수 발생 높음                  | 매우 안정적                 |
| 확장性    | 낮음                            | Async 전역에 적용 가능        |
| 표준성    | 낮음                            | Spring 공식 패턴           |

→ 당연히 TaskDecorator 방식이 더 정석이고 안정적임.

---

# ✔ 추가 Tip: Virtual Thread + MDC propagation

JDK21의 Virtual Thread에서도 MDC 쓸 수 있음.
ThreadLocal 기반이라 그대로 돌아가는데,
Virtual Thread 생성 시 Decorator 적용 가능.

예:

```java
ExecutorService executor = Executors.newThreadPerTaskExecutor(
    Thread.ofVirtual().name("vt-", 0)
         .inheritInheritableThreadLocals(false)
         .factory()
);
```

Virtual Thread도 TaskDecorator처럼 wrapping해서 MDC 복제 가능.

---

# ✔ 완벽 결론

* @Async도 MDC를 Worker Thread → Async Thread로 전파할 수 있음
* 가장 정석적이고 깔끔한 방식은 **TaskDecorator**
* 이 방식은 Spring 공식 지원이며 운영환경에서 많이 쓰임
* CompletableFuture로 supplier 넘길 필요 없음
* Virtual Thread 환경에서도 동일 개념으로 확장 가능

---

원하면

* Virtual Thread + MDC Complete Example
* @Async + MDC Propagation End-to-End 샘플 프로젝트
* Mermaid 다이어그램으로 동작 원리
  이것도 만들어줄게.
