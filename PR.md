## Change
Migrated the settlement endpoint to virtual threads
(spring.threads.virtual.enabled + a VT-per-task executor bean).
Found and fixed a pinning defect: LookupTable was loading its
downstream fetch inside a static initializer, which pinned every
virtual thread touching the class for 2.37s on cold start. Moved
the fetch into a @Component constructor so it runs once at startup,
before any request lands.

## Risk
Startup now depends on the downstream client being reachable — if
it's down, the app won't start (previously it would start fine and
only fail on the first request that hit the class).

## Rollback
Revert this commit. Restores the static-initializer version of
LookupTable and the static field access in PaymentController.

## Verification
docs/threads.md has before/after throughput and latency, the
captured pinned.jfr event, and confirmation that fixed.jfr shows
no LookupTable pins (residual Hibernate-internal pins are
unrelated, noted in the doc).