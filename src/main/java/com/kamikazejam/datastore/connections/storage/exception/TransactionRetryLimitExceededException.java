package com.kamikazejam.datastore.connections.storage.exception;

public class TransactionRetryLimitExceededException extends Exception {
    public TransactionRetryLimitExceededException(String message) {
        super(message);
    }
}
