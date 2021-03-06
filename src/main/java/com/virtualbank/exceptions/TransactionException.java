package com.virtualbank.exceptions;

public class TransactionException extends Exception {

  private static final long serialVersionUID = 1L;

  public TransactionException() {
    super();
  }

  public TransactionException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransactionException(String message) {
    super(message);
  }

  public TransactionException(Throwable cause) {
    super(cause);
  }


}
