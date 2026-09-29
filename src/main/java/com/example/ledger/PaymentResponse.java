package com.example.ledger;
public record PaymentResponse(String id, String merchantId, long amountMinor, String currency) {

}