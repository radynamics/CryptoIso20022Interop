package com.radynamics.dallipay.cryptoledger.bitcoin;

import com.radynamics.dallipay.cryptoledger.bitcoin.signing.RpcSubmitter;
import com.radynamics.dallipay.cryptoledger.signing.TransactionSubmitter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class TransactionSubmitterFactory implements com.radynamics.dallipay.cryptoledger.signing.TransactionSubmitterFactory {
    private final com.radynamics.dallipay.cryptoledger.bitcoin.Ledger ledger;

    public TransactionSubmitterFactory(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public TransactionSubmitter create(String id, Component parentComponent) {
        switch (id) {
            case RpcSubmitter.Id: {
                return ledger.createRpcTransactionSubmitter();
            }
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }
    }

    @Override
    public TransactionSubmitter[] all(Component parentComponent) {
        var list = new ArrayList<TransactionSubmitter>();

        list.add(create(RpcSubmitter.Id, parentComponent));

        return list.toArray(new TransactionSubmitter[0]);
    }

    @Override
    public TransactionSubmitter getSuggested(Component parentComponent) {
        var all = Arrays.asList(all(parentComponent));
        all.sort(Comparator.comparingInt(o -> o.getInfo().getOrder()));
        return all.size() == 0 ? null : all.get(all.size() - 1);
    }
}
