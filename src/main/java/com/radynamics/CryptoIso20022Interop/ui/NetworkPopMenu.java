package com.radynamics.CryptoIso20022Interop.ui;

import com.alexandriasoftware.swing.action.SplitButtonClickedActionListener;
import com.radynamics.CryptoIso20022Interop.cryptoledger.Network;
import com.radynamics.CryptoIso20022Interop.cryptoledger.NetworkHelper;
import com.radynamics.CryptoIso20022Interop.cryptoledger.NetworkInfo;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class NetworkPopMenu {
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final ArrayList<Pair<JCheckBoxMenuItem, NetworkInfo>> selectableEntries = new ArrayList<>();

    private final ArrayList<ChangedListener> changedListener = new ArrayList<>();

    public NetworkPopMenu(NetworkInfo[] networks) {
        var index = 0;
        for (var network : networks) {
            addEntry(network, String.format("%s network", NetworkHelper.toShort(network.getType())), index++);
        }

        {
            var pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            // TODO: enable option after support for sidechains is implemented.
            //popupMenu.add(pnl);
            pnl.setBorder(new EmptyBorder(10, 20, 0, 0));
            pnl.setBackground(popupMenu.getBackground());
            var txt = new JSidechainTextField();
            pnl.add(txt);
            txt.setPreferredSize(new Dimension(180, 21));
            txt.addChangedListener(value -> {
                popupMenu.setVisible(false);
                if (StringUtils.isEmpty(value)) {
                    return;
                }

                HttpUrl httpUrl;
                try {
                    httpUrl = HttpUrl.get(value);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(popupMenu, String.format("Could not parse %s as Http RPC endpoint.", value));
                    return;
                }

                // TODO: Sidechains are also "live" networks.
                var networkInfo = new NetworkInfo(Network.Test, httpUrl);
                var item = addEntryAtEnd(networkInfo, value);

                onNetworkChanged(item);
            });
        }
    }

    private JCheckBoxMenuItem addEntryAtEnd(NetworkInfo networkInfo, String text) {
        return addEntry(networkInfo, text, popupMenu.getComponentCount() - 1);
    }

    private JCheckBoxMenuItem addEntry(NetworkInfo networkInfo, String text, int index) {
        var item = new JCheckBoxMenuItem(text);
        popupMenu.add(item, index);
        selectableEntries.add(new ImmutablePair<>(item, networkInfo));
        item.addActionListener((SplitButtonClickedActionListener) e -> onNetworkChanged(item));

        return item;
    }

    private void onNetworkChanged(JCheckBoxMenuItem item) {
        stateChanged(item);
        raiseChanged();
    }

    private void stateChanged(JCheckBoxMenuItem selected) {
        for (var item : selectableEntries) {
            item.getKey().setSelected(item.getKey() == selected);
        }
    }

    public JPopupMenu get() {
        return popupMenu;
    }

    public NetworkInfo getSelectedNetwork() {
        for (var item : selectableEntries) {
            if (item.getKey().isSelected()) {
                return item.getValue();
            }
        }
        return null;
    }

    public void setSelectedNetwork(NetworkInfo network) {
        for (var item : selectableEntries) {
            if (item.getValue().getUrl().equals(network.getUrl())) {
                item.getKey().setSelected(true);
                return;
            }
        }

        var item = addEntryAtEnd(network, String.format("%s network", NetworkHelper.toShort(network.getType())));
        onNetworkChanged(item);
    }

    public void addChangedListener(ChangedListener l) {
        changedListener.add(l);
    }

    private void raiseChanged() {
        for (var l : changedListener) {
            l.onChanged();
        }
    }
}
