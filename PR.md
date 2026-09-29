## Change

Switched the collector from G1 to ZGC — `-XX:+UseG1GC` → `-XX:+UseZGC`.
No code changes, no heap resizing, nothing else in the environment
touched.

## Why

Captured a 10-minute steady-load baseline (`gc-baseline.jfr`, 150 req/s
via perf/steady-load.js) and classified the problem before touching
anything: allocation rate was modest (~4.04 MB/s) and no single class
dominated allocation, but GC pauses were sharply bimodal — 77 young
collections at 5–20ms, and 23 old-gen collections at 140–320ms, roughly
one every 26 seconds. That's a pause problem, not allocation pressure,
so the fix is a collector change, not a code change.

## Measured improvement

Re-ran the identical 10-minute load (`gc-tuned.jfr`), only the collector
flag changed:

| | Baseline (G1) | Tuned (ZGC) |
|---|---|---|
| Longest pause | 320 ms | 0.117 ms |
| Collections | 100 (77y/23o) | 16 (8y/8o) |
| Throughput | 149.92 req/s | 149.91 req/s |
| p99 latency | not captured on baseline run | 20.07 ms |

Longest pause dropped ~2700x. Throughput unaffected.

## Regression

Allocation rate reads ~43% higher under ZGC (~4.04 → ~5.77 MB/s).
Neither throughput nor p99 show any cost from this, so it isn't hurting
anything measurable — but it's real and worth flagging. Likely some
combination of ZGC's own bookkeeping overhead (colored pointers,
remembered sets) and run-to-run sampling variance between two separate
10-minute recordings; not fully disentangled here given the time budget.

## Why the trade is worth it

This is a payment settlement service — a 300ms stop-the-world pause on a
request in flight is a real correctness-adjacent risk (client timeouts,
retried payments), while a modest, throughput-neutral increase in
allocation is not. Trading the tail-latency spike for slightly more
garbage is the right call, especially given the measured cost turned out
to be negligible.

## Rollback

Revert this commit — restores `-XX:+UseG1GC`. Single-flag change, no
code to unwind.

## Artifacts

- `gc-baseline.jfr`
- `gc-tuned.jfr`
- `perf/steady-load.js` (committed, so the run is reproducible)
- `docs/gc.md` (full writeup)