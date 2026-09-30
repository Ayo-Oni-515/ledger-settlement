package com.example.ledger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private final PaymentRepository repo;
    private final BigDecimal feeRate;

    public SettlementService(PaymentRepository repo, @Value("${ledger.fee-rate}") BigDecimal feeRate) {
        this.repo = repo;
        this.feeRate = feeRate;
    }

    @Transactional
    public PaymentEntity record(String merchantId, long amountMinor, String currency) {
        return repo.save(new PaymentEntity(UUID.randomUUID().toString(), merchantId, amountMinor, currency, Instant.now()));
    }

    @Transactional(readOnly = true)
    public long owed(String merchantId) {
        var payments = repo.findByMerchantId(merchantId);
        if (payments.isEmpty()) {
            throw new MerchantNotFoundException(merchantId);
        }
        long total = payments.stream().mapToLong(PaymentEntity::getAmountMinor).sum();
        return calculateNet(total, feeRate);
    }

    /**
     * Pure calculation, no I/O — extracted so it can be benchmarked and
     * unit tested without a database. Stays on long minor units and
     * BigDecimal throughout; never double, since binary floating point
     * cannot represent a money value exactly.
     */
    public static long calculateNet(long totalMinor, BigDecimal feeRate) {
        long fee = BigDecimal.valueOf(totalMinor).multiply(feeRate)
                .setScale(0, RoundingMode.DOWN).longValueExact();
        return totalMinor - fee;
    }
}