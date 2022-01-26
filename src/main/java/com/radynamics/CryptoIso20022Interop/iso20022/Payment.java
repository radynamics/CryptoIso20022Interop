package com.radynamics.CryptoIso20022Interop.iso20022;

import com.radynamics.CryptoIso20022Interop.MoneyFormatter;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Ledger;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Transaction;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.TransmissionState;
import com.radynamics.CryptoIso20022Interop.exchange.CurrencyConverter;
import com.radynamics.CryptoIso20022Interop.exchange.CurrencyPair;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRate;
import com.radynamics.CryptoIso20022Interop.iso20022.creditorreference.StructuredReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private Transaction cryptoTrx;
    private Account senderAccount;
    private Account receiverAccount;
    private Address senderAddress;
    private Address receiverAddress;
    private Double amount = UnknownAmount;
    private String ccy = UnknownCCy;
    private ExchangeRate exchangeRate;

    private static final Double UnknownAmount = Double.valueOf(0);
    private static final String UnknownCCy = "";
    private boolean amountDefined;

    public Payment(Transaction cryptoTrx) {
        this.cryptoTrx = cryptoTrx;
    }

    public Address getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(Address address) {
        this.receiverAddress = address;
    }

    public Address getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(Address senderAddress) {
        this.senderAddress = senderAddress;
    }

    public Account getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(Account account) {
        receiverAccount = account;
    }

    public Account getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(Account account) {
        senderAccount = account;
    }

    public LocalDateTime getBooked() {
        return cryptoTrx.getBooked();
    }

    public TransmissionState getTransmission() {
        return cryptoTrx.getTransmission();
    }

    public Wallet getReceiverWallet() {
        return cryptoTrx.getReceiverWallet();
    }

    public Wallet getSenderWallet() {
        return cryptoTrx.getSenderWallet();
    }

    public Double getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal sourceAmt, String sourceCcy) {
        this.amount = sourceAmt.doubleValue();
        this.ccy = sourceCcy;
        if (!UnknownAmount.equals(sourceAmt.doubleValue())) {
            this.amountDefined = true;
        }

        refreshTransactionAmount();
    }

    private void refreshTransactionAmount() {
        if (exchangeRate == null) {
            cryptoTrx.setAmountSmallestUnit(0);
        } else {
            var cc = new CurrencyConverter(new ExchangeRate[]{exchangeRate});
            var amt = cc.convert(BigDecimal.valueOf(amount), exchangeRate.getPair().invert());
            cryptoTrx.setAmountSmallestUnit(getLedger().convertToSmallestAmount(amt));
        }
    }

    private void refreshAmount() {
        if (exchangeRate == null) {
            this.amount = UnknownAmount;
            return;
        }

        // Ccy read from pain.001 without exchange rates doesn't need a calc.
        if (!this.amount.equals(UnknownAmount) && exchangeRate.isNone()) {
            return;
        }

        var amt = getLedger().convertToNativeCcyAmount(getLedgerAmountSmallestUnit());
        var cc = new CurrencyConverter(new ExchangeRate[]{exchangeRate});
        this.amount = cc.convert(amt, exchangeRate.getPair());
        if (isCcyUnknown()) {
            this.ccy = exchangeRate.getPair().getFirst().equals(getLedgerCcy())
                    ? exchangeRate.getPair().getSecond()
                    : exchangeRate.getPair().getFirst();
        }
    }

    public String getFiatCcy() {
        return this.ccy;
    }

    public void setFiatCcy(String ccy) {
        this.ccy = ccy;
    }

    public void setAmountUnknown() {
        amount = UnknownAmount;
    }

    public boolean isAmountUnknown() {
        return amount == UnknownAmount;
    }

    public boolean isCcyUnknown() {
        return UnknownCCy.equals(ccy);
    }

    public void setExchangeRate(ExchangeRate rate) {
        var bothCcyKnown = !isAmountUnknown() && !isCcyUnknown();
        if (bothCcyKnown && rate != null && (!rate.getPair().affects(getFiatCcy()) || !rate.getPair().affects(getLedgerCcy()))) {
            throw new IllegalArgumentException(String.format("Exchange rate must affect %s and %s.", getFiatCcy(), getLedgerCcy()));
        }
        this.exchangeRate = rate;
        refreshAmounts();
    }

    public void refreshAmounts() {
        if (amountDefined) {
            refreshTransactionAmount();
        } else {
            refreshAmount();
        }
    }

    public long getLedgerAmountSmallestUnit() {
        return cryptoTrx.getAmountSmallestUnit();
    }

    public String getId() {
        return cryptoTrx.getId();
    }

    public StructuredReference[] getStructuredReferences() {
        return cryptoTrx.getStructuredReferences();
    }

    public String getInvoiceId() {
        return cryptoTrx.getInvoiceId();
    }

    public String[] getMessages() {
        return cryptoTrx.getMessages();
    }

    public void setSenderWallet(Wallet wallet) {
        cryptoTrx.setSenderWallet(wallet);
    }

    public void setReceiverWallet(Wallet wallet) {
        cryptoTrx.setReceiverWallet(wallet);
    }

    public Transaction getTransaction() {
        return cryptoTrx;
    }

    public String getLedgerCcy() {
        return cryptoTrx.getCcy();
    }

    public Ledger getLedger() {
        return cryptoTrx.getLedger();
    }

    public void addStructuredReference(StructuredReference structuredReference) {
        cryptoTrx.addStructuredReference(structuredReference);
    }

    public void addMessage(String message) {
        cryptoTrx.addMessage(message);
    }

    public ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    public String getDisplayText() {
        var amount = getAmount();
        if (amount != null) {
            return MoneyFormatter.formatFiat(BigDecimal.valueOf(amount), getFiatCcy());
        }

        var ledgerAmount = getLedger().convertToNativeCcyAmount(cryptoTrx.getAmountSmallestUnit());
        return MoneyFormatter.formatLedger(ledgerAmount, getLedgerCcy());
    }

    public Throwable getTransmissionError() {
        return cryptoTrx.getTransmissionError();
    }

    public CurrencyPair createCcyPair() {
        return new CurrencyPair(getLedgerCcy(), getFiatCcy());
    }
}
