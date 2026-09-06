package com.example.cola.order;

import com.example.cola.domain.order.Cancellation;
import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.OrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderConvertor {
  OrderDO toDataObject(Order order);

  default Order toDomain(OrderDO data) {
    var cancellation = data.cancellation();
    return Order.restore(
        data.id(),
        data.lines().stream()
            .map(line -> new OrderLine(line.sku(), line.quantity(), line.unitPrice()))
            .toList(),
        cancellation == null
            ? null
            : new Cancellation(cancellation.reason(), cancellation.cancelledAt()));
  }
}
