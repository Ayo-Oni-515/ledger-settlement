package com.example.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @LocalServerPort
    int port;

    @Test
    void settlementIsPaymentLessFee() {
        var client = RestClient.create("http://localhost:" + port);

        client.post().uri("/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new RecordPaymentRequest("MR-4471", 128450, "GBP"))
                .retrieve().toBodilessEntity();

        var s = client.get().uri("/payments/settlement?merchantId=MR-4471")
                .retrieve().body(SettlementResponse.class);

        assertThat(s.owedMinor()).isEqualTo(124469L);
    }
}
