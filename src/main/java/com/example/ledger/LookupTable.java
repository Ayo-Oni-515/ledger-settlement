package com.example.ledger;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class LookupTable {

    private final Map<String, String> table;

    public LookupTable() {
        this.table = DownstreamClient.INSTANCE.fetchTable();
    }

    public Map<String, String> table() {
        return table;
    }
}