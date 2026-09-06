package com.example.cola.api;

import com.alibaba.cola.dto.SingleResponse;
import com.example.cola.dto.OrderCancelCmd;
import com.example.cola.dto.OrderCreateCmd;
import com.example.cola.dto.OrderGetQry;
import com.example.cola.dto.data.CancellationResultDTO;
import com.example.cola.dto.data.OrderDTO;

public interface OrderServiceI {
  SingleResponse<OrderDTO> createOrder(OrderCreateCmd command);

  SingleResponse<OrderDTO> getOrder(OrderGetQry query);

  SingleResponse<CancellationResultDTO> cancelOrder(OrderCancelCmd command);
}
