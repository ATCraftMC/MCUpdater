package org.atcraftmc.updater.client.util;

import javax.swing.*;

public final class UIHandle<I extends UI<I>> {
    private final JFrame frame;
    private final I ui;

    public UIHandle(I ui) {
        this("MCUpdater", ui);
    }

    public UIHandle(String title, I ui) {
        this.frame = new JFrame();
        this.ui = ui;

        ui.setHandle(this);

        ui.build(this.frame);
        SwingUtil.center(this.frame);
        this.frame.setTitle(title);
        this.frame.setVisible(true);
        ui.setup(this);
    }

    public I ui() {
        return ui;
    }

    public JFrame frame() {
        return frame;
    }

    public void close() {
        this.frame().setVisible(false);
    }
}
