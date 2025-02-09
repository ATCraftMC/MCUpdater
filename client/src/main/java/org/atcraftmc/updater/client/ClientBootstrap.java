package org.atcraftmc.updater.client;

import com.formdev.flatlaf.FlatDarkLaf;
import me.gb2022.commons.container.Vector;
import org.atcraftmc.updater.FilePath;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public interface ClientBootstrap {
    Vector<String> SERVICE = new Vector<>("5122921b-1a41-6358-f9e9-2cd7ef5fef60.ofalias.com:37044");

    //javaagent entrypoint
    static void premain(String agentArgs, Instrumentation inst) {
        boot(agentArgs);
    }

    static void main(String[] args) {
        boot(args[0]);
    }

    static void boot(String service) {
        var file = new File(FilePath.updater() + "/client-service.txt");

        if (file.exists() && file.length() > 0) {
            try (var in = new FileInputStream(file)) {
                service = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        SERVICE.set(service);
        FilePath.SERVER.set(service);

        theme();
        MCUpdaterClient.run();
    }

    static void notify(String title, String message) {
        if (!SystemTray.isSupported()) {
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        if (tray.getTrayIcons().length == 0) {
            var systemTray = SystemTray.getSystemTray();
            var popupMenu = new PopupMenu();
            var trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "Updater", popupMenu);

            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        TrayIcon trayIcon = tray.getTrayIcons()[0];
        trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
        tray.remove(trayIcon);
    }

    static void theme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());

            var properties = new Properties();

            properties.load(ClientBootstrap.class.getResourceAsStream("/theme.properties"));

            for (var key : properties.keySet()) {
                UIManager.put(key, Color.decode(properties.getProperty(key.toString())));
            }
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }
}
