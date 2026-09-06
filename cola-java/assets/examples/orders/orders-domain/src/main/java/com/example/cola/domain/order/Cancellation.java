package com.example.cola.domain.order;

import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.data.ErrorCode;
import java.time.Instant;

public record Cancellation(String reason, Instant cancelledAt) {
  public Cancellation {
    if (reason == null || reason.isBlank() || cancelledAt == null) {
      throw new BizException(ErrorCode.INVALID_CANCELLATION.name(), "取消需要原因和时间");
    }
  }
}
