package org.acme.support;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.acme.support.AppError.ErrorInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record AppResponse(
    String message, Object entity, List<ErrorInfo> errors, OffsetDateTime timestamp) {

  public AppResponse {
    Objects.requireNonNull(message, "message cannot be null");
  }

  public static AppResponse fromMessage(String message) {
    return AppResponse.of(message, null, null);
  }

  public static AppResponse fromMessage(String message, Object entity) {
    return AppResponse.of(message, entity, null);
  }

  public static AppResponse fromError(AppError error) {
    return AppResponse.fromErrors(error.message(), List.of(error));
  }

  public static AppResponse fromError(String message, AppError error) {
    return AppResponse.fromErrors(message, List.of(error));
  }

  public static AppResponse fromErrors(String message, List<? extends AppError> errors) {
    return AppResponse.of(message, null, errors.stream().map(AppError::details).toList());
  }

  private static AppResponse of(String message, Object entity, List<ErrorInfo> errors) {
    return new AppResponse(message, entity, errors, OffsetDateTime.now(ZoneOffset.UTC));
  }
}
