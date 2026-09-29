# Virtual Thread Migration: Results

**Setup:** k6, 200 VUs, 60s, `GET /payments/settlement?merchantId=MR-4471`
Java 25, Spring Boot 4.1.

**Pooling before migration**

| Pool | Core | Max | Queue |
|---|---|---|---|
| Tomcat | 10 | 200 | 100 |
| appExecutor | 8 | 16 | 100 |

## Results

| Run | Throughput | p50 | p95 | p99 |
|---|---|---|---|---|
| Baseline (platform threads) | 3697.79 | 33.52 ms | 114.69 ms | 383.78 ms |
| Virtual threads | 3280.37 | 37.57 ms | 151.57 ms | 411.46 ms |
| Pinned (cold, with defect) | 2959.38 | 41.60 ms | 166.26 ms | 449.91 ms |
| Fixed (cold) | 3124.16 | 41.03 ms | 200.99 ms | 339.92 ms |

Virtual threads alone came in a bit worse than platform threads. Not
surprising — removing Tomcat's thread ceiling doesn't help if something
downstream (DB pool, HTTP client pool) is still capped low; requests just
queue there instead. Worth checking `hikari.maximum-pool-size` before
assuming VTs "didn't help."

## The pinning bug

Moved the lookup-table fetch into `LookupTable`'s static initializer, so
the first request to touch the class blocks inside `<clinit>`.

```
jdk.VirtualThreadPinned {
  duration = 2.37 s
  blockingOperation = "LockSupport.park"
  pinnedReason = "VM call to com.example.ledger.LookupTable.<clinit> on stack"
  stackTrace = [
    java.lang.VirtualThread.parkOnCarrierThread(boolean, long) line: 830
    java.lang.VirtualThread.parkNanos(long) line: 798
    java.lang.VirtualThread.sleepNanos(long) line: 980
    java.lang.Thread.sleepNanos(long) line: 507
    java.lang.Thread.sleep(long) line: 540
  ]
}
```

**Frame that can't unmount:** `LookupTable.<clinit>` — the JVM holds a
native class-init lock the whole time that frame's on the stack, so the
thread can't be unmounted no matter what's happening underneath.

**What's blocking:** the downstream fetch inside it (`Thread.sleep` here,
standing in for `client.fetchTable()`), 2.37s. Eight other requests piled
up behind it waiting for the same class to finish initializing.

**Why `synchronized` → `ReentrantLock` wouldn't fix this:** that advice is
from before Java 24, when `synchronized` itself pinned the carrier. There's
no `synchronized` in this stack — it's the JVM's own class-init lock, which
a `ReentrantLock` has no power over.

## The fix

`LookupTable` is now a `@Component` — the fetch happens once in its
constructor, at startup, before Tomcat takes any traffic. Swapped the one
call site (`PaymentController.settlement`) from the static field to the
injected bean.

`LookupTableConcurrencyTest` hits the bean with 32 threads at once and
checks they all get the same map instance (identity, not just equal
values). Worth noting: since the fetch now happens at startup, the bean's
already built by the time any thread touches it — so there's no real race
left to catch. The test mainly confirms reads stay stable, which is the
right thing to prove once the actual race is gone.

Re-ran cold with `fixed.jfr` capturing: still shows pin events, but they're
all Hibernate's own (`HqlLexer`/`HqlParser` init) — nothing about
`LookupTable`. That's a pre-existing Hibernate quirk, not something this
service owns or this lab asked me to fix, so it's out of scope here.
Throughput recovered to 3124.16 req/s and p99 dropped to 339.92 ms — the
best of all four runs.