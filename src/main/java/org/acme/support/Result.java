package org.acme.order.support;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<V, E> permits Result.Success, Result.Failure {

  boolean isSuccess();

  boolean isFailure();

  V getOrThrow() throws IllegalStateException;

  E getErrorOrThrow() throws IllegalStateException;

  <U> U fold(
      Function<? super E, ? extends U> onFailure, Function<? super V, ? extends U> onSuccess);

  Result<V, E> peek(Consumer<? super E> onFailure, Consumer<? super V> onSuccess);

  Result<V, E> peekSuccess(Consumer<? super V> c);

  Result<V, E> peekFailure(Consumer<? super E> c);

  Result<V, E> recover(Function<E, Result<V, E>> recovery);

  Result<V, E> recoverWith(Function<E, V> recovery);

  default <U> Result<U, E> map(Function<? super V, ? extends U> m) {
    return fold(Result::failure, v -> Result.success(m.apply(v)));
  }

  default <U> Result<U, E> flatMap(Function<? super V, Result<U, E>> m) {
    return fold(Result::failure, m);
  }

  default <U> Result<V, U> mapError(Function<? super E, ? extends U> m) {
    return fold(e -> Result.failure(m.apply(e)), Result::success);
  }

  public static <V, E> Result<V, E> success(V v) {
    return new Success<>(v);
  }

  public static <V, E> Result<V, E> failure(E e) {
    return new Failure<>(e);
  }

  public static <U> Result<U, Throwable> tryOf(CheckedSupplier<U> s) {
    try {
      return Result.success(s.get());
    } catch (Throwable t) {
      return Result.failure(t);
    }
  }

  public static <U, T> Result<U, T> tryOf(CheckedSupplier<U> s, Function<Throwable, T> m) {
    try {
      return Result.success(s.get());
    } catch (Throwable t) {
      return Result.failure(m.apply(t));
    }
  }

  public record Success<V, E>(V value) implements Result<V, E> {
    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public V getOrThrow() {
      return value;
    }

    @Override
    public E getErrorOrThrow() {
      throw new IllegalStateException("No error in Success");
    }

    @Override
    public <U> U fold(
        Function<? super E, ? extends U> onFailure, Function<? super V, ? extends U> onSuccess) {
      Objects.requireNonNull(onSuccess, "onSuccess function must not be null");
      return onSuccess.apply(value);
    }

    @Override
    public Result<V, E> peek(Consumer<? super E> onFailure, Consumer<? super V> onSuccess) {
      try {
        if (onSuccess != null) onSuccess.accept(value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public Result<V, E> peekSuccess(Consumer<? super V> c) {
      if (c == null) return this;
      return peek(null, c);
    }

    @Override
    public Result<V, E> peekFailure(Consumer<? super E> c) {
      return this;
    }

    @Override
    public Result<V, E> recover(Function<E, Result<V, E>> f) {
      return this;
    }

    @Override
    public Result<V, E> recoverWith(Function<E, V> f) {
      return this;
    }
  }

  public record Failure<V, E>(E error) implements Result<V, E> {
    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public V getOrThrow() {
      throw new IllegalStateException("No value in Failure");
    }

    @Override
    public E getErrorOrThrow() {
      return error;
    }

    @Override
    public <U> U fold(
        Function<? super E, ? extends U> onFailure, Function<? super V, ? extends U> onSuccess) {
      Objects.requireNonNull(onFailure, "onFailure function must not be null");
      return onFailure.apply(error);
    }

    @Override
    public Result<V, E> peek(Consumer<? super E> onFailure, Consumer<? super V> onSuccess) {
      try {
        if (onFailure != null) onFailure.accept(error);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public Result<V, E> peekFailure(Consumer<? super E> c) {
      if (c == null) return this;
      return peek(c, null);
    }

    @Override
    public Result<V, E> peekSuccess(Consumer<? super V> c) {
      return this;
    }

    @Override
    public Result<V, E> recover(Function<E, Result<V, E>> f) {
      Objects.requireNonNull(f, "recovery function must not be null");
      return f.apply(error);
    }

    @Override
    public Result<V, E> recoverWith(Function<E, V> f) {
      Objects.requireNonNull(f, "recovery function must not be null");
      return Result.success(f.apply(error));
    }
  }
}
