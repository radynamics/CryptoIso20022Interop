package com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.cryptoledger.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.net.URI;

public class XrplOrg implements WalletLookupProvider, TransactionLookupProvider {
    private final static Logger log = LogManager.getLogger(XrplOrg.class);
    private final String baseUrl;

    public static final String Id = "xrplExplorer";
    public static final String displayName = "XRPL Explorer";

    public XrplOrg(NetworkInfo network) throws LookupProviderException {
        if (network.isLivenet()) {
            this.baseUrl = "https://livenet.xrpl.org";
        } else if (network.isTestnet()) {
            this.baseUrl = "https://testnet.xrpl.org";
        } else {
            throw new LookupProviderException(String.format("%s doesn't support network %s.", displayName, network.getShortText()));
        }
    }

    @Override
    public void open(Wallet wallet) {
        openInBrowser("accounts", wallet.getPublicKey());
    }

    @Override
    public void open(String transactionId) {
        openInBrowser("transactions", transactionId);
    }

    private void openInBrowser(String suffix, String value) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(String.format("%s/%s/%s", baseUrl, suffix, value)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No desktop or no browsing supported");
        }
    }
}
