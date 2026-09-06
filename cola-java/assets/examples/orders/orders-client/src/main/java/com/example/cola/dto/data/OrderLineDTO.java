package com.example.cola.dto.data;

import com.alibaba.cola.dto.DTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderLineDTO extends DTO {
  @NotBlank private String sku;
  @Positive private int quantity;
  @NotNull @Positive private BigDecimal unitPrice;
}
