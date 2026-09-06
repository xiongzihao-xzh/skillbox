package com.example.cola.order.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;
import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.gateway.OrderGateway;
import com.example.cola.dto.OrderCreateCmd;
import com.example.cola.dto.data.ErrorCode;
import com.example.cola.dto.data.OrderDTO;
import com.example.cola.order.OrderDtoConvertor;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderCreateCmdExe {
  private final OrderGateway gateway;
  private final OrderDtoConvertor convertor;

  public OrderCreateCmdExe(OrderGateway gateway, OrderDtoConvertor convertor) {
    this.gateway = gateway;
    this.convertor = convertor;
  }

  public SingleResponse<OrderDTO> execute(OrderCreateCmd command) {
    if (command == null || command.getLines() == null) {
      throw new BizException(ErrorCode.INVALID_ORDER.name(), "订单需要明细");
    }
    Order order =
        Order.create(UUID.randomUUID().toString(), convertor.toDomainLines(command.getLines()));
    gateway.save(order);
    return SingleResponse.of(convertor.toDTO(order));
  }
}
