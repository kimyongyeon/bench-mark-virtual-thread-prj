# 📄 **Virtual Thread vs Platform Thread Blocking I/O 벤치마크 기술 분석 문서**

## 1. 개요

본 문서는 Spring + JDK21 환경에서 **Platform Thread 기반 @Async + Blocking I/O(RestTemplate)**과
**Virtual Thread 기반 Blocking I/O**의 차이를 실제 벤치마크로 비교한 결과를 정리한 분석 자료이다.

테스트는 다음 3가지 시나리오로 구성되었다.

1. Platform Thread 기반 @Async + Blocking I/O
2. Virtual Thread 기반 Blocking I/O
3. 두 방식 모두 sleep / blocking 작업을 포함한 고부하 wrk 테스트

본 문서를 통해 다음을 명확히 확인할 수 있다.

* Platform Thread는 thread exhaustion으로 인해 서버 내부에서 RejectedExecutionException이 발생한다.
* Virtual Thread는 blocking I/O가 많아져도 OS thread 고갈 없이 안정적으로 확장된다.
* wrk 결과는 Platform Thread가 더 빠르게 보이지만, 실제로는 내부 실패로 인해 “착시 효과”가 발생한다.
* Virtual Thread는 서버는 안정적이지만 클라이언트(wrk)의 timeout이 증가하는 방식으로 부하가 드러난다.

---

## 2. 테스트 환경 요약

* JDK 21 (Virtual Thread 사용 가능)
* Spring Boot 3.x (@Async = Platform Thread 기반 TaskExecutor)
* RestTemplate Blocking I/O 시뮬레이션
* wrk 부하 테스트

    * thread: 8
    * connection: 200
    * duration: 20 seconds

---

## 3. 핵심 비교 요약

### 📌 Platform Thread (@Async + RestTemplate)

* pool size = 50
* queue size = 100
* 150개 이상 동시 blocking → **즉시 폭발**
* 내부에서 RejectedExecutionException 연속 발생
* 컨트롤러는 바로 응답하므로 wrk에서는 timeout이 없음
* wrk RPS가 매우 높게 찍히는 **착시 현상** 발생
* 실제 작업은 거의 실행되지 않음

### 📌 Virtual Thread (StructuredConcurrency or Executors.newVirtualThreadPerTaskExecutor)

* blocking workload가 많아도 thread가 가볍게 확장됨
* 서버 내부 오류 없음
* wrk에서 client-side timeout 증가
  → 응답이 늦어지는 것일 뿐, 서버는 정상
* 서버는 절대 죽거나 thread가 고갈되지 않음

---

## 4. 실측 결과 요약

### 4.1 Platform Thread 결과

* wrk 출력

  ```
  Requests/sec: 10493
  timeout 없음
  ```
* 서버 내부 로그

  ```
  RejectedExecutionException
  pool size=50, active 50, queue 100 → 모두 꽉 참
  RestTemplate 호출 자체가 실행 불가
  ```

→ **wrk는 응답이 빠르게 리턴되므로 정상처럼 보이지만, 실제 서버는 내부에서 작업을 처리하지 못하고 붕괴 상태**

---

### 4.2 Virtual Thread 결과

* wrk 출력

  ```
  Requests/sec: 252
  Socket timeout 367
  ```
* 서버 내부 로그

  ```
  에러 없음
  deadlock 없음
  thread exhaustion 없음
  ```

→ **서버는 안정적으로 처리 중이며 응답이 늦어지면서 wrk가 timeout**

---

## 5. 두 결과의 의미

### 📌 Virtual Thread timeout = 정상 동작

이는 서버가 느려서가 아니라 **클라이언트(wrk)가 오래 기다려서 타임아웃**된 것.

### 📌 Platform Thread timeout 없음 = 서버 내부 폭발

컨트롤러는 즉시 응답하므로 wrk는 정상으로 보이지만
내부에서 실제 작업은 실행되지 못하고 reject됨.

---

## 6. 왜 이런 차이가 발생하는가?

### 6.1 Platform Thread의 본질

* OS Thread = 매우 비싸고 수가 적다
* blocking I/O가 길어지면 thread가 빠르게 고갈된다
* 고갈되면 더 이상 작업을 받을 수 없다
* Spring TaskExecutor는 queue + thread가 꽉 차면 모든 요청이 reject된다

### 6.2 Virtual Thread의 본질

* OS Thread에 바인딩되지 않는다
* sleep / socketRead / file IO 등 blocking 상태여도 user-mode에서 parking 가능
* 필요한 만큼 빠르게 증가 → 수천~수만 단위 생성 가능
* blocking 작업이 쌓여도 서버는 죽지 않는다
* 대신 응답이 길어져 client timeout이 발생

---

## 7. 비교 표

| 항목                       | Platform Thread            | Virtual Thread |
| ------------------------ | -------------------------- | -------------- |
| 서버 내부 안정성                | 낮음 (thread exhaustion)     | 매우 높음          |
| RestTemplate blocking 처리 | 불가 (빠르게 고갈)                | 처리 가능          |
| wrk 결과                   | RPS 높음 (착시)                | timeout 증가     |
| 내부 오류                    | RejectedExecutionException | 없음             |
| 서버 다운 위험                 | 높음                         | 거의 없음          |
| blocking I/O 최적화         | 필수 (WebClient 필요)          | 필수 아님          |

---

## 8. 결론

### 🎯 Virtual Thread를 사용하면 다음이 보장된다

1. Blocking I/O가 많아도 서버가 죽지 않는다
2. thread exhaustion이 사라진다
3. 안정성이 폭발적으로 증가한다

### 🎯 Platform Thread는 대량의 blocking 요청을 받으면 서버가 붕괴한다

* thread pool exhaustion
* queue overflow
* rejected task 폭발
* 실질적으로 API는 수행되지 못함

### 🎯 wrk에서 Platform Thread가 빠르게 보인 것은 착시다

실제 작업은 거의 실행되지 않는다.

---

## 9. 최종 요약

* Virtual Thread는 서버 안정성 관점에서 압도적으로 우수하다
* Platform Thread는 blocking I/O 환경에서 사용하면 절대 안 된다
* wrk 결과만 보거나 RPS만 보면 오해할 수 있으나
  실측 로그 분석을 통해 Virtual Thread의 안정성이 확실하게 입증되었다

---
