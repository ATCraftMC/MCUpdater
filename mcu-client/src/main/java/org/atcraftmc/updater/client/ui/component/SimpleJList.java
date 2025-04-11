package org.atcraftmc.updater.client.ui.component;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class SimpleJList extends JPanel {
    private final List<JComponent> cells = new ArrayList<>();

    public SimpleJList() {
        setLayout(new GridBagLayout());
    }

    public void render() {
        removeAll();

        for (var i = 0; i < this.cells.size(); i++) {
            var gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            add(this.cells.get(i), gbc);
        }

        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = this.cells.size();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(new JPanel(), gbc);
    }

    public List<JComponent> getCells() {
        return cells;
    }

    public void add(JComponent component) {
        this.cells.add(component);
        render();
    }

    public void clear() {
        this.cells.clear();
        render();
    }
}
