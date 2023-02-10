package com.radynamics.CryptoIso20022Interop.ui.paymentTable;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.Origin;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class RemoveCellRenderer extends JLabel implements TableCellRenderer {
    private final static int WIDTH = 16;
    private final static int HEIGHT = 16;

    private final static FlatSVGIcon delete = new FlatSVGIcon("svg/delete.svg", WIDTH, HEIGHT);

    public RemoveCellRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        var origin = (Origin) value;

        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setHorizontalAlignment(JLabel.CENTER);
        setIcon(origin.isDeletable() ? delete : null);
        setToolTipText("Remove");
        return this;
    }
}
