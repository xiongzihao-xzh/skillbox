package com.example.cola.dto.data;

import com.alibaba.cola.dto.DTO;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class OrderDTO extends DTO {
  private String id;
  private List<OrderLineDTO> lines;
  private BigDecimal total;
  private String status;
  private CancellationDTO cancellation;
}
