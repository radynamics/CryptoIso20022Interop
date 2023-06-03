package com.radynamics.dallipay.iso20022;

import com.radynamics.dallipay.MoneyFormatter;
import com.radynamics.dallipay.cryptoledger.*;
import com.radynamics.dallipay.cryptoledger.signing.NullSubmitter;
import com.radynamics.dallipay.cryptoledger.signing.TransactionSubmitter;
import com.radynamics.dallipay.cryptoledger.transaction.Origin;
import com.radynamics.dallipay.cryptoledger.transaction.TransmissionState;
import com.radynamics.dallipay.exchange.*;
import com.radynamics.dallipay.iso20022.creditorreference.StructuredReference;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Payment {
    private Transaction cryptoTrx;
    private String endToEndId;
    private Account senderAccount;
    private Account receiverAccount;
    private Address senderAddress;
    private Address receiverAddress;
    private Double amount = UnknownAmount;
    private Currency ccy = UnknownCCy;
    private ExchangeRate exchangeRate;
    private Origin origin;
    private ExpectedCurrency expectedCcy;
    private TransactionSubmitter submitter = new NullSubmitter();

    private static final Double UnknownAmount = Double.valueOf(0);
    private static final Currency UnknownCCy = null;
    private boolean amountDefined;

    public Payment(Transaction cryptoTrx) {
        if (cryptoTrx == null) throw new IllegalArgumentException("Parameter 'cryptoTrx' cannot be null");
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

    public ZonedDateTime getBooked() {
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

    public String getDestinationTag() {
        return cryptoTrx.getDestinationTag();
    }

    public void setDestinationTag(String destinationTag) {
        cryptoTrx.setDestinationTag(destinationTag);
    }

    public Double getAmount() {
        return this.amount;
    }

    public void setAmount(Money amt) {
        if (amt == null) throw new IllegalArgumentException("Parameter 'amt' cannot be null");
        var rounded = isLedgerCcy(amt.getCcy()) ? getLedger().roundNativeCcy(amt) : amt;
        this.amount = rounded.getNumber().doubleValue();
        this.ccy = rounded.getCcy();
        if (!UnknownAmount.equals(rounded.getNumber())) {
            this.amountDefined = true;
        }

        refreshTransactionAmount();
    }

    private void refreshTransactionAmount() {
        if (ccy.getIssuer() != null) {
            setExchangeRate(null, false);
            cryptoTrx.setAmount(Money.of(amount, ccy));
            return;
        }

        if (exchangeRate == null) {
            if (!ccy.equals(cryptoTrx.getAmount().getCcy())) {
                // Use ledger native currency if there is no specific issued currency
                cryptoTrx.setAmount(Money.of(0d, new Currency(cryptoTrx.getLedger().getNativeCcySymbol())));
            } else {
                var amt = isLedgerCcy() ? amount : 0;
                cryptoTrx.setAmount(Money.of(amt, cryptoTrx.getAmount().getCcy()));
            }
            return;
        }

        var cc = new CurrencyConverter(new ExchangeRate[]{exchangeRate});
        var amt = cc.convert(BigDecimal.valueOf(amount), exchangeRate.getPair().invert());
        cryptoTrx.setAmount(getLedger().roundNativeCcy(Money.of(amt, new Currency(cryptoTrx.getLedger().getNativeCcySymbol()))));
    }

    private boolean isLedgerCcy() {
        return isLedgerCcy(ccy);
    }

    private boolean isLedgerCcy(Currency ccy) {
        return cryptoTrx.getLedger().getNativeCcySymbol().equals(ccy.getCode());
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

        var amt = BigDecimal.valueOf(getAmountTransaction().getNumber().doubleValue());
        var cc = new CurrencyConverter(new ExchangeRate[]{exchangeRate});
        this.amount = cc.convert(amt, exchangeRate.getPair());
        if (isCcyUnknown()) {
            this.ccy = exchangeRate.getPair().getFirst().equals(getAmountTransaction().getCcy())
                    ? exchangeRate.getPair().getSecond()
                    : exchangeRate.getPair().getFirst();
        }
    }

    public String getUserCcyCodeOrEmpty() {
        return this.ccy == null ? "" : this.ccy.getCode();
    }

    public Currency getUserCcy() {
        return this.ccy;
    }

    public void setUserCcy(Currency ccy) {
        this.ccy = ccy;
    }

    public void setAmountUnknown() {
        amount = UnknownAmount;
    }

    public boolean isAmountUnknown() {
        return Payment.isAmountUnknown(amount);
    }

    private static boolean isAmountUnknown(Number value) {
        return value != null && Double.compare(value.doubleValue(), UnknownAmount) == 0;
    }

    public static boolean isAmountUnknown(Money value) {
        return value == null || Payment.isAmountUnknown(value.getNumber());
    }

    public boolean isCcyUnknown() {
        return UnknownCCy == ccy;
    }

    public void setExchangeRate(ExchangeRate rate) {
        setExchangeRate(rate, true);
    }

    private void setExchangeRate(ExchangeRate rate, boolean refreshAmounts) {
        var bothCcyKnown = !isAmountUnknown() && !isCcyUnknown();
        if (rate != null) {
            var affectsFiat = rate.getPair().affects(getUserCcyCodeOrEmpty());
            var affectsLedger = rate.getPair().affects(getAmountTransaction().getCcy().getCode());
            if (!affectsLedger) {
                throw new IllegalArgumentException(String.format("Exchange rate must affect %s", getAmountTransaction().getCcy().getCode()));
            }
            if (bothCcyKnown && !affectsFiat) {
                throw new IllegalArgumentException(String.format("Exchange rate must affect %s and %s.", getUserCcyCodeOrEmpty(), getAmountTransaction().getCcy().getCode()));
            }
        }
        this.exchangeRate = rate;
        if (refreshAmounts) {
            refreshAmounts();
        }
    }

    public void refreshAmounts() {
        if (amountDefined) {
            refreshTransactionAmount();
        } else {
            refreshAmount();
        }
    }

    public boolean isUserCcyEqualTransactionCcy() {
        return getUserCcy().equals(getAmountTransaction().getCcy());
    }

    public Money getAmountTransaction() {
        return cryptoTrx.getAmount();
    }

    public String getId() {
        return cryptoTrx.getId();
    }

    public String getEndToEndId() {
        return endToEndId != null ? endToEndId : cryptoTrx.getId();
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
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

    public void setMessage(String[] messages) {
        cryptoTrx.setMessage(messages);
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

    public Ledger getLedger() {
        return cryptoTrx.getLedger();
    }

    public void addStructuredReference(StructuredReference structuredReference) {
        cryptoTrx.addStructuredReference(structuredReference);
    }

    public void setStructuredReference(StructuredReference[] structuredReferences) {
        cryptoTrx.setStructuredReference(structuredReferences);
    }

    public void addMessage(String message) {
        cryptoTrx.addMessage(message);
    }

    public ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    public Fee[] getFees() {
        return cryptoTrx.getFees();
    }

    public void setLedgerTransactionFee(Money value) {
        cryptoTrx.setLedgerTransactionFee(value);
    }

    public String getDisplayText() {
        var amount = getAmount();
        if (amount != null) {
            return MoneyFormatter.formatFiat(BigDecimal.valueOf(amount), getUserCcyCodeOrEmpty());
        }

        return MoneyFormatter.formatLedger(cryptoTrx.getAmount());
    }

    public Throwable getTransmissionError() {
        return cryptoTrx.getTransmissionError();
    }

    public CurrencyPair createCcyPair() {
        return new CurrencyPair(getAmountTransaction().getCcy(), getUserCcy());
    }

    public boolean isSameCcy() {
        return cryptoTrx.getAmount().getCcy().equals(getUserCcy());
    }

    public boolean is(Transaction t) {
        return cryptoTrx == t;
    }

    public void refreshPaymentPath(CurrencyConverter currencyConverter) {
        var availablePaths = getLedger().createPaymentPathFinder().find(currencyConverter, this);
        if (availablePaths.length == 0) {
            return;
        }

        var pathSameCcyCode = Arrays.stream(availablePaths)
                .filter(o -> o.getCcy().withoutIssuer().equals(getUserCcy()))
                .findFirst()
                .orElse(null);
        if (pathSameCcyCode == null) {
            Arrays.sort(availablePaths, (a, b) -> Integer.compare(b.getRank(), a.getRank()));
            availablePaths[0].apply(this);
        } else {
            setAmount(Money.of(getAmount(), pathSameCcyCode.getCcy()));
            pathSameCcyCode.apply(this);
        }
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public ExpectedCurrency getExpectedCurrency() {
        return expectedCcy;
    }

    public void setExpectedCurrency(ExpectedCurrency expectedCcy) {
        this.expectedCcy = expectedCcy;
    }

    public TransactionSubmitter getSubmitter() {
        return this.submitter;
    }

    public void setSubmitter(TransactionSubmitter submitter) {
        if (submitter == null) throw new IllegalArgumentException("Parameter 'submitter' cannot be null");
        this.submitter = submitter;
    }
}
