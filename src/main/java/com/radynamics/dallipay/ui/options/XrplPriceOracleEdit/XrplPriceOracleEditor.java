package com.radynamics.dallipay.ui.options.XrplPriceOracleEdit;

import com.alexandriasoftware.swing.JSplitButton;
import com.alexandriasoftware.swing.action.SplitButtonClickedActionListener;
import com.radynamics.dallipay.cryptoledger.Ledger;
import com.radynamics.dallipay.cryptoledger.LedgerId;
import com.radynamics.dallipay.cryptoledger.generic.Wallet;
import com.radynamics.dallipay.cryptoledger.xrpl.IssuedCurrency;
import com.radynamics.dallipay.exchange.CurrencyPair;
import com.radynamics.dallipay.ui.TableColumnBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class XrplPriceOracleEditor extends JPanel {
    private final JTable table;
    private final IssuedCurrencyTableModel model = new IssuedCurrencyTableModel();
    private JSplitButton defaultPriceOracles;
    private Ledger ledger;

    private final ResourceBundle res = ResourceBundle.getBundle("i18n." + this.getClass().getSimpleName());

    public XrplPriceOracleEditor() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        table = new JTable(model);
        var cb = new TableColumnBuilder(table);
        cb.forColumn(IssuedCurrencyTableModel.COL_FROM).width(45).headerValue(res.getString("from"));
        cb.forColumn(IssuedCurrencyTableModel.COL_TO).width(45).headerValue(res.getString("to"));
        cb.forColumn(IssuedCurrencyTableModel.COL_ISSUER).width(300).headerValue(res.getString("issuer"));
        cb.forColumn(IssuedCurrencyTableModel.COL_RECEIVER).width(300).headerValue(res.getString("receiver"));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane sp = new JScrollPane(table);
        add(sp);
        sp.setPreferredSize(new Dimension(table.getPreferredSize().width + 20, 100));

        {
            var pnl = new JPanel();
            add(pnl, BorderLayout.PAGE_END);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.LINE_AXIS));
            pnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            pnl.add(Box.createHorizontalGlue());
            {

                defaultPriceOracles = new JSplitButton(res.getString("default"));
                defaultPriceOracles.setAlwaysPopup(true);
                defaultPriceOracles.setPreferredSize(new Dimension(90, 21));
                pnl.add(defaultPriceOracles);
            }
            pnl.add(Box.createRigidArea(new Dimension(5, 0)));
            {
                var cmd = new JButton("-");
                cmd.setPreferredSize(new Dimension(21, 21));
                cmd.addActionListener(e -> onRemove());
                pnl.add(cmd);
            }
            pnl.add(Box.createRigidArea(new Dimension(5, 0)));
            {
                var cmd = new JButton("+");
                cmd.setPreferredSize(new Dimension(21, 21));
                cmd.addActionListener(e -> onAdd());
                pnl.add(cmd);
            }
        }
    }

    private void onAdd() {
        var issuer = new com.radynamics.dallipay.cryptoledger.generic.Wallet(ledger.getId(), "rKaEc...");
        var receiver = new com.radynamics.dallipay.cryptoledger.generic.Wallet(ledger.getId(), "rKaEc...");
        var ic = new IssuedCurrency(new CurrencyPair(ledger.getNativeCcySymbol(), "USD"), issuer, receiver);
        var index = model.getRowIndex(model.newRecord(ic));
        table.setRowSelectionInterval(index, index);
    }

    private void onRemove() {
        model.remove(table.getSelectedRow());
    }

    public void load(List<IssuedCurrency> data) {
        model.load(toRecords(data));
    }

    public void apply() {
        var cellEditor = table.getCellEditor();
        if (cellEditor == null) {
            return;
        }

        // When user clicks "Save" while editing a cell, ensure new values are applied.
        cellEditor.stopCellEditing();
    }

    public List<IssuedCurrency> issuedCurrencies() {
        return toIssuesCurrencies(model.issuedCurrencies());
    }

    private List<Record> toRecords(List<IssuedCurrency> data) {
        var list = new ArrayList<Record>();
        for (var o : data) {
            list.add(new Record(o));
        }
        return list;
    }

    private List<IssuedCurrency> toIssuesCurrencies(List<Record> data) {
        var list = new ArrayList<IssuedCurrency>();
        for (var o : data) {
            list.add(new IssuedCurrency(new CurrencyPair(o.first, o.second),
                    new Wallet(LedgerId.Xrpl, o.issuer),
                    new Wallet(LedgerId.Xrpl, o.receiver)));
        }
        return list;
    }

    public void init(Ledger ledger) {
        if (ledger == null) throw new IllegalArgumentException("Parameter 'ledger' cannot be null");
        this.ledger = ledger;
        refreshDefaultPopupMenu();
    }

    private void refreshDefaultPopupMenu() {
        var popupMenu = new JPopupMenu();
        for (var o : ledger.getDefaultPriceOracles()) {
            var item = new JMenuItem(o.getDisplayText());
            popupMenu.add(item);
            item.addActionListener((SplitButtonClickedActionListener) e -> load(o.getIssuedCurrencies()));
        }

        if (popupMenu.getComponentCount() == 0) {
            var item = new JMenuItem(res.getString("noneAvailable"));
            popupMenu.add(item);
            item.setEnabled(false);
        }

        defaultPriceOracles.setPopupMenu(popupMenu);
    }
}
