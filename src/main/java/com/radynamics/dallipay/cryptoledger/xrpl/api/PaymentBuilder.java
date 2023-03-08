package com.radynamics.dallipay.cryptoledger.xrpl.api;

import com.google.common.primitives.UnsignedInteger;
import com.radynamics.dallipay.cryptoledger.FeeHelper;
import com.radynamics.dallipay.cryptoledger.FeeType;
import com.radynamics.dallipay.cryptoledger.LedgerException;
import com.radynamics.dallipay.cryptoledger.Transaction;
import com.radynamics.dallipay.cryptoledger.memo.PayloadConverter;
import com.radynamics.dallipay.exchange.Money;
import org.apache.commons.lang3.StringUtils;
import org.xrpl.xrpl4j.model.transactions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

public class PaymentBuilder {
    private Transaction transaction;

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }

    public PaymentBuilder payment(Transaction t) {
        this.transaction = t;
        return this;
    }

    public ImmutablePayment.Builder build() throws LedgerException {
        var sender = getSender();
        var receiver = Address.of(transaction.getReceiverWallet().getPublicKey());
        Optional<UnsignedInteger> destinationTag = transaction.getDestinationTag() == null ? Optional.empty() : Optional.of(UnsignedInteger.valueOf(transaction.getDestinationTag()));

        var memos = new ArrayList<MemoWrapper>();
        var memoData = PayloadConverter.toMemo(transaction.getStructuredReferences(), transaction.getMessages());
        if (!StringUtils.isEmpty(memoData)) {
            memos.add(Convert.toMemoWrapper(memoData));
        }

        var amount = toCurrencyAmount(transaction.getAmount());
        var lederTransactionFee = FeeHelper.get(transaction.getFees(), FeeType.LedgerTransactionFee).orElseThrow();
        var fee = XrpCurrencyAmount.ofXrp(BigDecimal.valueOf(lederTransactionFee.getNumber().doubleValue()));

        var builder = Payment.builder()
                .account(sender)
                .amount(amount)
                .addAllMemos(memos)
                .destination(receiver)
                .destinationTag(destinationTag)
                .fee(fee);
        var ccy = transaction.getAmount().getCcy();
        if (!transaction.getLedger().getNativeCcySymbol().equals(ccy.getCode())) {
            var transferFee = ccy.getTransferFeeAmount(transaction.getAmount());
            // maximum including an additional tolerance
            var sendMax = transaction.getAmount().plus(transferFee).plus(transferFee.multiply(0.01));
            builder.sendMax(toCurrencyAmount(sendMax));
        }
        return builder;
    }

    private CurrencyAmount toCurrencyAmount(Money amount) throws LedgerException {
        var ccy = amount.getCcy();
        if (ccy.getCode().equals(transaction.getLedger().getNativeCcySymbol())) {
            return XrpCurrencyAmount.ofXrp(BigDecimal.valueOf(amount.getNumber().doubleValue()));
        }

        if (ccy.getIssuer() == null) {
            throw new LedgerException(String.format("%s is considered an issued currency and therefore must have an issuer.", ccy.getCode()));
        }

        // 15 decimal digits of precision (Token Precision, https://xrpl.org/currency-formats.html)
        var scale = Math.pow(10, 15);
        var amt = Math.round(amount.getNumber().doubleValue() * scale) / scale;
        return IssuedCurrencyAmount.builder()
                .currency(ccy.getCode())
                .issuer(Address.of(ccy.getIssuer().getPublicKey()))
                .value(String.valueOf(amt))
                .build();
    }

    public Address getSender() {
        return Address.of(transaction.getSenderWallet().getPublicKey());
    }
}
