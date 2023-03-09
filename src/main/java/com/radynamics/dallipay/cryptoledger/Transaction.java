package com.radynamics.dallipay.cryptoledger;

import com.google.common.primitives.UnsignedInteger;
import com.radynamics.dallipay.cryptoledger.transaction.TransmissionState;
import com.radynamics.dallipay.exchange.Money;
import com.radynamics.dallipay.iso20022.creditorreference.StructuredReference;

import java.time.ZonedDateTime;

public interface Transaction {
    void setAmount(Money value);

    Money getAmount();

    ZonedDateTime getBooked();

    void setBooked(ZonedDateTime value);

    String getId();

    void setId(String value);

    Wallet getSenderWallet();

    void setSenderWallet(Wallet wallet);

    Wallet getReceiverWallet();

    void setReceiverWallet(Wallet wallet);

    UnsignedInteger getDestinationTag();

    void setDestinationTag(UnsignedInteger destinationTag);

    void addStructuredReference(StructuredReference structuredReference);

    void setStructuredReference(StructuredReference[] structuredReferences);

    StructuredReference[] getStructuredReferences();

    void removeStructuredReferences(int index);

    void addMessage(String message);

    String[] getMessages();

    void setMessage(String[] messages);

    String getInvoiceId();

    void setInvoiceId(String s);

    Ledger getLedger();

    TransmissionState getTransmission();

    Throwable getTransmissionError();

    Fee[] getFees();

    void setLedgerTransactionFee(Money value);
}
