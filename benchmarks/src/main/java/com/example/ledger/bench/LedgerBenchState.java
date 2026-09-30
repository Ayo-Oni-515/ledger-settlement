package com.example.ledger.bench;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.ledger.PaymentEntity;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LedgerBenchState {

    public long totalMinor;
    public BigDecimal feeRate;
    public PaymentEntity paymentEntity;
    public Map<String, String> merchantCurrencyByCode;
    public String lookupKey;

    @Setup
    public void setup() {
        totalMinor = 128450L;
        feeRate = new BigDecimal("0.031"); // matches ledger.fee-rate in application.yaml

        paymentEntity = new PaymentEntity(
                UUID.randomUUID().toString(),
                "MR-4471",
                128450L,
                "GBP",
                Instant.now()
        );

        merchantCurrencyByCode = new HashMap<>();
        for (int i = 0; i < 500; i++) {
            merchantCurrencyByCode.put("MR-" + (4000 + i), i % 3 == 0 ? "GBP" : (i % 3 == 1 ? "USD" : "EUR"));
        }
        lookupKey = "MR-4471";
    }
}