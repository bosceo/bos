package org.bos.core.exception;

public class VMMemoryOverflowException extends BosException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
