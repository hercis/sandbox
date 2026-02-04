package org.acme.support;

@FunctionalInterface
public interface CheckedSupplier<T> {
  T get() throws Exception;
}
