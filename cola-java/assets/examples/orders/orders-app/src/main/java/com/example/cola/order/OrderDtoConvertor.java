package com.example.cola.order;

import com.example.cola.domain.order.Cancellation;
import com.example.cola.domain.order.Order;
import com.example.cola.domain.order.OrderLine;
import com.example.cola.dto.data.CancellationDTO;
import com.example.cola.dto.data.OrderDTO;
import com.example.cola.dto.data.OrderLineDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderDtoConvertor {
  OrderDTO toDTO(Order order);

  OrderDTO toDTO(OrderDO data);

  CancellationDTO toDTO(Cancellation cancellation);

  List<OrderLine> toDomainLines(List<OrderLineDTO> lines);
}
