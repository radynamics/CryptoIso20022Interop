package com.radynamics.CryptoIso20022Interop.ui;

import com.radynamics.CryptoIso20022Interop.exchange.CurrencyConverter;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;
import com.radynamics.CryptoIso20022Interop.iso20022.PaymentConverter;
import com.radynamics.CryptoIso20022Interop.iso20022.pain001.Pain001Reader;
import com.radynamics.CryptoIso20022Interop.iso20022.pain001.TransactionValidator;
import com.radynamics.CryptoIso20022Interop.transformation.TransactionTranslator;
import com.radynamics.CryptoIso20022Interop.transformation.TransformInstruction;
import com.radynamics.CryptoIso20022Interop.ui.paymentTable.Actor;
import com.radynamics.CryptoIso20022Interop.ui.paymentTable.PaymentTable;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;

public class SendForm extends JFrame {
    private TransformInstruction transformInstruction;
    private CurrencyConverter currencyConverter;

    private PaymentTable table;
    private FilePathField txtInput;
    private Pain001Reader reader;

    public SendForm(TransformInstruction transformInstruction, CurrencyConverter currencyConverter) {
        if (transformInstruction == null) throw new IllegalArgumentException("Parameter 'transformInstruction' cannot be null");
        if (currencyConverter == null) throw new IllegalArgumentException("Parameter 'currencyConverter' cannot be null");
        this.transformInstruction = transformInstruction;
        this.currencyConverter = currencyConverter;

        setupUI();
    }

    private void setupUI() {
        setTitle("CryptoIso20022Interop");

        try {
            setIconImage(new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream("img/productIcon.png"))).getImage());
        } catch (IOException e) {
            LogManager.getLogger().error(e);
        }

        var pnlMain = new JPanel();
        pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlMain);

        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.Y_AXIS));

        var innerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        JPanel panel0 = new JPanel();
        panel0.setBorder(innerBorder);
        panel0.setLayout(new BoxLayout(panel0, BoxLayout.X_AXIS));
        JPanel panel1 = new JPanel();
        panel1.setBorder(innerBorder);
        var panel1Layout = new SpringLayout();
        panel1.setLayout(panel1Layout);
        JPanel panel2 = new JPanel();
        panel2.setBorder(innerBorder);
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
        JPanel panel3 = new JPanel();
        panel3.setBorder(innerBorder);
        var panel3Layout = new SpringLayout();
        panel3.setLayout(panel3Layout/*new FlowLayout(FlowLayout.RIGHT)*/);

        pnlMain.add(panel0);
        pnlMain.add(panel1);
        pnlMain.add(panel2);
        pnlMain.add(panel3);

        panel0.setMinimumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel0.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel0.setPreferredSize(new Dimension(500, 50));
        panel1.setMinimumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel1.setPreferredSize(new Dimension(500, 70));
        panel2.setPreferredSize(new Dimension(500, 500));
        panel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel3.setPreferredSize(new Dimension(500, 45));

        {
            var lbl = new JLabel();
            lbl.setText("Send Payments");
            lbl.putClientProperty("FlatLaf.style", "font: 200% $semibold.font");
            lbl.setOpaque(true);
            panel0.add(lbl);
        }

        {
            Component anchorComponentTopLeft;
            {
                var lbl = new JLabel("Input:");
                anchorComponentTopLeft = lbl;
                panel1Layout.putConstraint(SpringLayout.WEST, lbl, 0, SpringLayout.WEST, panel1);
                panel1Layout.putConstraint(SpringLayout.NORTH, lbl, 5, SpringLayout.NORTH, panel1);
                lbl.setOpaque(true);
                panel1.add(lbl);

                txtInput = new FilePathField();
                panel1Layout.putConstraint(SpringLayout.WEST, txtInput, 50, SpringLayout.EAST, anchorComponentTopLeft);
                panel1Layout.putConstraint(SpringLayout.NORTH, txtInput, 5, SpringLayout.NORTH, panel1);
                txtInput.addChangedListener(() -> {
                    try {
                        var t = new TransactionTranslator(transformInstruction, currencyConverter);
                        var payments = t.apply(reader.read(new FileInputStream(txtInput.getText())));

                        load(payments);
                    } catch (Exception e) {
                        ExceptionDialog.show(this, e);
                    }
                });
                panel1.add(txtInput);
            }
            {
                var lbl = new JLabel("Exchange:");
                panel1Layout.putConstraint(SpringLayout.WEST, lbl, 0, SpringLayout.WEST, panel1);
                panel1Layout.putConstraint(SpringLayout.NORTH, lbl, 35, SpringLayout.NORTH, panel1);
                lbl.setOpaque(true);
                panel1.add(lbl);

                var lbl2 = new JLabel(transformInstruction.getExchange().getDisplayText());
                panel1Layout.putConstraint(SpringLayout.WEST, lbl2, 50, SpringLayout.EAST, anchorComponentTopLeft);
                panel1Layout.putConstraint(SpringLayout.NORTH, lbl2, 35, SpringLayout.NORTH, panel1);
                panel1.add(lbl2);
            }
        }
        {
            table = new PaymentTable(transformInstruction, currencyConverter, Actor.Sender, new TransactionValidator());
            panel2.add(table);
        }
        {
            var cmd = new JButton("Send Payments");
            cmd.setPreferredSize(new Dimension(150, 35));
            cmd.addActionListener(e -> {
                var payments = table.selectedPayments();
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    transformInstruction.getLedger().send(PaymentConverter.toTransactions(payments));
                    for (var p : payments) {
                        table.refresh(p);
                    }
                } catch (Exception ex) {
                    LogManager.getLogger().error(ex);
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            });
            panel3Layout.putConstraint(SpringLayout.EAST, cmd, 0, SpringLayout.EAST, panel3);
            panel3.add(cmd);
        }
    }

    private void load(Payment[] payments) {
        table.load(payments);
    }

    public void setInput(String value) {
        txtInput.setText(value);
    }

    public void setReader(Pain001Reader reader) {
        this.reader = reader;
    }
}
