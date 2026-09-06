package com.example.cola.order;

import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.OrderChange;
import com.example.cola.domain.order.gateway.OrderGateway;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

@Component
public class OrderGatewayImpl implements OrderGateway {
  private final OrderMapper mapper;
  private final OrderConvertor convertor;

  public OrderGatewayImpl(OrderMapper mapper, OrderConvertor convertor) {
    this.mapper = mapper;
    this.convertor = convertor;
  }

  @Override
  public void save(Order order) {
    mapper.insert(convertor.toDataObject(order));
  }

  @Override
  public OrderChange update(String orderId, UnaryOperator<Order> change) {
    AtomicReference<OrderChange> result = new AtomicReference<>();
    mapper.update(
        orderId,
        stored -> {
          Order before = convertor.toDomain(stored);
          Order after = change.apply(before);
          if (!orderId.equals(after.getId()))
            throw new IllegalArgumentException("Order identity cannot change");
          result.set(new OrderChange(before, after));
          return convertor.toDataObject(after);
        });
    return result.get();
  }
}
