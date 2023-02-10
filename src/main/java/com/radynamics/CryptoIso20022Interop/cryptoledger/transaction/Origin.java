package com.radynamics.CryptoIso20022Interop.cryptoledger.transaction;

public enum Origin {
    Manual,
    Pain001,
    Ledger,
    ;

    public boolean isDeletable() {
        return this == Manual;
    }
}
