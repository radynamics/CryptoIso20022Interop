package com.radynamics.CryptoIso20022Interop.iso20022.pain001;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Ledger;
import com.radynamics.CryptoIso20022Interop.cryptoledger.NetworkInfo;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationResult;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;

public class NullHistoryValidator implements WalletHistoryValidator {
    @Override
    public ValidationResult[] validate(Payment p) {
        return new ValidationResult[0];
    }

    @Override
    public void loadHistory(Ledger ledger, Wallet wallet) {
        // do nothing
    }

    @Override
    public void clearCache() {
        // do nothing
    }

    @Override
    public void setNetwork(NetworkInfo networkInfo) {
        // do nothing
    }
}
