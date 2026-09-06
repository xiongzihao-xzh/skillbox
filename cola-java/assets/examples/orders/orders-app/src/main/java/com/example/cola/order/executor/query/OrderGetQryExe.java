package com.example.cola.order.executor.query;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.OrderGetQry;
import com.example.cola.dto.data.ErrorCode;
import com.example.cola.dto.data.OrderDTO;
import com.example.cola.order.OrderDO;
import com.example.cola.order.OrderDtoConvertor;
import com.example.cola.order.OrderMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderGetQryExe {
  private final OrderMapper mapper;
  private final OrderDtoConvertor convertor;

  public OrderGetQryExe(OrderMapper mapper, OrderDtoConvertor convertor) {
    this.mapper = mapper;
    this.convertor = convertor;
  }

  public SingleResponse<OrderDTO> execute(OrderGetQry query) {
    if (query == null || query.getOrderId() == null || query.getOrderId().isBlank()) {
      throw new BizException(ErrorCode.INVALID_REQUEST.name(), "查询需要订单标识");
    }
    OrderDO data = mapper.selectById(query.getOrderId());
    if (data == null) throw new BizException(ErrorCode.ORDER_NOT_FOUND.name(), "订单不存在");
    return SingleResponse.of(convertor.toDTO(data));
  }
}
