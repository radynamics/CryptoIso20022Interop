package com.radynamics.dallipay.ui;

import com.radynamics.dallipay.cryptoledger.Ledger;
import com.radynamics.dallipay.cryptoledger.Wallet;
import com.radynamics.dallipay.cryptoledger.WalletValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WalletFieldInputValidator implements InputControlValidator {
    private final static Logger log = LogManager.getLogger(WalletFieldInputValidator.class);
    private final Ledger ledger;

    public WalletFieldInputValidator(Ledger ledger) {
        if (ledger == null) throw new IllegalArgumentException("Parameter 'ledger' cannot be null");
        this.ledger = ledger;
    }

    @Override
    public boolean isValid(Object value) {
        var text = (String) value;
        return StringUtils.isEmpty(text) || getValidOrNull(text) != null;
    }

    public Wallet getValidOrNull(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        var addressInfo = ledger.createWalletAddressResolver().resolve(text);
        if (addressInfo == null) {
            return null;
        }
        var result = new WalletValidator(ledger).validateFormat(addressInfo.getWallet());
        return result == null ? addressInfo.getWallet() : null;
    }

    @Override
    public String getValidExampleInput() {
        return "\"rn8A9923tgWJGSQEQLoYfU2qNsn9nWSUKk\"";
    }
}
