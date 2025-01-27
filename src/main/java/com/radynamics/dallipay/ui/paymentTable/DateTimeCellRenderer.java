package com.radynamics.dallipay.ui.paymentTable;

import com.radynamics.dallipay.DateTimeConvert;
import com.radynamics.dallipay.ui.Utils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.ZonedDateTime;

public class DateTimeCellRenderer extends JLabel implements TableCellRenderer {
    public DateTimeCellRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value == null ? "" : Utils.createFormatDate().format(DateTimeConvert.toUserTimeZone((ZonedDateTime) value)));

        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        return this;
    }
}
