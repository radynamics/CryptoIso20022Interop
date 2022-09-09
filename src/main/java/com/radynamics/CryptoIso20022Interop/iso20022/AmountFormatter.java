package com.radynamics.CryptoIso20022Interop.iso20022;

import com.radynamics.CryptoIso20022Interop.MoneyFormatter;
import com.radynamics.CryptoIso20022Interop.exchange.Money;
import com.radynamics.CryptoIso20022Interop.ui.Utils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class AmountFormatter {
    private static final NumberFormat dfFiat = Utils.createFormatFiat();
    private static final NumberFormat dfCryptocurrency = Utils.createFormatLedger();

    public static String formatAmt(Payment p) {
        if (p.isAmountUnknown()) {
            return "n/a";
        }
        if (p.getAmount() == null) {
            return "...";
        }

        var df = StringUtils.equalsIgnoreCase(p.getLedger().getNativeCcySymbol(), p.getUserCcyCodeOrEmpty())
                ? dfCryptocurrency
                : dfFiat;
        return df.format(p.getAmount());
    }

    public static String formatAmtWithCcy(Money amt) {
        if (amt == null) {
            return "n/a";
        }
        return MoneyFormatter.formatFiat(dfFiat.format(amt.getNumber()), amt.getCcy().getCode());
    }

    public static String formatAmtWithCcy(Payment p) {
        return p.isAmountUnknown() || p.getAmount() == null
                ? MoneyFormatter.formatFiat(formatAmt(p), p.getUserCcyCodeOrEmpty())
                : MoneyFormatter.formatFiat(BigDecimal.valueOf(p.getAmount()), p.getUserCcyCodeOrEmpty());
    }
}
