package com.example.cola.order;

import com.alibaba.cola.dto.SingleResponse;
import com.example.cola.api.OrderServiceI;
import com.example.cola.dto.OrderCancelCmd;
import com.example.cola.dto.OrderCreateCmd;
import com.example.cola.dto.OrderGetQry;
import com.example.cola.dto.data.CancellationResultDTO;
import com.example.cola.dto.data.OrderDTO;
import com.example.cola.order.executor.OrderCancelCmdExe;
import com.example.cola.order.executor.OrderCreateCmdExe;
import com.example.cola.order.executor.query.OrderGetQryExe;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderServiceI {
  private final OrderCreateCmdExe create;
  private final OrderGetQryExe get;
  private final OrderCancelCmdExe cancel;

  public OrderServiceImpl(OrderCreateCmdExe create, OrderGetQryExe get, OrderCancelCmdExe cancel) {
    this.create = create;
    this.get = get;
    this.cancel = cancel;
  }

  @Override
  public SingleResponse<OrderDTO> createOrder(OrderCreateCmd command) {
    return create.execute(command);
  }

  @Override
  public SingleResponse<OrderDTO> getOrder(OrderGetQry query) {
    return get.execute(query);
  }

  @Override
  public SingleResponse<CancellationResultDTO> cancelOrder(OrderCancelCmd command) {
    return cancel.execute(command);
  }
}
