package com.example.cola.domain.order.gateway;

import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.OrderChange;
import java.util.function.UnaryOperator;

public interface OrderGateway {
  void save(Order order);

  /** Apply a pure domain transformation atomically for this order; failures leave it unchanged. */
  OrderChange update(String orderId, UnaryOperator<Order> change);
}
