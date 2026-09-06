package com.example.cola.dto.data;

import com.alibaba.cola.dto.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CancellationResultDTO extends DTO {
  private CancellationDTO cancellation;
  private boolean created;
}
