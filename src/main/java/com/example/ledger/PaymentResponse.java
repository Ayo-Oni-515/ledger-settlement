package com.example.ledger;

public record PaymentResponse(String id, String merchantId, long amountMinor, String currency) {

    public static PaymentResponse from(PaymentEntity p) {
        return new PaymentResponse(p.getId(), p.getMerchantId(), p.getAmountMinor(), p.getCurrency());
    }
}