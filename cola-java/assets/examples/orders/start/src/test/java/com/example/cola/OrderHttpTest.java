package com.example.cola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OrderHttpTest.HttpFixtures.class)
class OrderHttpTest {
  @Autowired TestRestTemplate http;

  @Test
  void creationReturns201AndALocationThatCanBeRead() {
    var created =
        http.postForEntity(
            "/sales/orders",
            Map.of("lines", List.of(Map.of("sku", "book", "quantity", 2, "unitPrice", "12.50"))),
            JsonNode.class);
    assertEquals(201, created.getStatusCode().value());
    assertNotNull(created.getHeaders().getLocation());
    var found = http.getForEntity(created.getHeaders().getLocation().toString(), JsonNode.class);
    assertEquals(200, found.getStatusCode().value());
    assertEquals("CREATED", found.getBody().path("data").path("status").asText());
    assertEquals(
        0,
        new BigDecimal("25.00")
            .compareTo(found.getBody().path("data").path("total").decimalValue()));
  }

  @Test
  void cancellationHasOneIdentityAndPreservesTheFirstRepresentationOnRetryOrConflict() {
    var created =
        http.postForEntity(
            "/sales/orders",
            Map.of("lines", List.of(Map.of("sku", "book", "quantity", 1, "unitPrice", "12.50"))),
            JsonNode.class);
    String order = created.getHeaders().getLocation().toString();
    String cancellation = order + "/cancellation";
    assertEquals(404, http.getForEntity(cancellation, JsonNode.class).getStatusCode().value());

    var request = new HttpEntity<>(Map.of("reason", "changed my mind"));
    var first = http.exchange(cancellation, HttpMethod.PUT, request, JsonNode.class);
    assertEquals(201, first.getStatusCode().value());
    assertEquals(cancellation, first.getHeaders().getLocation().toString());
    JsonNode representation = first.getBody().path("data");
    assertFalse(representation.has("created"));
    assertFalse(representation.path("cancelledAt").asText().isBlank());
    var repeated = http.exchange(cancellation, HttpMethod.PUT, request, JsonNode.class);
    assertEquals(200, repeated.getStatusCode().value());
    assertEquals(representation, repeated.getBody().path("data"));

    var conflict =
        http.exchange(
            cancellation,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("reason", "different reason")),
            JsonNode.class);
    assertEquals(409, conflict.getStatusCode().value());
    assertEquals("CANCELLATION_CONFLICT", conflict.getBody().path("errCode").asText());
    assertFalse(conflict.getBody().path("success").asBoolean());
    assertEquals(
        representation, http.getForEntity(cancellation, JsonNode.class).getBody().path("data"));
    assertEquals(
        "CANCELLED",
        http.getForEntity(order, JsonNode.class).getBody().path("data").path("status").asText());
  }

  @Test
  void protocolErrorsKeepTheirStatusAndColaErrorBody() {
    var invalid = http.postForEntity("/sales/orders", Map.of("lines", List.of()), JsonNode.class);
    assertEquals(400, invalid.getStatusCode().value());
    assertEquals("INVALID_REQUEST", invalid.getBody().path("errCode").asText());
    assertEquals("no-store", invalid.getHeaders().getCacheControl());
    var missing = http.getForEntity("/sales/orders/does-not-exist", JsonNode.class);
    assertEquals(404, missing.getStatusCode().value());
    assertEquals("ORDER_NOT_FOUND", missing.getBody().path("errCode").asText());
    var method = http.postForEntity("/sales/orders/does-not-exist", Map.of(), JsonNode.class);
    assertEquals(405, method.getStatusCode().value());
    assertEquals("INVALID_REQUEST", method.getBody().path("errCode").asText());
    assertTrue(method.getHeaders().getAllow().contains(HttpMethod.GET));
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    var media =
        http.postForEntity("/sales/orders", new HttpEntity<>("not JSON", headers), JsonNode.class);
    assertEquals(415, media.getStatusCode().value());
    assertEquals("INVALID_REQUEST", media.getBody().path("errCode").asText());
    headers.setContentType(MediaType.APPLICATION_JSON);
    var malformed =
        http.postForEntity("/sales/orders", new HttpEntity<>("{", headers), JsonNode.class);
    assertEquals(400, malformed.getStatusCode().value());
    assertEquals("INVALID_REQUEST", malformed.getBody().path("errCode").asText());
  }

  @Test
  void invalidCancellationOrUnknownFieldsDoNotChangeTheOrder() {
    var created =
        http.postForEntity(
            "/sales/orders",
            Map.of("lines", List.of(Map.of("sku", "book", "quantity", 1, "unitPrice", "12.50"))),
            JsonNode.class);
    String order = created.getHeaders().getLocation().toString();
    for (var body :
        List.of(Map.of("reason", " "), Map.of("reason", "valid", "orderId", "another-order"))) {
      var invalid =
          http.exchange(
              order + "/cancellation", HttpMethod.PUT, new HttpEntity<>(body), JsonNode.class);
      assertEquals(400, invalid.getStatusCode().value());
      assertEquals("INVALID_REQUEST", invalid.getBody().path("errCode").asText());
    }
    assertEquals(
        "CREATED",
        http.getForEntity(order, JsonNode.class).getBody().path("data").path("status").asText());
  }

  @Test
  void emptyResponsesAndUnexpectedErrorsRespectTheWireContract() {
    var empty = http.getForEntity("/test/no-content", String.class);
    assertEquals(204, empty.getStatusCode().value());
    assertNull(empty.getBody());
    var created =
        http.postForEntity(
            "/sales/orders",
            Map.of("lines", List.of(Map.of("sku", "book", "quantity", 1, "unitPrice", "12.50"))),
            JsonNode.class);
    String order = created.getHeaders().getLocation().toString();
    var head = http.exchange(order, HttpMethod.HEAD, HttpEntity.EMPTY, String.class);
    assertEquals(200, head.getStatusCode().value());
    assertNull(head.getBody());
    var failure = http.getForEntity("/test/failure", JsonNode.class);
    assertEquals(500, failure.getStatusCode().value());
    assertEquals("INTERNAL_ERROR", failure.getBody().path("errCode").asText());
    assertEquals("Internal server error", failure.getBody().path("errMessage").asText());
    assertFalse(failure.getBody().toString().contains("private-detail"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class HttpFixtures {
    @Bean
    WireContractController wireContractController() {
      return new WireContractController();
    }
  }

  // Test-only endpoints exercise response contracts without adding artificial order operations.
  @RestController
  static class WireContractController {
    @GetMapping("/test/no-content")
    ResponseEntity<Void> noContent() {
      return ResponseEntity.noContent().build();
    }

    @GetMapping("/test/failure")
    ResponseEntity<Void> fail() {
      throw new IllegalStateException("private-detail");
    }
  }
}
