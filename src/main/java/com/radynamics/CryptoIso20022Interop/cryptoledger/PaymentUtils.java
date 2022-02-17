package com.radynamics.CryptoIso20022Interop.cryptoledger;

import com.radynamics.CryptoIso20022Interop.MoneyFormatter;
import com.radynamics.CryptoIso20022Interop.exchange.CurrencyPair;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRate;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRateProvider;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PaymentUtils {
    public static ArrayList<Wallet> distinctSendingWallets(Payment[] payments) {
        var list = new ArrayList<Wallet>();
        for (var p : payments) {
            var existing = list.stream().anyMatch(w -> WalletCompare.isSame(p.getSenderWallet(), w));
            if (!existing) {
                list.add(p.getSenderWallet());
            }
        }
        return list;
    }

    public static Ledger[] distinctLedgers(Payment[] payments) {
        var list = new ArrayList<Ledger>();
        for (var p : payments) {
            var existing = list.stream().anyMatch(l -> p.getLedger().getId().equals(l.getId()));
            if (!existing) {
                list.add(p.getLedger());
            }
        }
        return list.toArray(list.toArray(new Ledger[0]));
    }

    public static ArrayList<Payment> fromSender(Wallet w, Payment[] payments) {
        var list = new ArrayList<Payment>();
        for (var p : payments) {
            if (WalletCompare.isSame(p.getSenderWallet(), w)) {
                list.add(p);
            }
        }
        return list;
    }

    public static long sumSmallestLedgerUnit(Collection<Payment> payments) {
        long sum = 0;
        for (var p : payments) {
            sum += p.getLedgerAmountSmallestUnit();
        }
        return sum;
    }

    public static String sumString(ArrayList<Payment> payments) {
        var sb = new StringBuilder();
        var i = 0;
        var sums = sum(payments);
        for (var sum : sums.entrySet()) {
            sb.append(MoneyFormatter.formatFiat(BigDecimal.valueOf(sum.getValue()), sum.getKey()));
            if (i + 1 < sums.size()) {
                sb.append(", ");
            }
            i++;
        }
        return sb.toString();
    }

    public static Map<String, Double> sum(ArrayList<Payment> payments) {
        var map = new HashMap<String, Double>();
        for (var p : payments) {
            if (!map.containsKey(p.getFiatCcy())) {
                map.put(p.getFiatCcy(), (double) 0);
            }
            map.put(p.getFiatCcy(), map.get(p.getFiatCcy()) + p.getAmount());
        }
        return map;
    }

    public static String totalFeesText(Payment[] payments, ExchangeRateProvider provider) {
        if (payments.length == 0) {
            return "";
        }

        var sb = new StringBuilder();
        var i = 0;
        var fees = totalFees(payments);
        Double fiatSum = (double) 0;
        var fiatCcy = payments[0].getFiatCcy();
        for (var fee : fees.entrySet()) {
            var l = fee.getKey();
            var amt = l.convertToNativeCcyAmount(fee.getValue());

            var r = ExchangeRate.getOrNull(provider.latestRates(), new CurrencyPair(l.getNativeCcySymbol(), fiatCcy));
            fiatSum = r == null || fiatSum == null ? null : fiatSum + amt.longValue() * r.getRate();

            sb.append(MoneyFormatter.formatLedger(amt, l.getNativeCcySymbol()));
            if (i + 1 < fees.size()) {
                sb.append(", ");
            }
            i++;
        }

        return fiatSum == null
                ? sb.toString()
                : String.format("%s (%s)", MoneyFormatter.formatFiat(BigDecimal.valueOf(fiatSum), fiatCcy), sb);
    }

    public static Map<Ledger, Long> totalFees(Payment[] payments) {
        var map = new HashMap<Ledger, Long>();
        for (var p : payments) {
            if (!map.containsKey(p.getLedger())) {
                map.put(p.getLedger(), 0L);
            }
            map.put(p.getLedger(), map.get(p.getLedger()) + p.getFeeSmallestUnit());
        }
        return map;
    }
}
