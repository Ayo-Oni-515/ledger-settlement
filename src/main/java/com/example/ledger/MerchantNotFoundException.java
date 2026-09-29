package com.example.ledger;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String id) {
        super("Unknown merchant " + id);
    }
}
