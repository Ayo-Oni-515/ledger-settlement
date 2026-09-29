# Virtual Thread Migration: Results

## Setup
- Load: k6, 200 VUs, 60 s, `GET /payments/settlement?merchantId=MR-4471`
- Service: Ledger settlement, Java 25, Spring Boot 4.1
- Baseline: platform threads (Tomcat defaults), warm start

## Current pooling

| Pool | Core | Max | Queue |
|---|---|---|---|
| Tomcat (serves the endpoint) | 10 | 200 | 100 (accept-count) |
| appExecutor (ThreadPoolTaskExecutor) | 8 | 16 | 100 |

## Results

| Run | Throughput (req/s) | p50 | p95 | p99 | Verdict |
|---|---|---|---|---|---|
| Baseline (platform threads) | 3697.79 | 33.52 ms | 114.69 ms | 383.78 ms | n/a |
| Virtual threads | 3280.37 | 37.57 ms | 151.57 ms | 411.46 ms | worse |
| Pinned (cold, with defect) | 2959.38 | 41.60 ms | 166.26 ms | 449.91 ms | worse (pinning) |
| Fixed (cold, after fix) | 3124.16 | 41.03 ms | 200.99 ms | 339.92 ms | fixed |

### Analysis: why virtual threads alone were worse here

Throughput dropped (3697.79 → 3280.37 req/s) and every latency percentile
got worse after switching to virtual threads. This is a plausible and
common outcome, not a sign the migration was done wrong: virtual threads
remove Tomcat's own thread-count ceiling, but they don't remove ceilings
further downstream. If the DB connection pool (`hikari.maximum-pool-size`)
or the downstream client's connection pool is still capped at, say, 10,
then requests that used to queue politely behind Tomcat's bounded thread
pool now queue behind that pool instead — with virtual-thread scheduling
overhead added on top. Worth checking `hikari.maximum-pool-size` and the
downstream client's pool size before concluding virtual threads "didn't
help": the fix here is very likely to raise that pool's ceiling, not to
revert the thread-per-task change.

Throughput on the pinned cold-start run (2959.38 req/s) sits below both
earlier runs, and p99 (449.91 ms) is the worst of the three — consistent
with the 2.37s class-init stall and the cascade of requests waiting behind
it, captured below.

## Pinning defect

Planted by moving the downstream lookup-table fetch into `LookupTable`'s
static initializer, so the first request that touches the class blocks
inside `<clinit>`.

```
jdk.VirtualThreadPinned {
  startTime = 20:08:03.410 (2026-09-29)
  duration = 2.37 s
  blockingOperation = "LockSupport.park"
  pinnedReason = "VM call to com.example.ledger.LookupTable.<clinit> on stack"
  carrierThread = "ForkJoinPool-1-worker-3" (javaThreadId = 58)
  eventThread = "tomcat-handler-169" (javaThreadId = 230, virtual)
  stackTrace = [
    java.lang.VirtualThread.parkOnCarrierThread(boolean, long) line: 830
    java.lang.VirtualThread.parkNanos(long) line: 798
    java.lang.VirtualThread.sleepNanos(long) line: 980
    java.lang.Thread.sleepNanos(long) line: 507
    java.lang.Thread.sleep(long) line: 540
  ]
}
```

**Frame that cannot unmount:** `com.example.ledger.LookupTable.<clinit>` —
named directly in `pinnedReason`. The JVM holds a native class-initialization
lock while this frame is on the stack, so the virtual thread cannot be
unmounted from its carrier no matter what runs underneath it.

**Call that blocks inside it:** the simulated downstream fetch
(`Thread.sleep`, standing in for `client.fetchTable()`), 2.37 s.

Eight further concurrent requests pinned with
`pinnedReason = "Waited for initialization of com.example.ledger.LookupTable
by another thread"` — every request that arrives while the first is still
inside `<clinit>` stalls too, not just the unlucky first request.

**Why `synchronized` → `ReentrantLock` (pre-Java 24 advice) doesn't help
here:** that guidance predates Java 24, when `synchronized` itself pinned
the carrier on every block. There is no `synchronized` anywhere in this
stack — the pin is the JVM's native class-initialization lock around
`<clinit>`, which `ReentrantLock` has no effect on.

## Fix

Moved the downstream fetch out of `LookupTable`'s static initializer.
`LookupTable` is now a `@Component` with an instance constructor that runs
the fetch once, at application startup, before Tomcat accepts any traffic.
`LookupTable.TABLE.get(...)` was replaced with an injected
`lookupTable.table().get(...)` at its one call site, in
`PaymentController.settlement`.

Verified on a second cold restart, capturing `fixed.jfr` across the same
200 VU / 60s load: `jfr print --events jdk.VirtualThreadPinned fixed.jfr`
still reports pin events, but every one of them is Hibernate/HQL-internal
(`HqlLexer`/`HqlParser` grammar initialization, `Contended monitor enter`
during query translation) — pre-existing framework-level pinning unrelated
to this defect. **No event mentions `LookupTable`.** Throughput on this run
(3124.16 req/s) is back above the pinned-defect run and close to the plain
virtual-threads run, and p99 (339.92 ms) is the best of all four runs,
consistent with the 2.37s class-init stall being gone.