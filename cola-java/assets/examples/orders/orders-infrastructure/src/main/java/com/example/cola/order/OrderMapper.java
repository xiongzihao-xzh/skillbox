package com.example.cola.order;

import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.data.ErrorCode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

/** In-memory storage; select returns immutable snapshots, not live domain entities. */
@Repository
public class OrderMapper {
  private final ConcurrentMap<String, OrderDO> rows = new ConcurrentHashMap<>();

  public void insert(OrderDO order) {
    if (rows.putIfAbsent(order.id(), order) != null) {
      throw new IllegalStateException("Duplicate order identifier");
    }
  }

  public OrderDO selectById(String id) {
    return rows.get(id);
  }

  public void update(String id, UnaryOperator<OrderDO> change) {
    rows.compute(
        id,
        (key, previous) -> {
          if (previous == null) throw new BizException(ErrorCode.ORDER_NOT_FOUND.name(), "订单不存在");
          return change.apply(previous);
        });
  }
}
