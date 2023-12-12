package com.radynamics.dallipay.cryptoledger;

import com.radynamics.dallipay.iso20022.Payment;
import org.apache.commons.lang3.StringUtils;

public class PaymentComparer {
    private boolean compareSender = true;

    public boolean similar(Payment first, Payment second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null && second != null) {
            return false;
        }
        if (first != null && second == null) {
            return false;
        }

        if (compareSender && !WalletCompare.isSame(first.getSenderWallet(), second.getSenderWallet())) {
            return false;
        }

        if (!WalletCompare.isSame(first.getReceiverWallet(), second.getReceiverWallet())) {
            return false;
        }

        if (!StringUtils.equals(first.getUserCcyCodeOrEmpty(), second.getUserCcyCodeOrEmpty())) {
            return false;
        }

        // Amount is unknown, if no exchange rate is applied (ex. fetched ledger transaction)
        if (!first.isAmountUnknown() && !second.isAmountUnknown()) {
            final Double tolerancePercent = 0.005;
            return Math.abs(first.getAmount() - second.getAmount()) <= first.getAmount() * tolerancePercent;
        } else {
            var sameCcy = first.getAmountTransaction().getCcy().equals(second.getAmountTransaction().getCcy());
            final Double tolerancePercent = 0.02;
            return sameCcy && Math.abs(first.getAmountTransaction().minus(second.getAmountTransaction()).getNumber().doubleValue()) <= first.getAmountTransaction().multiply(tolerancePercent).getNumber().doubleValue();
        }
    }

    public void compareSender(boolean value) {
        this.compareSender = value;
    }

    public boolean compareSender() {
        return this.compareSender;
    }
}
