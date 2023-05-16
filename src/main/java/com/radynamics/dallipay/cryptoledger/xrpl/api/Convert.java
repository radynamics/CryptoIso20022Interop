package com.radynamics.dallipay.cryptoledger.xrpl.api;

import com.radynamics.dallipay.iso20022.Utils;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xrpl.xrpl4j.model.transactions.ImmutableMemo;
import org.xrpl.xrpl4j.model.transactions.ImmutableMemoWrapper;

import java.io.UnsupportedEncodingException;

public final class Convert {
    private final static Logger log = LogManager.getLogger(Convert.class);

    // The standard format for currency codes is a three-character string such as USD. (https://xrpl.org/currency-formats.html)
    private static final int ccyCodeStandardFormatLength = 3;

    public static ImmutableMemoWrapper toMemoWrapper(String value) {
        return ImmutableMemoWrapper.builder().memo(
                ImmutableMemo.builder()
                        .memoData(Utils.stringToHex(value))
                        .memoFormat(Utils.stringToHex("json"))
                        .build()
        ).build();
    }

    public static String toCurrencyCode(String currency) {
        try {
            // trim() needed, due value is always 20 bytes, filled with 0.
            return currency.length() <= ccyCodeStandardFormatLength ? currency : Utils.hexToString(currency).trim();
        } catch (DecoderException | UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            return currency;
        }
    }
}
