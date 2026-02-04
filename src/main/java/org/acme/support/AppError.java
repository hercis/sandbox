package org.acme.support;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public sealed interface AppError
    permits AppError.InternalAppError,
        AppError.ExternalSystemError,
        AppError.ValidationError,
        AppError.NotFoundError {

  String message();

  @JsonInclude(Include.NON_NULL)
  public record ErrorResponse(String message, List<ErrorInfo> errors, OffsetDateTime timestamp) {

    public ErrorResponse {
      Objects.requireNonNull(message, "message cannot be null");
    }

    public static ErrorResponse fromMessage(String message) {
      return ErrorResponse.of(message, null);
    }

    public static ErrorResponse fromSingleError(ErrorInfo errorInfo) {
      return ErrorResponse.of(errorInfo.message(), List.of(errorInfo));
    }

    public static ErrorResponse fromErrors(String message, List<ErrorInfo> errors) {
      return ErrorResponse.of(message, errors);
    }

    public static ErrorResponse of(String message, List<ErrorInfo> errors) {
      return new ErrorResponse(message, errors, OffsetDateTime.now(ZoneOffset.UTC));
    }
  }

  @JsonInclude(Include.NON_NULL)
  public record ErrorInfo(String field, String message, String code) {

    public static ErrorInfo of(String field, String message) {
      return ErrorInfo.of(field, message, null);
    }

    public static ErrorInfo of(String field, String message, String code) {
      return new ErrorInfo(field, message, code);
    }
  }

  public record InternalAppError(ErrorInfo errorInfo, Throwable cause) implements AppError {
    private static final String INTERNAL_PATTERN = "Internal server error: %s";
    private static final String NO_DETAILS_FOUND = "<no details found>";
    private static final String INTERNAL_CODE = "001";

    public InternalAppError {
      Objects.requireNonNull(errorInfo, "errorInfo cannot be null");
    }

    @Override
    public String message() {
      return errorInfo.message();
    }

    public static InternalAppError fromCause(Throwable cause) {
      String message =
          INTERNAL_PATTERN.formatted(
              Optional.ofNullable(cause.getMessage()).orElse(NO_DETAILS_FOUND));
      ErrorInfo errorInfo = new ErrorInfo(null, message, INTERNAL_CODE);
      return new InternalAppError(errorInfo, cause);
    }

    public static InternalAppError fromCause(String code, Throwable cause) {
      String message =
          INTERNAL_PATTERN.formatted(
              Optional.ofNullable(cause.getMessage()).orElse(NO_DETAILS_FOUND));
      ErrorInfo errorInfo = new ErrorInfo(null, message, code);
      return new InternalAppError(errorInfo, cause);
    }

    public static InternalAppError fromMessage(String message) {
      ErrorInfo errorInfo = new ErrorInfo(null, message, INTERNAL_CODE);
      return new InternalAppError(errorInfo, null);
    }

    public static InternalAppError fromMessage(String code, String message) {
      ErrorInfo errorInfo = new ErrorInfo(null, message, code);
      return new InternalAppError(errorInfo, null);
    }
  }

  public record ExternalSystemError(ErrorInfo errorInfo, Throwable cause) implements AppError {
    private static final String EXTERNAL_PATTERN = "External service error: %s";
    private static final String NO_DETAILS_FOUND = "<no details found>";
    private static final String EXTERNAL_CODE = "001";

    public ExternalSystemError {
      Objects.requireNonNull(errorInfo, "errorInfo cannot be null");
    }

    @Override
    public String message() {
      return errorInfo.message();
    }

    public static ExternalSystemError fromCause(Throwable cause) {
      String message =
          EXTERNAL_PATTERN.formatted(
              Optional.ofNullable(cause.getMessage()).orElse(NO_DETAILS_FOUND));
      ErrorInfo errorInfo = new ErrorInfo(null, message, EXTERNAL_CODE);
      return new ExternalSystemError(errorInfo, cause);
    }

    public static ExternalSystemError fromCause(String code, Throwable cause) {
      String message =
          EXTERNAL_PATTERN.formatted(
              Optional.ofNullable(cause.getMessage()).orElse(NO_DETAILS_FOUND));
      ErrorInfo errorInfo = new ErrorInfo(null, message, code);
      return new ExternalSystemError(errorInfo, cause);
    }
  }

  public record ValidationError(ErrorInfo errorInfo) implements AppError {
    @Override
    public String message() {
      return errorInfo.message();
    }

    public static ValidationError fromMessage(String code, String messagePattern, Object... args) {
      return ofTemplate(code, messagePattern).build(args);
    }

    public static ValidationError fromMessage(
        String code, String field, String messagePattern, Object... args) {
      return ofTemplate(code, field, messagePattern).build(args);
    }

    public static ValidationError.Builder ofTemplate(String code, String messagePattern) {
      return new ValidationError.Builder(messagePattern).code(code);
    }

    public static ValidationError.Builder ofTemplate(
        String code, String field, String messagePattern) {
      return new ValidationError.Builder(messagePattern).code(code).field(field);
    }

    public static class Builder {
      private String field;
      private String messagePattern;
      private String code;

      public Builder(String messagePatten) {
        this.messagePattern = messagePatten;
      }

      public Builder field(String field) {
        this.field = field;
        return this;
      }

      public Builder code(String code) {
        this.code = code;
        return this;
      }

      public ValidationError build(Object... args) {
        String message = String.format(messagePattern, args);
        ErrorInfo errorInfo = new ErrorInfo(field, message, code);
        return new ValidationError(errorInfo);
      }
    }
  }

  public record NotFoundError(ErrorInfo errorInfo) implements AppError {
    private static final String NFE_PATTERN = "{0} with ID ''{1}'' was not found";

    @Override
    public String message() {
      return errorInfo.message();
    }

    public static NotFoundError fromId(String code, String entity, Object id) {
      return ofTemplate(code, entity).build(id);
    }

    public static NotFoundError.Builder ofTemplate(String code, String entity) {
      return new NotFoundError.Builder().code(code).entity(entity);
    }

    public static class Builder {
      private String code;
      private String entity = "Entity";

      public Builder entity(String entity) {
        this.entity = entity;
        return this;
      }

      public Builder code(String code) {
        this.code = code;
        return this;
      }

      public NotFoundError build(Object id) {
        String message =
            MessageFormat.format(NFE_PATTERN, entity, id == null ? "<null>" : id.toString());
        ErrorInfo errorInfo = new ErrorInfo(null, message, code);
        return new NotFoundError(errorInfo);
      }
    }
  }
}
