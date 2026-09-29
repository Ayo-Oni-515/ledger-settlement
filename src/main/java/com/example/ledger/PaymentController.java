package com.example.ledger;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final SettlementService service;
    private final LookupTable lookupTable;

    public PaymentController(SettlementService service, LookupTable lookupTable) {
        this.service = service;
        this.lookupTable = lookupTable;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody RecordPaymentRequest req) {
        var p = service.record(req.merchantId(), req.amountMinor(), req.currency());
        return ResponseEntity.created(URI.create("/payments/" + p.getId()))
                .body(new PaymentResponse(p.getId(), p.getMerchantId(), p.getAmountMinor(), p.getCurrency()));
    }

    @GetMapping("/settlement")
    public SettlementResponse settlement(@RequestParam String merchantId) {
        String currency = lookupTable.table().get(merchantId);
        return new SettlementResponse(merchantId, service.owed(merchantId));
    }
}