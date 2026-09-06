package com.example.cola.domain.order;

import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.data.ErrorCode;
import java.math.BigDecimal;

public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {
  public OrderLine {
    if (sku == null
        || sku.isBlank()
        || quantity <= 0
        || unitPrice == null
        || unitPrice.signum() <= 0) {
      throw new BizException(ErrorCode.INVALID_ORDER.name(), "明细需要商品标识、正数量和正单价");
    }
  }
}
