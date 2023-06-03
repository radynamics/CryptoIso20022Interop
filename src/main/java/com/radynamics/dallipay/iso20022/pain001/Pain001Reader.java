package com.radynamics.dallipay.iso20022.pain001;

import com.radynamics.dallipay.cryptoledger.Ledger;
import com.radynamics.dallipay.cryptoledger.transaction.Origin;
import com.radynamics.dallipay.exchange.Currency;
import com.radynamics.dallipay.exchange.Money;
import com.radynamics.dallipay.iso20022.*;
import com.radynamics.dallipay.iso20022.creditorreference.ReferenceType;
import com.radynamics.dallipay.iso20022.creditorreference.StructuredReferenceFactory;
import com.radynamics.dallipay.iso20022.pain001.pain00100103.generated.CashAccount16;
import com.radynamics.dallipay.iso20022.pain001.pain00100103.generated.CreditorReferenceInformation2;
import com.radynamics.dallipay.iso20022.pain001.pain00100103.generated.Document;
import com.radynamics.dallipay.iso20022.pain001.pain00100103.generated.PartyIdentification32;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.ArrayList;

public class Pain001Reader implements PaymentInstructionReader {
    final static Logger log = LogManager.getLogger(Pain001Reader.class);
    private final Ledger ledger;

    public Pain001Reader(Ledger ledger) {
        this.ledger = ledger;
    }

    public Payment[] read(InputStream pain001) throws Exception {
        var doc = fromXml(pain001);

        if (doc.getCstmrCdtTrfInitn() == null) {
            return new Payment[0];
        }

        var list = new ArrayList<Payment>();
        for (var pmtInf : doc.getCstmrCdtTrfInitn().getPmtInf()) {
            var senderAccount = getAccount(pmtInf.getDbtrAcct());
            var senderAddress = getAddress(pmtInf.getDbtr());
            for (var cdtTrfTxInf : pmtInf.getCdtTrfTxInf()) {
                var receiverAccount = getAccount(cdtTrfTxInf.getCdtrAcct());
                var sourceCcy = cdtTrfTxInf.getAmt().getInstdAmt() == null ? null : cdtTrfTxInf.getAmt().getInstdAmt().getCcy();
                var sourceAmt = cdtTrfTxInf.getAmt().getInstdAmt() == null ? null : cdtTrfTxInf.getAmt().getInstdAmt().getValue();
                var eqvtAmt = cdtTrfTxInf.getAmt().getEqvtAmt();
                if (eqvtAmt != null) {
                    sourceCcy = eqvtAmt.getAmt() == null ? null : eqvtAmt.getAmt().getCcy();
                    sourceAmt = eqvtAmt.getAmt() == null ? null : eqvtAmt.getAmt().getValue();
                }

                var t = new Payment(ledger.createTransaction());
                t.setEndToEndId(cdtTrfTxInf.getPmtId().getEndToEndId());
                t.setSenderAccount(senderAccount);
                t.setSenderWallet(ReaderUtils.toValidWalletOrNull(ledger, senderAccount));
                t.setSenderAddress(senderAddress);
                t.setReceiverAccount(receiverAccount);
                t.setReceiverWallet(ReaderUtils.toValidWalletOrNull(ledger, receiverAccount));
                t.setReceiverAddress(getAddress(cdtTrfTxInf.getCdtr()));
                t.setOrigin(Origin.Imported);
                if (sourceAmt == null || sourceCcy == null) {
                    t.setAmountUnknown();
                } else {
                    t.setAmount(Money.of(sourceAmt.doubleValue(), new Currency(sourceCcy)));
                }

                var rmtInf = cdtTrfTxInf.getRmtInf();
                if (rmtInf != null) {
                    if (rmtInf.getStrd() != null) {
                        for (var strd : rmtInf.getStrd()) {
                            var cdtrRefInf = strd.getCdtrRefInf();
                            if (cdtrRefInf == null) {
                                continue;
                            }
                            var typeText = getReferenceType(cdtrRefInf);
                            var reference = cdtrRefInf.getRef();
                            t.addStructuredReference(StructuredReferenceFactory.create(typeText, reference));

                            for (var addtlRmtInf : strd.getAddtlRmtInf()) {
                                t.addMessage(addtlRmtInf);
                            }
                        }
                    }

                    if (rmtInf.getUstrd() != null) {
                        for (var ustrd : rmtInf.getUstrd()) {
                            t.addMessage(ustrd);
                        }
                    }
                }

                list.add(t);
            }
        }

        log.trace(String.format("%s payments read from pain001", list.size()));
        return list.toArray(new Payment[0]);
    }

    private Address getAddress(PartyIdentification32 obj) {
        if (obj == null) {
            return null;
        }

        var a = new Address(obj.getNm());
        if (obj.getPstlAdr() == null) {
            return a;
        }

        var pstlAdr = obj.getPstlAdr();
        var adrLines = pstlAdr.getAdrLine();
        if (adrLines.size() == 2) {
            a.setStreet(adrLines.get(0));
            a.setCity(adrLines.get(1));
            return a;
        }
        if (adrLines.size() == 1) {
            a.setCity(adrLines.get(0));
            return a;
        }

        if (!StringUtils.isAllEmpty(pstlAdr.getStrtNm())) {
            a.setStreet(StringUtils.isAllEmpty(pstlAdr.getBldgNb()) ? pstlAdr.getStrtNm() : String.format("%s %s", pstlAdr.getStrtNm(), pstlAdr.getBldgNb()));
        }
        if (!StringUtils.isAllEmpty(pstlAdr.getPstCd())) {
            a.setZip(pstlAdr.getPstCd());
        }
        if (!StringUtils.isAllEmpty(pstlAdr.getTwnNm())) {
            a.setCity(pstlAdr.getTwnNm());
        }
        if (!StringUtils.isAllEmpty(pstlAdr.getCtry())) {
            a.setCountryShort(pstlAdr.getCtry());
        }

        return a;
    }

    private ReferenceType getReferenceType(CreditorReferenceInformation2 cdtrRefInf) {
        var tp = cdtrRefInf.getTp();
        if (tp == null) {
            return StructuredReferenceFactory.detectType(cdtrRefInf.getRef());
        }

        var cdOrPrtry = tp.getCdOrPrtry();
        var typeText = cdOrPrtry.getCd() == null ? cdOrPrtry.getPrtry() : cdOrPrtry.getCd().value();
        return StructuredReferenceFactory.getType(typeText);
    }

    private Account getAccount(CashAccount16 acct) {
        if (acct == null) {
            return null;
        }

        var id = acct.getId();
        return id.getIBAN() != null ? new IbanAccount(id.getIBAN()) : new OtherAccount(id.getOthr().getId());
    }

    private Document fromXml(InputStream input) throws JAXBException, XMLStreamException {
        // TODO: RST 2021-12-31 manually ensure input matches ISO version (ex "pain.001.001.03") without regional derived xsd.
        var xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        var xsr = xif.createXMLStreamReader(input);

        var ctx = JAXBContext.newInstance(Document.class);
        var jaxbUnmarshaller = ctx.createUnmarshaller();
        return (Document) jaxbUnmarshaller.unmarshal(xsr);
    }

    public Ledger getLedger() {
        return ledger;
    }

    @Override
    public JPanel createParameterPanel() {
        return null;
    }

    @Override
    public boolean applyParameters(JPanel parameterPanel) {
        return true;
    }
}
