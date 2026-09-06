package com.example.cola.domain.order;

import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.data.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Order {
  private final String id;
  private final List<OrderLine> lines;
  private final Cancellation cancellation;

  private Order(String id, List<OrderLine> lines, Cancellation cancellation) {
    if (id == null
        || id.isBlank()
        || lines == null
        || lines.isEmpty()
        || lines.stream().anyMatch(Objects::isNull)) {
      throw new BizException(ErrorCode.INVALID_ORDER.name(), "订单需要标识和至少一条有效明细");
    }
    this.id = id;
    this.lines = List.copyOf(lines);
    this.cancellation = cancellation;
  }

  public static Order create(String id, List<OrderLine> lines) {
    return new Order(id, lines, null);
  }

  public static Order restore(String id, List<OrderLine> lines, Cancellation cancellation) {
    return new Order(id, lines, cancellation);
  }

  public String getId() {
    return id;
  }

  public List<OrderLine> getLines() {
    return lines;
  }

  public OrderStatus getStatus() {
    return cancellation == null ? OrderStatus.CREATED : OrderStatus.CANCELLED;
  }

  public Cancellation getCancellation() {
    return cancellation;
  }

  public Order cancel(String reason, Instant now) {
    Cancellation requested = new Cancellation(reason, now);
    if (cancellation != null) {
      if (!cancellation.reason().equals(reason)) {
        throw new BizException(ErrorCode.CANCELLATION_CONFLICT.name(), "已记录的取消原因不可更改");
      }
      return this;
    }
    return new Order(id, lines, requested);
  }

  public BigDecimal getTotal() {
    return lines.stream()
        .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
