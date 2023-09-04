package com.radynamics.dallipay.ui.paymentTable;

import com.radynamics.dallipay.cryptoledger.transaction.ValidationResult;
import com.radynamics.dallipay.cryptoledger.transaction.ValidationState;
import com.radynamics.dallipay.iso20022.IbanAccount;
import com.radynamics.dallipay.iso20022.Payment;

public class Record {
    public Payment payment;
    public ValidationResult[] validationResults;
    public boolean selected;
    public ValidationState status;

    private Object senderLedger;
    private Object receiverLedger;
    private Double amount;

    public Record(Payment p) {
        payment = p;
        senderLedger = new WalletCellValue(payment.getSenderWallet(), null);
        receiverLedger = new WalletCellValue(payment.getReceiverWallet(), payment.getDestinationTag());
    }

    public Object getSenderLedger() {
        return senderLedger;
    }

    public void setSenderLedger(WalletCellValue value) {
        senderLedger = value;
        payment.setSenderWallet(value.getWallet());
    }

    public void setSenderLedger(String value) {
        senderLedger = value;
    }

    public Object getReceiverLedger() {
        return receiverLedger;
    }

    public void setReceiverLedger(WalletCellValue value) {
        receiverLedger = value;
        payment.setReceiverWallet(value.getWallet());
        payment.setDestinationTag(value.getDestinationTag());
    }

    public void setReceiverLedger(String value) {
        receiverLedger = value;
    }

    public Object getActorAddressOrAccount(Actor actor) {
        Object actorAddressOrAccount = actor.get(payment.getSenderAddress(), payment.getReceiverAddress());
        if (actorAddressOrAccount == null) {
            var actorAccount = actor.get(payment.getSenderAccount(), payment.getReceiverAccount());
            actorAddressOrAccount = actorAccount == null ? IbanAccount.Empty : actorAccount;
        }
        return actorAddressOrAccount;
    }

    public Double getAmount(Actor actor) {
        return actor == Actor.Sender ? payment.getAmount() : amount;
    }

    public void setAmount(Double value) {
        amount = value;
    }

    public Object getCcy() {
        // When exporting 'As received" return received ccy instance to let user know more about issuer.
        return payment.getAmountTransaction().getCcy().getCode().equals(payment.getUserCcyCodeOrEmpty())
                ? payment.getAmountTransaction().getCcy()
                : payment.getUserCcy();
    }

    @Override
    public String toString() {
        return payment.toString();
    }
}
