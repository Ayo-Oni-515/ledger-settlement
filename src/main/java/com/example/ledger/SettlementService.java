package com.example.ledger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

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
        long fee = BigDecimal.valueOf(total).multiply(feeRate)
                .setScale(0, RoundingMode.DOWN).longValueExact();
        return total - fee;
    }
}
