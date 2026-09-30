#!/usr/bin/env bash
# Builds the main app, builds the benchmarks module, and runs the full
# JMH suite with JSON output and allocation profiling.
#
# Run from the benchmarks/ directory:
#   ./run.sh
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Installing ledger-settlement to local repo..."
(cd .. && mvn -q clean install -DskipTests)

echo "==> Building benchmarks module..."
mvn -q clean package

echo "==> Running JMH suite (this takes a while — 3 benchmarks x 3 forks x 15 iterations each)..."
java -jar target/benchmarks.jar -rf json -rff results.json -prof gc

echo "==> Done. Results written to benchmarks/results.json"
