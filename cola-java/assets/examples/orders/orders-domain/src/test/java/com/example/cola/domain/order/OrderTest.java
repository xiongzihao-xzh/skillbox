package com.example.cola.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.cola.exception.BizException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {
  @Test
  void cancellationRetainsTheFirstRecordAndRejectsAChangedReason() {
    Order original = Order.create("order-1", List.of(new OrderLine("book", 1, BigDecimal.ONE)));
    Instant first = Instant.parse("2026-01-01T00:00:00Z");
    Order cancelled = original.cancel("changed my mind", first);
    Order replay = cancelled.cancel("changed my mind", first.plusSeconds(20));
    assertEquals(OrderStatus.CREATED, original.getStatus());
    assertEquals(OrderStatus.CANCELLED, replay.getStatus());
    assertEquals(first, replay.getCancellation().cancelledAt());
    assertEquals(cancelled.getCancellation(), replay.getCancellation());
    assertEquals(
        "CANCELLATION_CONFLICT",
        assertThrows(BizException.class, () -> cancelled.cancel("another reason", first))
            .getErrCode());
    assertThrows(BizException.class, () -> original.cancel(" ", first));
  }

  @Test
  void invalidLinesCannotEnterAnOrderEvenWithoutHttpValidation() {
    assertThrows(BizException.class, () -> Order.create("order-1", List.of()));
    assertThrows(BizException.class, () -> new OrderLine("book", 0, BigDecimal.ONE));
    assertThrows(BizException.class, () -> new OrderLine("book", 1, BigDecimal.ZERO));
    assertThrows(BizException.class, () -> new OrderLine(" ", 1, BigDecimal.ONE));
  }

  @Test
  void creationCalculatesTheTotalFromLines() {
    Order order =
        Order.create(
            "order-1",
            List.of(
                new OrderLine("book", 2, new BigDecimal("12.50")),
                new OrderLine("pen", 1, new BigDecimal("5.25"))));
    assertEquals(new BigDecimal("30.25"), order.getTotal());
    assertEquals(OrderStatus.CREATED, order.getStatus());
  }
}
