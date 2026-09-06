package com.example.cola.dto;

import com.alibaba.cola.dto.Command;
import com.example.cola.dto.data.OrderLineDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class OrderCreateCmd extends Command {
  @NotEmpty @Valid private List<OrderLineDTO> lines;
}
