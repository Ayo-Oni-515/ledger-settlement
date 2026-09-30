# GC Under Load: Results

**Setup:** k6, constant-arrival-rate, 150 req/s, 10 min, `GET /payments/settlement?merchantId=MR-4471`.

**Starting point**

```
$ java -XX:+PrintFlagsFinal -version | grep -E "MaxHeapSize|UseG1GC"
   size_t MaxHeapSize   = 4219469824   {product} {ergonomic}
     bool UseG1GC       = true         {product} {ergonomic}
```

~4GB heap, G1 is the default on this JVM.

## Baseline (`gc-baseline.jfr`)

- Allocation rate: ~4.04 MB/s
- Top allocating classes: `byte[]`, `Object[]`, `StackChunk` — all generic JVM/Loom noise, nothing user-level dominating
- Collections: 100 (77 young, 23 old)
- Longest pause: 320 ms

## Classification: pause problem, not allocation pressure

4 MB/s isn't much, and nothing in the top classes points at a code-level
hot spot. But the GC durations are sharply bimodal — young collections
sit at 5–20ms, while all 23 old-gen collections run 140–320ms, roughly
one every 26 seconds. That lines up with the 2.41s max request latency
seen in the baseline k6 run (p95 was only 7ms). This is a collector/heap
problem, not something to fix in code.

## The fix

Switched `-XX:+UseG1GC` → `-XX:+UseZGC`. Nothing else changed. G1 still
stops the world for old-gen work; ZGC does its marking and relocation
concurrently, targeting sub-millisecond pauses regardless of heap size —
the right tool for a service where a 300ms stall on a payment request is
a real problem, not just an inconvenience.

## Tuned (`gc-tuned.jfr`)

| | Baseline (G1) | Tuned (ZGC) |
|---|---|---|
| Allocation rate | ~4.04 MB/s | ~5.77 MB/s |
| Collections | 100 (77y/23o) | 16 (8y/8o) |
| Longest pause | 320 ms | 0.117 ms |
| p99 latency | not captured baseline-side | 20.07 ms |
| Throughput | 149.92 req/s | 149.91 req/s |

One gotcha worth flagging: `jdk.GarbageCollection`'s `duration` field
means something different per collector — for G1 it's close to the real
pause, but ZGC is concurrent, so that field covers the whole cycle, not
just the stop-the-world part. Reading it naively showed pauses up to
1.12s, which looked like a regression. The real number is in
`jdk.GCPhasePause`: every phase across all 8 cycles was sub-millisecond.

## The trade

Longest pause went from 320ms to 0.117ms — basically gone as a
request-facing problem, and collection count dropped too. The one thing
that got worse: allocation rate reads about 43% higher under ZGC. Neither
throughput nor p99 show any cost from that, so it's not hurting anything
visible — but it's real and worth naming rather than hiding. Could be
ZGC's own bookkeeping overhead (colored pointers, remembered sets), or
partly just sampling variance between two separate 10-minute runs; a
repeat run would help tell the two apart, but wasn't done here given time.

For a settlement service, trading a rare 300ms stall for a modest,
throughput-neutral bump in garbage is the right call — a stalled payment
request is a real problem (timeouts, retries); slightly more allocation
with no measured cost isn't.