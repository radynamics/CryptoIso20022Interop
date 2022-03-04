package com.radynamics.CryptoIso20022Interop.ui.paymentTable;

import com.radynamics.CryptoIso20022Interop.cryptoledger.PaymentUtils;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.TransmissionState;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationState;
import com.radynamics.CryptoIso20022Interop.db.AccountMapping;
import com.radynamics.CryptoIso20022Interop.db.AccountMappingRepo;
import com.radynamics.CryptoIso20022Interop.exchange.CurrencyConverter;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRateProvider;
import com.radynamics.CryptoIso20022Interop.exchange.HistoricExchangeRateLoader;
import com.radynamics.CryptoIso20022Interop.iso20022.*;
import com.radynamics.CryptoIso20022Interop.transformation.TransformInstruction;
import com.radynamics.CryptoIso20022Interop.ui.ExceptionDialog;
import com.radynamics.CryptoIso20022Interop.ui.PaymentDetailForm;
import com.radynamics.CryptoIso20022Interop.ui.TableColumnBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class PaymentTable extends JPanel {
    final static Logger log = LogManager.getLogger(PaymentTable.class);
    private final JTable table;
    private final PaymentTableModel model;
    private TransformInstruction transformInstruction;
    private final Actor actor;
    private Payment[] data = new Payment[0];
    private PaymentValidator validator;
    private ArrayList<ProgressListener> listener = new ArrayList<>();

    public PaymentTable(TransformInstruction transformInstruction, CurrencyConverter currencyConverter, Actor actor, PaymentValidator validator) {
        super(new GridLayout(1, 0));
        this.transformInstruction = transformInstruction;
        this.actor = actor;
        this.validator = validator;

        var exchangeRateLoader = new HistoricExchangeRateLoader(transformInstruction, currencyConverter);
        model = new PaymentTableModel(exchangeRateLoader, validator);
        model.setActor(actor);
        model.addProgressListener(progress -> raiseProgress(progress));

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(ValidationState.class, new ValidationStateCellRenderer(table.getColumn(PaymentTableModel.COL_VALIDATION_RESULTS)));
        table.setDefaultRenderer(TransmissionState.class, new TransmissionCellRenderer());
        table.setDefaultRenderer(IbanAccount.class, new AccountCellRenderer());
        table.setDefaultRenderer(OtherAccount.class, new AccountCellRenderer());
        table.setDefaultRenderer(Address.class, new AddressCellRenderer());
        table.setDefaultRenderer(LocalDateTime.class, new LocalDateTimeCellRenderer());
        {
            var column = table.getColumnModel().getColumn(model.findColumn(PaymentTableModel.COL_ACTOR_ISO20022));
            if (actor == Actor.Receiver) {
                column.setCellEditor(new AccountCellEditor(true));
                column.setCellRenderer(new AccountCellRenderer());
            }
        }
        var lookupProvider = transformInstruction.getLedger().getLookupProvider();
        var objectColumn = table.getColumn(PaymentTableModel.COL_OBJECT);
        var cellEditor = new WalletCellEditor(objectColumn, lookupProvider, actor == Actor.Sender);
        table.getColumnModel().getColumn(model.findColumn(PaymentTableModel.COL_SENDER_LEDGER)).setCellEditor(cellEditor);
        table.getColumnModel().getColumn(model.findColumn(PaymentTableModel.COL_RECEIVER_LEDGER)).setCellEditor(cellEditor);

        table.setRowHeight(30);
        initColumns();

        add(new JScrollPane(table));
    }

    private void initColumns() {
        var cb = new TableColumnBuilder(table);
        cb.forColumn(PaymentTableModel.COL_OBJECT).headerCenter().hide();
        cb.forColumn(PaymentTableModel.COL_VALIDATION_RESULTS).headerCenter().hide();
        cb.forColumn(PaymentTableModel.COL_SELECTOR).headerValue("").fixedWidth(40);
        {
            var c = cb.forColumn(PaymentTableModel.COL_STATUS).headerValue("").fixedWidth(40).headerCenter().getColumn();
            c.setCellRenderer(new ValidationStateCellRenderer(table.getColumn(PaymentTableModel.COL_VALIDATION_RESULTS)));
        }
        {
            var c = cb.forColumn(PaymentTableModel.COL_SENDER_LEDGER).headerValue("Sender Wallet").width(200).getColumn();
            c.setCellRenderer(new WalletCellRenderer());
        }
        {
            var headerValue = model.getActor().get("Receiver from Input", "Sender for Export");
            cb.forColumn(PaymentTableModel.COL_ACTOR_ISO20022).headerValue(headerValue).width(200);
        }
        {
            var c = cb.forColumn(PaymentTableModel.COL_RECEIVER_LEDGER).headerValue("Receiver Wallet").width(200).getColumn();
            c.setCellRenderer(new WalletCellRenderer());
        }
        {
            var c = cb.forColumn(PaymentTableModel.COL_BOOKED).headerValue("Booked").width(90).getColumn();
            c.setCellRenderer(new LocalDateTimeCellRenderer());
            if (model.getActor() == Actor.Sender) {
                cb.hide();
            }
        }
        {
            var c = cb.forColumn(PaymentTableModel.COL_AMOUNT).headerValue("Amount").width(100).headerRigth().getColumn();
            c.setCellRenderer(new AmountCellRenderer(transformInstruction, table.getColumn(PaymentTableModel.COL_OBJECT)));
        }
        {
            var c = cb.forColumn(PaymentTableModel.COL_CCY).headerValue("").maxWidth(50).getColumn();
            c.setCellRenderer(new DefaultTableCellRenderer());
        }
        cb.forColumn(PaymentTableModel.COL_TRX_STATUS).headerValue("").maxWidth(50);
        {
            var c = cb.forColumn(PaymentTableModel.COL_DETAIL).headerValue("").maxWidth(50).headerCenter().getColumn();
            c.setCellRenderer(new ShowDetailCellRenderer());
        }

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showMore(getSelectedRow(table));
                    return;
                }

                var clickedColumn = table.getColumnModel().getColumn(table.columnAtPoint(e.getPoint()));
                if (!StringUtils.equals((String) clickedColumn.getIdentifier(), PaymentTableModel.COL_DETAIL)) {
                    return;
                }

                if (e.getClickCount() == 1) {
                    showMore(getSelectedRow(table));
                }
            }
        });
        new TableCellListener(table, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onCellEdited((TableCellListener) e.getSource());
            }
        });
    }

    private void onCellEdited(TableCellListener tcl) {
        var cleanedInput = tcl.getNewValue().toString().trim();
        var row = tcl.getRow();
        var t = (Payment) model.getValueAt(row, table.getColumnModel().getColumnIndex(PaymentTableModel.COL_OBJECT));

        var editedActor = getEditedActor(tcl.getColumn());
        AccountMapping mapping = new AccountMapping(t.getLedger().getId());
        // While processing received payments user is able to change "Sender for Export" multiple times and therefor oldValue ca be a String.
        var a = tcl.getOldValue() instanceof Account
                ? (Account) tcl.getOldValue()
                : createAccountOrNull((String) tcl.getOldValue());
        var w = editedActor == Actor.Sender ? t.getSenderWallet() : t.getReceiverWallet();
        mapping.setAccount(a);
        mapping.setWallet(w);
        try (var repo = new AccountMappingRepo()) {
            mapping = repo.single(t.getLedger().getId(), a, w).orElse(mapping);
        } catch (Exception ex) {
            ExceptionDialog.show(table, ex);
        }

        var changed = false;
        if (tcl.getColumn() == table.getColumnModel().getColumnIndex(PaymentTableModel.COL_SENDER_LEDGER)) {
            mapping.setWallet(createWalletOrNull(cleanedInput));
            PaymentUtils.apply(t, mapping);
            changed = true;
        }
        if (tcl.getColumn() == table.getColumnModel().getColumnIndex(PaymentTableModel.COL_ACTOR_ISO20022)) {
            mapping.setAccount(createAccountOrNull(cleanedInput));
            PaymentUtils.apply(t, mapping);
            changed = true;
        }
        if (tcl.getColumn() == table.getColumnModel().getColumnIndex(PaymentTableModel.COL_RECEIVER_LEDGER)) {
            mapping.setWallet(createWalletOrNull(cleanedInput));
            PaymentUtils.apply(t, mapping);
            changed = true;
        }

        if (!changed) {
            return;
        }

        try (var repo = new AccountMappingRepo()) {
            if (mapping.allPresent()) {
                repo.saveOrUpdate(mapping);
            } else if (mapping.isPersisted() && mapping.accountOrWalletMissing()) {
                // Interpret "" as removal. During creation values are maybe not yet defined.
                repo.delete(mapping);
            }
            repo.commit();
        } catch (Exception ex) {
            ExceptionDialog.show(table, ex);
        }

        // Also update other affected payments using same mapping
        for (var p : data) {
            if (PaymentUtils.apply(p, mapping)) {
                model.onAccountOrWalletsChanged(p);
            }
        }
    }

    private Actor getEditedActor(int col) {
        if (col == table.getColumnModel().getColumnIndex(PaymentTableModel.COL_SENDER_LEDGER)) {
            return Actor.Sender;
        }
        // While processing received payments user is able to change "Sender for Export".
        if (col == table.getColumnModel().getColumnIndex(PaymentTableModel.COL_ACTOR_ISO20022) && actor == Actor.Receiver) {
            return Actor.Sender;
        }

        return Actor.Receiver;
    }

    private Wallet createWalletOrNull(String text) {
        return StringUtils.isEmpty(text) ? null : transformInstruction.getLedger().createWallet(text, null);
    }

    private Account createAccountOrNull(String text) {
        return StringUtils.isEmpty(text) ? null : AccountFactory.create(text);
    }

    public void load(Payment[] data) {
        this.data = data;
        model.load(data);
        table.revalidate();
        table.repaint();
    }

    private Payment getSelectedRow(JTable table) {
        var row = table.getSelectedRow();
        var col = table.getColumn(PaymentTableModel.COL_OBJECT).getModelIndex();
        return (Payment) table.getModel().getValueAt(row, col);
    }

    private void showMore(Payment obj) {
        var frm = new PaymentDetailForm(obj, validator, getExchangeRateProvider());
        frm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frm.setSize(650, 430);
        frm.setModal(true);
        frm.setLocationRelativeTo(this);
        frm.setVisible(true);

        if (frm.getPaymentChanged()) {
            refresh(obj);
        }
    }

    private ExchangeRateProvider getExchangeRateProvider() {
        return actor == Actor.Sender
                ? transformInstruction.getExchangeRateProvider()
                : transformInstruction.getHistoricExchangeRateSource();
    }

    public Payment[] selectedPayments() {
        return model.selectedPayments();
    }

    public void refresh(Payment t) {
        var row = getRow(t);
        if (row == -1) {
            log.warn(String.format("Could not find %s in table.", t.getReceiverAccount().getUnformatted()));
            return;
        }
        model.onTransactionChanged(t);
    }

    private int getRow(Payment t) {
        var col = table.getColumn(PaymentTableModel.COL_OBJECT).getModelIndex();
        for (var row = 0; row < table.getModel().getRowCount(); row++) {
            if (table.getModel().getValueAt(row, col) == t) {
                return row;
            }
        }
        return -1;
    }

    public void addProgressListener(ProgressListener l) {
        listener.add(l);
    }

    private void raiseProgress(Progress progress) {
        for (var l : listener) {
            l.onProgress(progress);
        }
    }
}
