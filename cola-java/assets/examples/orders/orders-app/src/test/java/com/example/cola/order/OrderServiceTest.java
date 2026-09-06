package com.example.cola.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.cola.api.OrderServiceI;
import com.example.cola.dto.OrderCancelCmd;
import com.example.cola.dto.OrderCreateCmd;
import com.example.cola.dto.OrderGetQry;
import com.example.cola.dto.data.OrderLineDTO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(OrderServiceTest.Config.class)
class OrderServiceTest {
  @Configuration
  @ComponentScan("com.example.cola")
  static class Config {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired OrderServiceI orders;

  @Test
  void concurrentCancellationCreatesExactlyOneRecord() throws Exception {
    OrderCreateCmd command = new OrderCreateCmd();
    command.setLines(List.of(new OrderLineDTO("book", 1, BigDecimal.ONE)));
    String id = orders.createOrder(command).getData().getId();
    try (var pool = Executors.newFixedThreadPool(8)) {
      var tasks =
          IntStream.range(0, 24)
              .mapToObj(
                  i ->
                      (Callable<Boolean>)
                          () ->
                              orders
                                  .cancelOrder(new OrderCancelCmd(id, "changed my mind"))
                                  .getData()
                                  .isCreated())
              .toList();
      int created = 0;
      for (var future : pool.invokeAll(tasks)) if (future.get()) created++;
      assertEquals(1, created);
    }
    var result = orders.getOrder(new OrderGetQry(id)).getData();
    assertEquals("CANCELLED", result.getStatus());
    assertEquals("changed my mind", result.getCancellation().getReason());
    assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result.getCancellation().getCancelledAt());
  }

  @Test
  void createdOrderCanBeQueriedAndReturnedDtosCannotMutateStoredData() {
    OrderCreateCmd command = new OrderCreateCmd();
    command.setLines(List.of(new OrderLineDTO("book", 2, new BigDecimal("12.50"))));
    var created = orders.createOrder(command);
    assertTrue(created.isSuccess());
    var query = new OrderGetQry(created.getData().getId());
    var result = orders.getOrder(query);
    assertEquals(new BigDecimal("25.00"), result.getData().getTotal());
    assertEquals("CREATED", result.getData().getStatus());
    result.getData().getLines().getFirst().setQuantity(100);
    assertEquals(2, orders.getOrder(query).getData().getLines().getFirst().getQuantity());
  }
}
