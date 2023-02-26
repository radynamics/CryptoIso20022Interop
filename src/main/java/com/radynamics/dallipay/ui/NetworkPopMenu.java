package com.radynamics.dallipay.ui;

import com.alexandriasoftware.swing.action.SplitButtonClickedActionListener;
import com.radynamics.dallipay.cryptoledger.NetworkInfo;
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
            addEntry(network, String.format("%s network", network.getShortText()), index++);
        }

        {
            var pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            popupMenu.add(pnl);
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

                var item = addEntryAtEnd(NetworkInfo.create(httpUrl), value);
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

        var text = network.getId() == null ? network.getUrl().toString() : String.format("%s network", network.getShortText());
        var item = addEntryAtEnd(network, text);
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