package org.atcraftmc.updater.client.ui.framework;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class UIHandle<I extends UI<I>> {
    private final JFrame frame;
    private final I ui;
    private Runnable callback;

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

        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    public I ui() {
        return ui;
    }

    public JFrame frame() {
        return frame;
    }

    public void close() {
        this.frame.setVisible(false);
        this.frame.dispatchEvent(new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING));
    }

    public void setCloseCallback(Runnable cb) {
        this.callback = cb;
    }
}
