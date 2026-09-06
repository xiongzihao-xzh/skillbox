package com.example.cola.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDO(
    String id, List<LineDO> lines, BigDecimal total, String status, CancellationDO cancellation) {
  public OrderDO {
    lines = List.copyOf(lines);
  }

  public record LineDO(String sku, int quantity, BigDecimal unitPrice) {}

  public record CancellationDO(String reason, Instant cancelledAt) {}
}
