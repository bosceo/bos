package org.bos.core.exception;

public class BosRuntimeException extends RuntimeException {

  public BosRuntimeException() {
    super();
  }

  public BosRuntimeException(String message) {
    super(message);
  }

  public BosRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public BosRuntimeException(Throwable cause) {
    super(cause);
  }

  protected BosRuntimeException(String message, Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
