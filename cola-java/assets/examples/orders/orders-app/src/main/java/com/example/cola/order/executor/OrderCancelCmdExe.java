package com.example.cola.order.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;
import com.example.cola.domain.order.OrderChange;
import com.example.cola.domain.order.gateway.OrderGateway;
import com.example.cola.dto.OrderCancelCmd;
import com.example.cola.dto.data.CancellationResultDTO;
import com.example.cola.dto.data.ErrorCode;
import com.example.cola.order.OrderDtoConvertor;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelCmdExe {
  private final OrderGateway gateway;
  private final OrderDtoConvertor convertor;
  private final Clock clock;

  public OrderCancelCmdExe(OrderGateway gateway, OrderDtoConvertor convertor, Clock clock) {
    this.gateway = gateway;
    this.convertor = convertor;
    this.clock = clock;
  }

  public SingleResponse<CancellationResultDTO> execute(OrderCancelCmd command) {
    if (command == null || command.getOrderId() == null || command.getOrderId().isBlank()) {
      throw new BizException(ErrorCode.INVALID_REQUEST.name(), "取消需要订单标识");
    }
    OrderChange change =
        gateway.update(
            command.getOrderId(), order -> order.cancel(command.getReason(), clock.instant()));
    return SingleResponse.of(
        new CancellationResultDTO(
            convertor.toDTO(change.after().getCancellation()),
            change.before().getCancellation() == null));
  }
}
