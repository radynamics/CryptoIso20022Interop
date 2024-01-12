package com.radynamics.dallipay.ui;

import com.alexandriasoftware.swing.JSplitButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.radynamics.dallipay.DateTimeRange;
import com.radynamics.dallipay.ReturnCode;
import com.radynamics.dallipay.VersionController;
import com.radynamics.dallipay.cryptoledger.*;
import com.radynamics.dallipay.db.ConfigRepo;
import com.radynamics.dallipay.exchange.CurrencyConverter;
import com.radynamics.dallipay.paymentrequest.EmbeddedServer;
import com.radynamics.dallipay.transformation.PaymentRequestUri;
import com.radynamics.dallipay.transformation.TransformInstruction;
import com.radynamics.dallipay.transformation.TransformInstructionFactory;
import com.radynamics.dallipay.update.OnlineUpdate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_MINIMUM_TAB_WIDTH;

public class MainForm extends JFrame {
    private TransformInstruction transformInstruction;
    private JTabbedPane tabbedPane;
    private SendForm sendingPanel;
    private ReceiveForm receivingPanel;
    private OptionsForm optionsPanel;
    private JSplitButton cmdLedger;
    private JSplitButton cmdNetwork;
    private NetworkPopMenu networkPopupMenu;

    private final ResourceBundle res = ResourceBundle.getBundle("i18n." + this.getClass().getSimpleName());
    private final String ITEM_OBJECT_ID = "itemObjectId";

    public MainForm() {
        setupUI();
    }

    private void setupUI() {
        var vc = new VersionController();
        setTitle(String.format("DalliPay [%s]", vc.getVersion()));
        setIconImage(Utils.getProductIcon());

        setJMenuBar(createMenuBar());

        var pnlMain = new JPanel();
        add(pnlMain);
        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel();
        var mainContentBorder = new EmptyBorder(0, 10, 10, 10);
        final int TABBEDPANE_WIDTH = 100;
        {
            final int HEIGHT = 80;
            var pnl = new JPanel();
            pnlMain.add(pnl);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
            pnl.setMinimumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
            pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
            pnl.setPreferredSize(new Dimension(500, HEIGHT));
            {
                var lbl = new JLabel();
                lbl.setIcon(Utils.getScaled("img/productIcon.png", 32, 32));
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setMinimumSize(new Dimension(TABBEDPANE_WIDTH, HEIGHT));
                lbl.setMaximumSize(new Dimension(TABBEDPANE_WIDTH, HEIGHT));
                lbl.setPreferredSize(new Dimension(TABBEDPANE_WIDTH, HEIGHT));
                pnl.add(lbl);
            }
            {
                lblTitle.setBorder(BorderFactory.createEmptyBorder(0, mainContentBorder.getBorderInsets().left, 0, 0));
                pnl.add(lblTitle);
                lblTitle.putClientProperty("FlatLaf.styleClass", "h1");
            }
        }
        tabbedPane = new JTabbedPane();
        {
            var pnl = new JPanel();
            pnlMain.add(pnl);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
            {
                pnl.add(tabbedPane);
                tabbedPane.putClientProperty(TABBED_PANE_MINIMUM_TAB_WIDTH, TABBEDPANE_WIDTH);
                tabbedPane.setTabPlacement(JTabbedPane.LEFT);
                tabbedPane.addChangeListener(e -> {
                    var selected = tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
                    if (selected instanceof MainFormPane) {
                        lblTitle.setText(((MainFormPane) selected).getTitle());
                    } else {
                        lblTitle.setText("");
                    }
                });

                {
                    sendingPanel = new SendForm();
                    sendingPanel.setBorder(mainContentBorder);
                    tabbedPane.addTab(res.getString("send"), sendingPanel);
                }
                {
                    receivingPanel = new ReceiveForm();
                    receivingPanel.setBorder(mainContentBorder);
                    tabbedPane.addTab(res.getString("receive"), receivingPanel);
                }
                {
                    tabbedPane.addTab("", new JPanel());
                    tabbedPane.setEnabledAt(2, false);
                }
                {
                    optionsPanel = new OptionsForm();
                    optionsPanel.addChangedListener(this::refreshChangedSettings);
                    optionsPanel.setBorder(mainContentBorder);
                    tabbedPane.addTab(res.getString("options"), optionsPanel);
                }
            }
        }

        var paymentRequestServer = new EmbeddedServer();
        paymentRequestServer.addRequestListenerListener(this::onPaymentRequestReceived);
        try {
            paymentRequestServer.start();
        } catch (IOException e) {
            ExceptionDialog.show(this, e);
        }
    }

    private void onPaymentRequestReceived(URI requestUrl) {
        if (!PaymentRequestUri.matches(requestUrl.toString())) {
            return;
        }

        Utils.bringToFront(this);
        tabbedPane.setSelectedComponent(sendingPanel);

        PaymentRequestUri paymentRequestUri;
        try {
            paymentRequestUri = PaymentRequestUri.create(transformInstruction.getLedger(), requestUrl);
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
            return;
        }
        var actual = transformInstruction.getNetwork();
        var expected = paymentRequestUri.networkInfo();
        if (expected != null && actual.getNetworkId() != expected.getNetworkId()) {
            var expectedLedger = LedgerFactory.create(paymentRequestUri.ledgerId());
            askSwitchingNetwork(transformInstruction.getLedger(), actual, expectedLedger, expected);
        }

        sendingPanel.addNewPaymentByRequest(paymentRequestUri);
    }

    private void askSwitchingNetwork(Ledger actualLedger, NetworkInfo actual, Ledger expectedLedger, NetworkInfo expected) {
        var actualText = "%s: %s (NetworkID %s)".formatted(actualLedger.getDisplayText(), actual.getDisplayName(), actual.getNetworkId());
        var expectedText = "%s: %s (NetworkID %s)".formatted(expectedLedger.getDisplayText(), expected.getDisplayName(), expected.getNetworkId());
        var text = res.getString("switchNetwork").formatted(actualText, expectedText);
        int ret = JOptionPane.showConfirmDialog(this, text, res.getString("switchNetworkTitle"), JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret != JOptionPane.YES_OPTION) {
            return;
        }

        if (!transformInstruction.getLedger().getId().sameAs(expectedLedger.getId())) {
            onLedgerClicked(expectedLedger);
        }
        if (actual.getNetworkId() != expected.getNetworkId()) {
            networkPopupMenu.setSelectedNetwork(expected);
        }
    }

    private void refreshChangedSettings() {

        Wallet defaultSenderWallet = null;
        String selectedCcy = null;
        var ledger = transformInstruction.getLedger();
        var historicExchangeRateSource = transformInstruction.getHistoricExchangeRateSource();
        try (var repo = new ConfigRepo()) {
            defaultSenderWallet = repo.getDefaultSenderWallet(ledger);
            selectedCcy = repo.getTargetCcy(transformInstruction.getTargetCcy());
            historicExchangeRateSource.init(repo);
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }

        sendingPanel.refreshDefaultSenderWallet(ledger.getId(), defaultSenderWallet);
        receivingPanel.refreshTargetCcys(selectedCcy, historicExchangeRateSource);
    }

    private JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();
        menuBar.add(Box.createGlue());

        {
            OnlineUpdate.search().thenAccept((updateInfo) -> {
                if (updateInfo == null) {
                    return;
                }

                if (updateInfo.isMandatory()) {
                    var text = String.format(res.getString("mandatoryUpdateAvailable"), updateInfo.getVersion());
                    int ret = JOptionPane.showConfirmDialog(this, text, "Update", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (ret == JOptionPane.YES_OPTION) {
                        Utils.openBrowser(this, updateInfo.getUri());
                    }
                    System.exit(ReturnCode.MandatoryUpdate.value);
                }

                var cmdUpdate = new FlatButton();
                var icon = new FlatSVGIcon("svg/update.svg", 16, 16);
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Consts.ColorAccent));
                cmdUpdate.setIcon(icon);
                var text = String.format(res.getString("updateAvailable"), updateInfo.getVersion());
                cmdUpdate.setToolTipText(text);
                cmdUpdate.setButtonType(FlatButton.ButtonType.toolBarButton);
                cmdUpdate.setFocusable(false);
                cmdUpdate.addActionListener(e -> {
                    Utils.openBrowser(this, updateInfo.getUri());
                });
                menuBar.add(cmdUpdate);
                menuBar.updateUI();

                int ret = JOptionPane.showConfirmDialog(this, text, "Update", JOptionPane.YES_NO_CANCEL_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    Utils.openBrowser(this, updateInfo.getUri());
                }
            });
        }

        final String DROPDOWN_ARROW_OVERLAP_HACK = "     ";
        cmdLedger = new JSplitButton(DROPDOWN_ARROW_OVERLAP_HACK);
        cmdLedger.setBackground(getBackground());
        cmdLedger.setBorder(BorderFactory.createEmptyBorder());
        cmdLedger.setAlwaysPopup(true);
        menuBar.add(cmdLedger);

        menuBar.add(Box.createHorizontalStrut(10));

        cmdNetwork = new JSplitButton(DROPDOWN_ARROW_OVERLAP_HACK);
        cmdNetwork.setBorder(BorderFactory.createEmptyBorder());
        cmdNetwork.setBackground(getBackground());
        cmdNetwork.setAlwaysPopup(true);
        menuBar.add(cmdNetwork);

        return menuBar;
    }

    private void refreshLedgerButton(Ledger ledger) throws Exception {
        JMenuItem item = null;
        var compareValue = ledger.getId().numericId();
        for (var e : cmdLedger.getPopupMenu().getSubElements()) {
            var mi = (JMenuItem) e;
            if (mi.getClientProperty(ITEM_OBJECT_ID).equals(compareValue)) {
                item = mi;
                break;
            }
        }

        if (item == null) {
            throw new Exception("MenuItem for %s not found.".formatted(compareValue));
        }

        cmdLedger.setIcon(item.getIcon());
        cmdLedger.setToolTipText(item.getText());
    }

    private void onLedgerClicked(Ledger ledger) {
        if (transformInstruction.getLedger().getId().sameAs(ledger.getId())) {
            return;
        }
        setTransformInstruction(TransformInstructionFactory.create(ledger, transformInstruction.getConfig().getLoadedFilePath(), transformInstruction.getNetwork().getId()));
        try {
            refreshLedgerButton(ledger);
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }
        saveLastUsedLedger(ledger.getId());
    }

    private void refreshNetworkButton() {
        var icon = new FlatSVGIcon("svg/network.svg", 16, 16);
        var networkInfo = transformInstruction.getNetwork();
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> networkInfo.isLivenet() ? Consts.ColorLivenet : Consts.ColorTestnet));
        cmdNetwork.setIcon(icon);
        cmdNetwork.setToolTipText(String.format(res.getString("currentlyUsing"), networkInfo.getShortText(), Utils.withoutPath(networkInfo.getUrl().uri())));
    }

    private void saveLastUsedLedger(LedgerId ledgerId) {
        try (var repo = new ConfigRepo()) {
            repo.saveLastUsedLedger(ledgerId);
            repo.commit();
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }
    }

    private void saveLastUsedNetwork(NetworkInfo selected) {
        try (var repo = new ConfigRepo()) {
            repo.setLastUsedRpcUrl(transformInstruction.getLedger(), selected.getUrl());
            repo.commit();
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }
    }

    private NetworkInfo[] getCustomSidechains() {
        try (var repo = new ConfigRepo()) {
            return repo.getCustomSidechains(transformInstruction.getLedger());
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }
        return new NetworkInfo[0];
    }

    public void setTransformInstruction(TransformInstruction transformInstruction) {
        if (transformInstruction == null) throw new IllegalArgumentException("Parameter 'transformInstruction' cannot be null");
        this.transformInstruction = transformInstruction;

        var provider = transformInstruction.getExchangeRateProvider();
        provider.load();

        sendingPanel.init(transformInstruction, new CurrencyConverter(provider.latestRates()));
        receivingPanel.init(transformInstruction, new CurrencyConverter());
        optionsPanel.init(transformInstruction.getLedger());
        optionsPanel.load();

        cmdLedger.setPopupMenu(createLedgerPopMenu());
        try {
            refreshLedgerButton(transformInstruction.getLedger());
        } catch (Exception e) {
            ExceptionDialog.show(this, e);
        }
        cmdNetwork.setPopupMenu(createNetworkPopMenu().get());
        refreshNetworkButton();

        refreshChangedSettings();
    }

    private JPopupMenu createLedgerPopMenu() {
        var popupMenu = new JPopupMenu();
        for (var l : LedgerFactory.all()) {
            var item = new JMenuItem(l.getDisplayText(), l.getIcon());
            item.putClientProperty(ITEM_OBJECT_ID, l.getId().numericId());
            item.addActionListener(e -> onLedgerClicked(l));
            popupMenu.add(item);
        }

        return popupMenu;
    }

    private NetworkPopMenu createNetworkPopMenu() {
        var networkInfos = new ArrayList<>(List.of(transformInstruction.getConfig().getNetworkInfos()));
        networkInfos.addAll(List.of(getCustomSidechains()));

        networkPopupMenu = new NetworkPopMenu(this, transformInstruction.getLedger(), networkInfos.toArray(NetworkInfo[]::new));
        networkPopupMenu.setSelectedNetwork(transformInstruction.getNetwork());
        networkPopupMenu.addChangedListener(() -> {
            var selected = networkPopupMenu.getSelectedNetwork();
            if (selected == null) {
                return;
            }
            transformInstruction.setNetwork(selected);
            refreshNetworkButton();
            sendingPanel.setNetwork(selected);
            saveLastUsedNetwork(selected);
        });
        return networkPopupMenu;
    }

    public void setInputFileName(String inputFileName) {
        sendingPanel.setInput(inputFileName);
    }

    public void setReceivingWallet(Wallet wallet) {
        receivingPanel.setWallet(wallet);
    }

    public void setOutputFileName(String outputFileName) {
        receivingPanel.setTargetFileName(outputFileName);
    }

    public void setPeriod(DateTimeRange period) {
        receivingPanel.setPeriod(period);
    }
}
