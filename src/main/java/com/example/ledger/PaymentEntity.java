package com.example.ledger;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    // Mutable class, not a record: JPA needs a no-arg constructor and
    // non-final fields so Hibernate can instantiate and populate it.
    @Id
    private String id;
    private String merchantId;
    private long amountMinor;
    private String currency;
    private Instant recordedAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(String id, String merchantId, long amountMinor, String currency, Instant recordedAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.recordedAt = recordedAt;
    }

    public String getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
