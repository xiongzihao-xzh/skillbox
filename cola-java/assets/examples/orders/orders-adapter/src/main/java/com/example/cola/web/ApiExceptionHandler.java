package com.example.cola.web;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BizException;
import com.example.cola.dto.data.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(BizException.class)
  public ResponseEntity<Response> businessFailure(BizException exception) {
    HttpStatus status =
        switch (exception.getErrCode()) {
          case "ORDER_NOT_FOUND", "CANCELLATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
          case "CANCELLATION_CONFLICT" -> HttpStatus.CONFLICT;
          case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;
          default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    return ResponseEntity.status(status)
        .cacheControl(CacheControl.noStore())
        .body(Response.buildFailure(exception.getErrCode(), exception.getMessage()));
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    // Keep framework status and headers such as Allow, while using the agreed error contract.
    String code =
        status.is5xxServerError()
            ? ErrorCode.INTERNAL_ERROR.name()
            : ErrorCode.INVALID_REQUEST.name();
    if (status.is5xxServerError()) LOG.error("HTTP request failed", exception);
    return ResponseEntity.status(status)
        .headers(headers)
        .cacheControl(CacheControl.noStore())
        .body(
            Response.buildFailure(
                code, status.is5xxServerError() ? "Internal server error" : "Invalid request"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Response> unexpectedFailure(Exception exception) {
    LOG.error("Unexpected request failure", exception);
    return ResponseEntity.internalServerError()
        .cacheControl(CacheControl.noStore())
        .body(Response.buildFailure(ErrorCode.INTERNAL_ERROR.name(), "Internal server error"));
  }
}
