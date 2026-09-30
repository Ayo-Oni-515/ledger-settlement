# Ledger Benchmarks

JMH microbenchmarks against real methods in the Ledger settlement
service: the settlement fee calculation, payment→response mapping, and
a merchant lookup. See [`docs/benchmarks.md`](../docs/benchmarks.md) for
results, analysis, and limits.

## Build and run

From this directory (`benchmarks/`):

```bash
./run.sh
```

That single command:
1. Installs the main `ledger-settlement` app to your local Maven repo
   (`mvn clean install -DskipTests`, run from the repo root)
2. Builds this module into `target/benchmarks.jar`
3. Runs the full JMH suite with `-prof gc` allocation profiling, writing
   `results.json`

On Windows via Git Bash / MINGW, run it the same way:
```bash
bash run.sh
```

## Running a single benchmark

To run just one benchmark class instead of the full suite:

```bash
mvn clean package
java -cp target/benchmarks.jar org.openjdk.jmh.Main SettlementBenchmark
```

Replace with `MappingBenchmark` or `LookupBenchmark` as needed.

## Requirements

- Java 25
- Maven
- The main `ledger-settlement` project must be buildable on its own
  first (`run.sh` handles this automatically)
