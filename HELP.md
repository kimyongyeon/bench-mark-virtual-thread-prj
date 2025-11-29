yongyeonkim  ~/dev/bench-mark-virtual-thread-prj   master ±✚  wrk -t8 -c200 -d20s "http://localhost:8080/bench/virtual-block?sleep=2000"

Running 20s test @ http://localhost:8080/bench/virtual-block?sleep=2000
8 threads and 200 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
Latency    37.95ms  151.56ms   1.21s    95.58%
Req/Sec     3.56k   799.11     5.81k    69.31%
536013 requests in 20.03s, 70.13MB read
Requests/sec:  26762.89
Transfer/sec:      3.50MB
yongyeonkim  ~/dev/bench-mark-virtual-thread-prj   master ±✚  wrk -t8 -c200 -d20s "http://localhost:8080/bench/platform-block?sleep=2000"

Running 20s test @ http://localhost:8080/bench/platform-block?sleep=2000
8 threads and 200 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
Latency     0.00us    0.00us   0.00us     nan%
Req/Sec     2.43      1.50     6.00     73.42%
492 requests in 20.06s, 63.90KB read
Socket errors: connect 0, read 0, write 0, timeout 492
Requests/sec:     24.53
Transfer/sec:      3.19KB

🔍 왜 이 결과가 Virtual Thread의 완전한 증명인가?
① Platform Thread 버전 (platform-block)

Tomcat worker threads(기본 200개)

요청 200개가 동시에 들어오면
→ worker thread 200개 모두 Thread.sleep(2000) 으로 block됨

wrk는 더 이상 요청을 처리할 worker가 없어서 타임아웃

즉:

❌ 서버 처리 불가
❌ Requests/sec: 24.53
❌ 492 timeout
❌ 사실상 서버 down 상태

이게 바로 Blocking I/O + Platform Thread의 한계다.

② Virtual Thread 버전 (virtual-block)

요청 처리(컨트롤러)는 worker thread에서 즉시 리턴

실제 blocking 작업은 Virtual Thread 안에서 실행

Virtual Thread는 sleep(2000ms) 중 OS thread를 반납

동시 200개가 가도 worker thread는 항상 free 상태

Virtual Threads는 수천 개도 안정적

결과:

✔ Requests/sec: 26,762 ← 1100배 이상 높음
✔ Timeout 없음
✔ 서버는 부하를 그대로 소화
✔ Virtual Thread의 핵심 철학 그대로 재현됨



yongyeonkim  ~/dev/bench-mark-virtual-thread-prj   master ±✚   wrk -t8 -c200 -d20s "http://localhost:8080/bench/virtual"            
Running 20s test @ http://localhost:8080/bench/virtual
8 threads and 200 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
Latency   691.98ms  714.82ms   1.99s    76.85%
Req/Sec    38.48     51.36   670.00     92.35%
5070 requests in 20.10s, 668.41KB read
Socket errors: connect 0, read 0, write 0, timeout 367
Requests/sec:    252.21
Transfer/sec:     33.25KB

2025-11-29T10:01:23.470+09:00  INFO 279 --- [bench-mark-virtual-thread-prj] [virtual-1043356] c.k.b.BlockingJobService                 : [VIRTUAL] end, thread=

yongyeonkim  ~/dev/bench-mark-virtual-thread-prj   master ±✚   wrk -t8 -c200 -d20s "http://localhost:8080/bench/platform?sleep=2000&tasks=50"
Running 20s test @ http://localhost:8080/bench/platform?sleep=2000&tasks=50
8 threads and 200 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
Latency    21.64ms   24.69ms 359.58ms   91.64%
Req/Sec     1.33k   306.94     2.24k    70.15%
210932 requests in 20.10s, 51.09MB read
Non-2xx or 3xx responses: 210932
Requests/sec:  10493.38
Transfer/sec:      2.54MB


java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.FutureTask@6c87bf84[Not completed, task = org.springframework.aop.interceptor.AsyncExecutionInterceptor$$Lambda/0x0000000301508b90@32dfbb14] rejected from org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor$1@66b59b7d[Running, pool size = 50, active threads = 50, queued tasks = 100, completed tasks = 450]


RestTemplate는 blocking I/O라서
→ 요청 하나마다 thread 1개를 반드시 점유해야 한다.
그런데 지금은:
50개는 이미 sleep/RestTemplate로 blocking 중
100개는 큐에서 대기 중
그 뒤 들어오는 요청은 큐에도 못 들어가고 reject됨

👉 RestTemplate 호출 자체가 수행되지 못하고 튕겨나감.

🧠 운영에서는 이 상황이 어떻게 보일까?
이 패턴 그대로 운영에서 일어나면:

🔥 1) 외부 API 호출 요청은 아예 서버에서 던지지도 못함
→ "외부 서버 타임아웃"처럼 보이지만 사실 “내 서버가 호출조차 못함”
🔥 2) 내부 에러가 쌓임 (RejectedExecutionException)
🔥 3) API 응답 시간 폭발
→ Tomcat thread들이 async 호출 시도 과정에서 지연됨
🔥 4) 점점 서버가 응답을 못하게 됨
→ Health check 실패
→ 파드 재시작 반복 (CrashLoop)
🔥 5) 레이스 상황 발생
→ 어떤 API는 timeout, 어떤 API는 정상
→ 간헐적 5초/10초 지연

