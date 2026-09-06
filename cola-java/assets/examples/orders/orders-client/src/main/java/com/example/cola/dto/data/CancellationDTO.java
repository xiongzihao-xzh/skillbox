package com.example.cola.dto.data;

import com.alibaba.cola.dto.DTO;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CancellationDTO extends DTO {
  private String reason;
  private Instant cancelledAt;
}
