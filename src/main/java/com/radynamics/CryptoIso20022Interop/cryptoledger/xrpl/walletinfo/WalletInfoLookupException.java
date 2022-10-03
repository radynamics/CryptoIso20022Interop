package com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.walletinfo;

public class WalletInfoLookupException extends Exception {
    public WalletInfoLookupException(String errorMessage) {
        super(errorMessage);
    }

    public WalletInfoLookupException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
