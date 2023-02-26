package com.radynamics.dallipay.cryptoledger;

import com.radynamics.dallipay.cryptoledger.transaction.ValidationResult;
import com.radynamics.dallipay.cryptoledger.transaction.ValidationState;

import java.util.ArrayList;

public class WalletValidator {
    private Ledger ledger;

    public WalletValidator(Ledger ledger) {
        this.ledger = ledger;
    }

    public ValidationResult[] validate(Wallet wallet, String senderOrReceiver) {
        var list = new ArrayList<ValidationResult>();

        var formatResult = validateFormat(wallet, senderOrReceiver);
        if (formatResult != null) {
            list.add(formatResult);
            return list.toArray(new ValidationResult[0]);
        }

        if (!ledger.exists(wallet)) {
            list.add(new ValidationResult(ValidationState.Error, String.format("%s wallet doesn't exist.", senderOrReceiver)));
        }

        return list.toArray(new ValidationResult[0]);
    }

    public static boolean isValidFormat(Ledger ledger, Wallet wallet) {
        var v = new WalletValidator(ledger);
        return v.validateFormat(wallet) == null;
    }

    public ValidationResult validateFormat(Wallet wallet) {
        return validateFormat(wallet, null);
    }

    private ValidationResult validateFormat(Wallet wallet, String senderOrReceiver) {
        var prefix = senderOrReceiver == null ? "" : String.format("%s ", senderOrReceiver);
        return wallet != null && ledger.isValidPublicKey(wallet.getPublicKey())
                ? null
                : new ValidationResult(ValidationState.Error, String.format("%sCryptocurrency wallet isn't a valid address.", prefix));
    }

    public ValidationResult validateSecret(Wallet wallet) {
        return ledger.isSecretValid(wallet)
                ? null
                : new ValidationResult(ValidationState.Error, String.format("Wallet secret (private Key) for %s is not valid or doesn't match it's public key.", wallet.getPublicKey()));
    }
}