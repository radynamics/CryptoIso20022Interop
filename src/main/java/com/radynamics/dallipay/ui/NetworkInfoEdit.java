package com.radynamics.dallipay.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.radynamics.dallipay.cryptoledger.NetworkInfo;
import com.radynamics.dallipay.util.RequestFocusListener;
import okhttp3.HttpUrl;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class NetworkInfoEdit {
    private static final ResourceBundle res = ResourceBundle.getBundle("i18n." + NetworkInfoEdit.class.getSimpleName());

    public static NetworkInfo show(Component parent, HttpUrl url, String displayName) {
        var txtName = new JTextField();
        var txtRpcUrl = new JTextField();
        var txtNetworkId = new JTextField();

        txtName.setText(displayName);
        txtName.addAncestorListener(new RequestFocusListener());
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, res.getString("displayName.placeholderText"));
        txtRpcUrl.setText(url == null ? "" : url.toString());
        txtRpcUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, res.getString("rpcUrl.placeholderText"));
        txtNetworkId.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, res.getString("networkId.placeholderText"));

        var pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        pnl.setPreferredSize(new Dimension(350, 70));
        pnl.add(new JLabel(res.getString("displayName")), createGridConstraints(0.3, 1, 0, 0));
        pnl.add(txtName, createGridConstraints(0.7, 1, 1, 0));
        pnl.add(new JLabel(res.getString("rpcUrl")), createGridConstraints(0.3, 1, 0, 1));
        pnl.add(txtRpcUrl, createGridConstraints(0.7, 1, 1, 1));
        pnl.add(new JLabel(res.getString("networkId")), createGridConstraints(0.3, 1, 0, 2));
        pnl.add(txtNetworkId, createGridConstraints(0.7, 1, 1, 2));

        int result = JOptionPane.showConfirmDialog(null, pnl, res.getString("descText"), JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        var displayText = txtName.getText().trim();
        var rpcUrlText = txtRpcUrl.getText().trim();
        var networkIdText = txtNetworkId.getText().trim();
        if (displayText.length() == 0 || rpcUrlText.length() == 0) {
            return null;
        }

        HttpUrl httpUrl;
        try {
            httpUrl = HttpUrl.get(rpcUrlText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, String.format(res.getString("urlParseFailed"), rpcUrlText), res.getString("connectionFailed"), JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        var info = NetworkInfo.create(httpUrl, displayText);
        info.setNetworkId(toIntegerOrNull(networkIdText));
        return info;
    }

    private static Integer toIntegerOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static GridBagConstraints createGridConstraints(double weightx, double weighty, int x, int y) {
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = weightx;
        c.weighty = weighty;
        c.gridx = x;
        c.gridy = y;
        return c;
    }
}