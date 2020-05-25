package org.bos.core.exception;

public class BosException extends Exception {

  public BosException() {
    super();
  }

  public BosException(String message) {
    super(message);
  }

  public BosException(String message, Throwable cause) {
    super(message, cause);
  }

}
