package com.radynamics.dallipay.iso20022.camt054;

import com.radynamics.dallipay.iso20022.creditorreference.ReferenceType;
import org.apache.commons.lang3.NotImplementedException;

public class CreditorReferenceConverter {
    public static String toPrtry(ReferenceType value) {
        switch (value) {
            case Unknown:
                return null;
            case Scor:
                return "SCOR";
            case SwissQrBill:
                return "QRR";
            default:
                throw new NotImplementedException(value.name());
        }
    }
}
