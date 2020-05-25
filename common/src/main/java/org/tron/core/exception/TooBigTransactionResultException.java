package org.bos.core.exception;

public class TooBigTransactionResultException extends BosException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
