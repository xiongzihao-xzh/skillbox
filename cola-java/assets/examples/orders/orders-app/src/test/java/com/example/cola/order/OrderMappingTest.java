package com.example.cola.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(OrderServiceTest.Config.class)
class OrderMappingTest {
  @Autowired OrderConvertor storage;
  @Autowired OrderDtoConvertor client;

  @Test
  void mappedSnapshotsPreserveNestedValuesAcrossTheDomainAndClientBoundaries() {
    var instant = Instant.parse("2026-02-03T04:05:06Z");
    var order =
        Order.create(
                "round-trip",
                List.of(
                    new OrderLine("book", 2, new BigDecimal("12.50")),
                    new OrderLine("pen", 3, new BigDecimal("1.25"))))
            .cancel("duplicate purchase", instant);
    var restored = storage.toDomain(storage.toDataObject(order));
    var dto = client.toDTO(storage.toDataObject(restored));
    assertEquals("round-trip", dto.getId());
    assertEquals(new BigDecimal("28.75"), dto.getTotal());
    assertEquals("CANCELLED", dto.getStatus());
    assertEquals(3, dto.getLines().get(1).getQuantity());
    assertEquals(new BigDecimal("1.25"), dto.getLines().get(1).getUnitPrice());
    assertEquals("duplicate purchase", dto.getCancellation().getReason());
    assertEquals(instant, dto.getCancellation().getCancelledAt());
    assertEquals(order.getLines(), restored.getLines());
  }
}
