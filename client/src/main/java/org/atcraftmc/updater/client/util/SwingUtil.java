package org.atcraftmc.updater.client.util;

import javax.swing.*;
import java.awt.*;

public interface SwingUtil {

    static void center(JFrame frame) {
        var kit = Toolkit.getDefaultToolkit();
        var scr = kit.getScreenSize();

        var fw = frame.getWidth();
        var fh = frame.getHeight();
        var sw = scr.width;
        var sh = scr.height;

        frame.setLocation(sw / 2 - fw / 2, sh / 2 - fh / 2 - (int) (0.05 * sh));
    }
}
