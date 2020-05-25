package org.bos.core.exception;

public class DupTransactionException extends BosException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
