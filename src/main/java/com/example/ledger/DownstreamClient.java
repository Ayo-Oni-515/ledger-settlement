package com.example.ledger;

import java.util.Map;

public final class DownstreamClient {

    public static final DownstreamClient INSTANCE = new DownstreamClient();

    private DownstreamClient() {
    }

    public Map<String, String> fetchTable() {
        try {
            Thread.sleep(2000);   // simulates a blocking network call
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Map.of("MR-4471", "USD", "MR-1000", "EUR");
    }
}