package com.radynamics.CryptoIso20022Interop.iso20022.camt054.camt05400102;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Ledger;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.iso20022.IdGenerator;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;
import com.radynamics.CryptoIso20022Interop.iso20022.UUIDIdGenerator;
import com.radynamics.CryptoIso20022Interop.iso20022.Utils;
import com.radynamics.CryptoIso20022Interop.iso20022.camt054.*;
import com.radynamics.CryptoIso20022Interop.iso20022.camt054.camt05400102.generated.*;
import com.radynamics.CryptoIso20022Interop.iso20022.creditorreference.StructuredReference;
import com.radynamics.CryptoIso20022Interop.transformation.TransformInstruction;
import org.apache.commons.lang3.NotImplementedException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Camt05400102Writer implements Camt054Writer {
    private final Ledger ledger;
    private TransformInstruction transformInstruction;
    private final String productVersion;
    private IdGenerator idGenerator;
    private LocalDateTime creationDate;

    public Camt05400102Writer(Ledger ledger, TransformInstruction transformInstruction, String productVersion) {
        this.ledger = ledger;
        this.transformInstruction = transformInstruction;
        this.productVersion = productVersion;
        this.idGenerator = new UUIDIdGenerator();
        this.creationDate = LocalDateTime.now();
    }

    @Override
    public Object createDocument(Payment[] transactions) throws Exception {
        var d = new Document();

        d.setBkToCstmrDbtCdtNtfctn(new BankToCustomerDebitCreditNotificationV02());
        d.getBkToCstmrDbtCdtNtfctn().setGrpHdr(new GroupHeader42());
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().setMsgId(idGenerator.createMsgId());
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().setCreDtTm(Utils.toXmlDateTime(creationDate));
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().setAddtlInf(String.format("CryptoIso20022Interop/%s", productVersion));
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().setMsgPgntn(new Pagination());
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().getMsgPgntn().setPgNb("1");
        d.getBkToCstmrDbtCdtNtfctn().getGrpHdr().getMsgPgntn().setLastPgInd(true);

        for (var t : transactions) {
            var receiver = t.getReceiverWallet();
            var stmt = getNtfctnOrNull(d, receiver);
            if (stmt == null) {
                stmt = new AccountNotification2();
                d.getBkToCstmrDbtCdtNtfctn().getNtfctn().add(stmt);
                stmt.setId(idGenerator.createStmId());
                stmt.setElctrncSeqNb(BigDecimal.valueOf(0));
                stmt.setCreDtTm(Utils.toXmlDateTime(creationDate));
                stmt.setAcct(createAcct(receiver));
            }

            stmt.getNtry().add(createNtry(t));
            stmt.setElctrncSeqNb(stmt.getElctrncSeqNb().add(BigDecimal.ONE));
        }
        return d;
    }

    private AccountNotification2 getNtfctnOrNull(Document d, Wallet receiver) {
        for (var ntfctn : d.getBkToCstmrDbtCdtNtfctn().getNtfctn()) {
            if (CashAccountCompare.isSame(ntfctn.getAcct(), createAcct(receiver))) {
                return ntfctn;
            }
        }
        return null;
    }

    private CashAccount20 createAcct(Wallet receiver) {
        var acct = new CashAccount20();
        acct.setId(new AccountIdentification4Choice());
        var iban = transformInstruction.getIbanOrNull(receiver);
        if (iban == null) {
            acct.getId().setOthr(new GenericAccountIdentification1());
            acct.getId().getOthr().setId(receiver.getPublicKey());
        } else {
            acct.getId().setIBAN(iban.getUnformatted());
        }
        acct.setCcy(transformInstruction.getTargetCcy());

        return acct;
    }

    private ReportEntry2 createNtry(Payment trx) throws DatatypeConfigurationException {
        var ntry = new ReportEntry2();

        // Seite 44: "Nicht standardisierte Verfahren: In anderen Fällen kann die «Referenz für den Kontoinhaber» geliefert werden."
        ntry.setNtryRef(trx.getSenderAccount().getUnformatted());
        ntry.setAmt(new ActiveOrHistoricCurrencyAndAmount());

        var amtValue = AmountRounder.round(trx.getAmount(), 2);
        ntry.getAmt().setValue(amtValue);
        ntry.getAmt().setCcy(trx.getFiatCcy());

        ntry.setCdtDbtInd(CreditDebitCode.CRDT);
        ntry.setSts(EntryStatus2Code.BOOK);

        var booked = Utils.toXmlDateTime(trx.getBooked());
        ntry.setBookgDt(createDateAndDateTimeChoice(booked, transformInstruction.getBookingDateFormat()));
        ntry.setValDt(createDateAndDateTimeChoice(booked, transformInstruction.getValutaDateFormat()));

        ntry.setBkTxCd(new BankTransactionCodeStructure4());
        ntry.getBkTxCd().setDomn(createDomn("VCOM"));

        var dtls = new EntryDetails1();
        ntry.getNtryDtls().add(dtls);
        dtls.setBtch(new BatchInformation2());
        dtls.getBtch().setNbOfTxs(String.valueOf(1));

        var txDtls = new EntryTransaction2();
        dtls.getTxDtls().add(txDtls);
        txDtls.setRefs(new TransactionReferences2());
        // Split due max allowed length of 35 per node (Max35Text)
        final int MAX_LEN = 35;
        var idPart0 = trx.getId().substring(0, MAX_LEN);
        var idPart1 = trx.getId().substring(MAX_LEN);
        txDtls.getRefs().setEndToEndId(idPart0);
        txDtls.getRefs().setMsgId(idPart1);

        txDtls.setAmtDtls(createAmtDtls(amtValue, trx.getFiatCcy()));
        txDtls.setBkTxCd(new BankTransactionCodeStructure4());
        txDtls.getBkTxCd().setDomn(createDomn("AUTT"));

        txDtls.setRltdAgts(new TransactionAgents2());
        txDtls.getRltdAgts().setDbtrAgt(new BranchAndFinancialInstitutionIdentification4());
        txDtls.getRltdAgts().getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentification7());
        txDtls.getRltdAgts().getDbtrAgt().getFinInstnId().setNm(ledger.getId());

        var structuredReferences = trx.getStructuredReferences();
        var hasStructuredReferences = structuredReferences != null && structuredReferences.length > 0;
        if (!hasStructuredReferences && transformInstruction.getCreditorReferenceIfMissing() != null) {
            structuredReferences = new StructuredReference[1];
            structuredReferences[0] = transformInstruction.getCreditorReferenceIfMissing();
            hasStructuredReferences = true;
        }

        var hasStrd = hasStructuredReferences || trx.getInvoiceId() != null;
        if (hasStrd || trx.getMessages().length > 0) {
            txDtls.setRmtInf(new RemittanceInformation5());
        }

        if (hasStrd) {
            txDtls.getRmtInf().getStrd().add(createStrd(structuredReferences, trx.getInvoiceId()));
        }

        var sb = new StringBuilder();
        for (String s : trx.getMessages()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(s);
        }
        if (sb.length() > 0) {
            txDtls.getRmtInf().getUstrd().add(sb.toString());
        }

        return ntry;
    }

    private AmountAndCurrencyExchange3 createAmtDtls(BigDecimal amt, String ccy) {
        var amtCcy = new AmountAndCurrencyExchangeDetails3();
        amtCcy.setAmt(new ActiveOrHistoricCurrencyAndAmount());
        amtCcy.getAmt().setValue(amt);
        amtCcy.getAmt().setCcy(ccy);

        var o = new AmountAndCurrencyExchange3();
        o.setInstdAmt(amtCcy);
        o.setTxAmt(amtCcy);
        return o;
    }

    private DateAndDateTimeChoice createDateAndDateTimeChoice(XMLGregorianCalendar dt, DateFormat format) {
        var value = (XMLGregorianCalendar) dt.clone();
        var o = new DateAndDateTimeChoice();
        switch (format) {
            case Date -> {
                value.setTime(0, 0, 0, 0);
                o.setDt(value);
                break;
            }
            case DateTime -> {
                o.setDtTm(value);
                break;
            }
            default -> throw new NotImplementedException(String.format("DateFormat %s unknown.", format));
        }
        return o;
    }

    private StructuredRemittanceInformation7 createStrd(StructuredReference[] structuredReferences, String invoiceNo) {
        StructuredRemittanceInformation7 strd = null;

        if (invoiceNo != null && invoiceNo.length() > 0) {
            strd = new StructuredRemittanceInformation7();
            var x = new ReferredDocumentInformation3();
            strd.getRfrdDocInf().add(x);
            x.setTp(new ReferredDocumentType2());
            x.getTp().setCdOrPrtry(new ReferredDocumentType1Choice());
            x.getTp().getCdOrPrtry().setCd(DocumentType5Code.CINV);
            x.setNb(invoiceNo);
        }

        if (structuredReferences != null && structuredReferences.length > 0) {
            if (strd == null) {
                strd = new StructuredRemittanceInformation7();
            }
            strd.setCdtrRefInf(new CreditorReferenceInformation2());
            for (var ref : structuredReferences) {
                strd.getCdtrRefInf().setTp(new CreditorReferenceType2());
                strd.getCdtrRefInf().getTp().setCdOrPrtry(new CreditorReferenceType1Choice());
                strd.getCdtrRefInf().getTp().getCdOrPrtry().setPrtry(CreditorReferenceConverter.toPrtry(ref.getType()));
                strd.getCdtrRefInf().setRef(ref.getUnformatted());
            }
        }

        return strd;
    }

    private BankTransactionCodeStructure5 createDomn(String subFmlCd) {
        var o = new BankTransactionCodeStructure5();
        o.setCd("PMNT");
        o.setFmly(new BankTransactionCodeStructure6());
        o.getFmly().setCd("RCDT");
        o.getFmly().setSubFmlyCd(subFmlCd);
        return o;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
