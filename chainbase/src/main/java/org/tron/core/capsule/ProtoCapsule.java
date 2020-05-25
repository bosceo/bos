package org.bos.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
