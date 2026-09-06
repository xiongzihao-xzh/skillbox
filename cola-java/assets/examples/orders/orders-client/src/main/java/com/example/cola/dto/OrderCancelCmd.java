package com.example.cola.dto;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderCancelCmd extends Command {
  @NotBlank private String orderId;
  @NotBlank private String reason;
}
