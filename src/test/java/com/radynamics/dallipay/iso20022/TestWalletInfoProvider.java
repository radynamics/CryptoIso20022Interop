package com.radynamics.dallipay.iso20022;

import com.radynamics.dallipay.cryptoledger.Wallet;
import com.radynamics.dallipay.cryptoledger.WalletInfo;
import com.radynamics.dallipay.cryptoledger.WalletInfoProvider;
import com.radynamics.dallipay.cryptoledger.generic.walletinfo.InfoType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestWalletInfoProvider implements WalletInfoProvider {
    private final Map<String, ArrayList<WalletInfo>> list = new HashMap<>();


    public void addName(String walletPublicKey, String name) {
        add(walletPublicKey, new WalletInfo(this, name, InfoType.Name));
    }

    public void add(String walletPublicKey, WalletInfo info) {
        if (!list.containsKey(walletPublicKey)) {
            list.put(walletPublicKey, new ArrayList<>());
        }
        list.get(walletPublicKey).add(info);
    }

    @Override
    public WalletInfo[] list(Wallet wallet) {
        for (var entry : list.entrySet()) {
            if (entry.getKey().equals(wallet.getPublicKey())) {
                return entry.getValue().toArray(new WalletInfo[0]);
            }
        }
        return new WalletInfo[0];
    }

    @Override
    public String getDisplayText() {
        return "Test provider";
    }

    @Override
    public InfoType[] supportedTypes() {
        return new InfoType[]{InfoType.Name};
    }
}
