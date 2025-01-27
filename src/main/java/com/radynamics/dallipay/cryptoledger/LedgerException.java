package com.radynamics.dallipay.cryptoledger;

public class LedgerException extends Exception {
    public LedgerException(String errorMessage) {
        this(errorMessage, null);
    }

    public LedgerException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
