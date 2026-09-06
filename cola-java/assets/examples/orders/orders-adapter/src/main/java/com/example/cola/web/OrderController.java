package com.example.cola.web;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;
import com.example.cola.api.OrderServiceI;
import com.example.cola.dto.OrderCancelCmd;
import com.example.cola.dto.OrderCreateCmd;
import com.example.cola.dto.OrderGetQry;
import com.example.cola.dto.data.CancellationDTO;
import com.example.cola.dto.data.ErrorCode;
import com.example.cola.dto.data.OrderDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
  private final OrderServiceI orders;

  public OrderController(OrderServiceI orders) {
    this.orders = orders;
  }

  @PostMapping("/sales/orders")
  public ResponseEntity<SingleResponse<OrderDTO>> create(
      @Valid @RequestBody OrderCreateCmd command) {
    var result = orders.createOrder(command);
    return ResponseEntity.created(URI.create("/sales/orders/" + result.getData().getId()))
        .cacheControl(CacheControl.noStore())
        .body(result);
  }

  @GetMapping("/sales/orders/{orderId}")
  public ResponseEntity<SingleResponse<OrderDTO>> get(@PathVariable String orderId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(orders.getOrder(new OrderGetQry(orderId)));
  }

  @PutMapping("/sales/orders/{orderId}/cancellation")
  public ResponseEntity<SingleResponse<CancellationDTO>> cancel(
      @PathVariable String orderId, @Valid @RequestBody CancellationRequest request) {
    var result = orders.cancelOrder(new OrderCancelCmd(orderId, request.reason())).getData();
    var response =
        result.isCreated()
            ? ResponseEntity.created(URI.create("/sales/orders/" + orderId + "/cancellation"))
            : ResponseEntity.ok();
    return response
        .cacheControl(CacheControl.noStore())
        .body(SingleResponse.of(result.getCancellation()));
  }

  @GetMapping("/sales/orders/{orderId}/cancellation")
  public ResponseEntity<SingleResponse<CancellationDTO>> getCancellation(
      @PathVariable String orderId) {
    var cancellation = orders.getOrder(new OrderGetQry(orderId)).getData().getCancellation();
    if (cancellation == null) {
      throw new BizException(
          ErrorCode.CANCELLATION_NOT_FOUND.name(), "Cancellation does not exist");
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(SingleResponse.of(cancellation));
  }

  public record CancellationRequest(@NotBlank String reason) {}
}
