package com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Ledger;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.TransmissionState;
import com.radynamics.CryptoIso20022Interop.exchange.Currency;
import com.radynamics.CryptoIso20022Interop.exchange.Money;
import com.radynamics.CryptoIso20022Interop.iso20022.creditorreference.StructuredReference;
import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class Transaction implements com.radynamics.CryptoIso20022Interop.cryptoledger.Transaction {
    private String id;
    private Ledger ledger;
    private Double amt;
    private Currency ccy;
    private ZonedDateTime booked;
    private com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.Wallet senderWallet;
    private com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.Wallet receiverWallet;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<StructuredReference> references = new ArrayList<>();
    private String invoiceId;
    private TransmissionState transmission = TransmissionState.Pending;
    private Throwable transmissionError;
    private Money fee;

    public Transaction(Ledger ledger, Double amt, Currency ccy) {
        if (amt == null) throw new IllegalArgumentException("Parameter 'amt' cannot be null");
        if (ccy == null) throw new IllegalArgumentException("Parameter 'ccy' cannot be null");
        this.ledger = ledger;
        this.amt = amt;
        this.ccy = ccy;
        this.fee = Money.zero(new Currency(ledger.getNativeCcySymbol()));
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    @Override
    public void setAmount(Double value) {
        amt = value;
    }

    @Override
    public Double getAmount() {
        return amt;
    }

    @Override
    public Currency getCcy() {
        return ccy;
    }

    @Override
    public ZonedDateTime getBooked() {
        return booked;
    }

    @Override
    public void setBooked(ZonedDateTime value) {
        this.booked = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        this.id = value;
    }

    @Override
    public Wallet getSenderWallet() {
        return senderWallet;
    }

    @Override
    public void setSenderWallet(Wallet wallet) {
        this.senderWallet = WalletConverter.from(wallet);
    }

    @Override
    public Wallet getReceiverWallet() {
        return receiverWallet;
    }

    @Override
    public void setReceiverWallet(Wallet wallet) {
        setReceiver(WalletConverter.from(wallet));
    }

    @Override
    public void addStructuredReference(StructuredReference value) {
        references.add(value);
    }

    @Override
    public StructuredReference[] getStructuredReferences() {
        return references.toArray(new StructuredReference[0]);
    }

    @Override
    public void removeStructuredReferences(int index) {
        references.remove(index);
    }

    @Override
    public String[] getMessages() {
        return messages.toArray(new String[0]);
    }

    @Override
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    @Override
    public Ledger getLedger() {
        return ledger;
    }

    @Override
    public TransmissionState getTransmission() {
        return transmission;
    }

    @Override
    public Throwable getTransmissionError() {
        return transmissionError;
    }

    @Override
    public Money getFee() {
        return fee;
    }

    @Override
    public void setFee(Money value) {
        if (!value.getCcy().getCcy().equals(ledger.getNativeCcySymbol())) {
            throw new IllegalArgumentException(String.format("Currency of fee must be %s", ledger.getNativeCcySymbol()));
        }
        fee = value;
    }

    public void setSender(com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.Wallet sender) {
        this.senderWallet = sender;
    }

    public void setReceiver(com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.Wallet receiver) {
        this.receiverWallet = receiver;
    }

    public void refreshTransmission() {
        refreshTransmission(null);
    }

    public void refreshTransmission(Throwable t) {
        this.transmissionError = t;
        this.transmission = StringUtils.isAllEmpty(getId()) ? TransmissionState.Error : TransmissionState.Success;
    }
}
