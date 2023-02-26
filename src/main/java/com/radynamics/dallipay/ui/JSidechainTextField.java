package com.radynamics.dallipay.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class JSidechainTextField extends JTextField {
    private final ArrayList<SidechainChangedListener> sidechainChangedListener = new ArrayList<>();

    public JSidechainTextField() {
        putClientProperty("JTextField.placeholderText", "Add custom sidechain");
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

        registerKeyboardAction(e -> onAccept(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void onAccept() {
        var value = getText();
        setText("");
        raiseSidechainChanged(value);
    }

    public void addChangedListener(SidechainChangedListener l) {
        sidechainChangedListener.add(l);
    }

    private void raiseSidechainChanged(String value) {
        for (var l : sidechainChangedListener) {
            l.onChanged(value);
        }
    }
}