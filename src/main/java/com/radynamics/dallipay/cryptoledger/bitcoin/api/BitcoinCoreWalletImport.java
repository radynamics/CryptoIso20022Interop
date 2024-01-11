package com.radynamics.dallipay.cryptoledger.bitcoin.api;

import com.radynamics.dallipay.cryptoledger.WalletSetupProcess;
import com.radynamics.dallipay.cryptoledger.bitcoin.Ledger;
import com.radynamics.dallipay.ui.WaitingForm;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BitcoinCoreWalletImport implements WalletSetupProcess {
    private final Component parentComponent;
    private final Ledger ledger;

    private final ResourceBundle res = ResourceBundle.getBundle("i18n." + this.getClass().getSimpleName());

    public BitcoinCoreWalletImport(Component parentComponent, Ledger ledger) {
        this.parentComponent = parentComponent;
        this.ledger = ledger;
    }

    @Override
    public void start() {
        var frm = new BitcoinCoreWalletImportForm();
        frm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frm.setSize(500, 400);
        frm.setModal(true);
        frm.setLocationRelativeTo(parentComponent);
        frm.setVisible(true);

        if (!frm.isDialogAccepted()) {
            return;
        }

        var dlg = WaitingForm.create(null, res.getString("walletImportInProgress"));
        Future<Boolean> future = Executors.newCachedThreadPool().submit(() -> {
            try {
                if (frm.importWalletAddress() && !StringUtils.isEmpty(frm.walletAddress())) {
                    ledger.importWallet(frm.walletName().orElse(frm.walletAddress()), frm.historicTransactionSince(), ledger.createWallet(frm.walletAddress()));
                    return true;
                } else if (frm.importDevice() && frm.device() != null) {
                    ledger.importWallet(frm.walletName().orElse(frm.device().type()), frm.historicTransactionSince(), frm.device());
                    return true;
                } else {
                    return false;
                }
            } catch (ApiException e) {
                throw new RuntimeException(e);
            } finally {
                dlg.setVisible(false);
            }
        });

        try {
            dlg.setVisible(true);
            if (!future.get()) {
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            var errorJson = BitcoinCoreRpcClientExt.errorJson(e.getCause());
            if (errorJson.isPresent()) {
                final var ERR_RESCAN_ABORTED_BY_USER = -1;
                if (errorJson.get().getInt("code") == ERR_RESCAN_ABORTED_BY_USER) {
                    return;
                }
            }
            throw new RuntimeException(e);
        }

        JOptionPane.showMessageDialog(parentComponent, res.getString("successfullyAdded"), res.getString("successfullyAddedTitle"), JOptionPane.INFORMATION_MESSAGE);
    }
}
