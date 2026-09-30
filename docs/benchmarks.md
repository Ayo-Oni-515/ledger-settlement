# JMH Benchmark Suite: Results

## Methods chosen

| # | Method | Why |
|---|---|---|
| 1 | `SettlementService.owed(merchantId)` (settlement calc) | The one calculation that must never lose a penny — stays on `long` minor units, not `double`, for exactly the reason Chapter 1 of the money-types lab covers. Worth knowing its real cost since it's on the hot path of every settlement request. |
| 2 | TODO — serialisation/mapping method | TODO — why this one |
| 3 | TODO — string/collection-heavy method | TODO — why this one |

## Dead code elimination: the broken benchmark

`BrokenSettlementBenchmark.calculateNet_discardsResult` calls the real
`SettlementService.calculateNet` but never uses the result.

| Run | Score | Converted to µs/op |
|---|---|---|
| Broken (discarded result, Throughput mode — no `@BenchmarkMode` set, defaulted) | 24,460,316.697 ± 1,485,339.255 ops/s | ≈ 0.0409 µs/op |
| Fixed (`SettlementBenchmark.calculateNet_fixed`, AverageTime, returned) | 0.041 ± 0.001 µs/op | 0.041 µs/op |

**Size of the error: effectively none.** The two ran in different modes
(the broken benchmark defaulted to Throughput since it had no
`@BenchmarkMode`), but once converted to the same unit, the numbers are
statistically indistinguishable — discarding the result did not make this
benchmark faster.

**Why:** `calculateNet` calls `BigDecimal.longValueExact()`, which can
throw `ArithmeticException`. A call that might throw has an observable
side effect even when its return value is unused — removing the call
would also remove the possibility of that exception, which is a real
behavioral change the JIT is not permitted to make. So unlike a trivial
loop the JIT can prove is side-effect-free, this real production method
resists dead code elimination on its own, because it can fail. The lab's
assumed failure mode (a suspiciously fast broken number) doesn't appear
here — which is itself the honest result worth reporting, not a sign the
setup is wrong.

## Why forks matter

Each `@Fork` runs the benchmark in a brand-new JVM process. Without
forking, back-to-back benchmarks in the same process share JIT
compilation history and heap/GC state, so an earlier benchmark's
warmup can quietly bias a later one's numbers — `@Fork(3)` runs the
whole thing three times in three clean JVMs and reports the combined
statistics, so no benchmark's result depends on what ran before it.

## Results

All runs: `@Warmup(iterations = 5)`, `@Measurement(iterations = 10)`,
`@Fork(3)`, `AverageTime` mode, microseconds.

| Benchmark | Score ± Error | Allocation (`-prof gc`) |
|---|---|---|
| `SettlementBenchmark.sumPennies_fixed` | TODO us/op ± TODO | TODO B/op |
| `MappingBenchmark.mapPaymentResponse` | TODO us/op ± TODO | TODO B/op |
| `LookupBenchmark.lookupByKey` | TODO us/op ± TODO | TODO B/op |

**Comparisons:** TODO — either name a specific pair being compared and
say whether their error intervals overlap (if they do: "inconclusive —
error bars overlap"), or state plainly that no comparisons were made
between these three benchmarks (they measure different things, not
variants of the same thing) and that's fine — the lab only requires
marking overlaps as inconclusive where a comparison is actually being
made.

## Limits — what this suite does not tell you

1. **Concurrency.** Every benchmark here runs single-threaded. It says
   nothing about contention, lock behaviour, or throughput when many
   virtual threads hit the same method simultaneously under real load.
2. **Production data sizes and cache effects.** The inputs here are
   small, fixed, in-memory lists/maps. A method that's fast against 10
   payments or a 500-entry map may behave very differently against a
   production-sized dataset that doesn't fit in cache, or that triggers
   different JIT inlining decisions at scale.
3. **Benchmark harness vs. live request path.** JMH measures the method
   in isolation, warmed up, with no Spring request-handling overhead
   (deserialization, validation, transaction management, network I/O,
   connection pool contention) around it — a fast microbenchmark result
   doesn't mean the real endpoint is fast.