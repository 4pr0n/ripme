package com.rarchives.ripme.ui;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClipboardUtils {

    private static final Logger LOGGER = Logger.getLogger(ClipboardUtils.class);
    private static AutoripThread autoripThread = new AutoripThread();

    private ClipboardUtils() {
    }

    public static void setClipboardAutoRip(boolean enabled) {
        if (enabled) {
            autoripThread.kill();
            autoripThread = new AutoripThread();
            autoripThread.isRunning = true;
            autoripThread.start();
        } else
            autoripThread.kill();
    }

    public static boolean getClipboardAutoRip() {
        return autoripThread.isRunning;
    }

    public static String getClipboardString() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (IllegalStateException e) {
            LOGGER.error("Caught and recovered from IllegalStateException: " + e.getMessage(), e);
        } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}

class AutoripThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(AutoripThread.class);
    protected volatile boolean isRunning = false;
    Set<String> rippedURLs = new HashSet<>();

    @Override
    public void run() {
        isRunning = true;
        try {
            while (isRunning) {
                // Check clipboard
                String clipboard = ClipboardUtils.getClipboardString();
                if (clipboard != null) {
                    Pattern p = Pattern.compile(
                            "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                                    "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
                                    "|mil|biz|info|mobi|name|aero|jobs|museum" +
                                    "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                                    "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                                    "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                                    "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                                    "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                                    "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                                    "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
                    Matcher m = p.matcher(clipboard);
                    while (m.find()) {
                        String url = m.group();
                        if (!rippedURLs.contains(url)) {
                            rippedURLs.add(url);
                            // TODO Queue rip instead of just starting it
                            MainWindow.ripAlbumStatic(url);
                        }
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    public void kill() {
        isRunning = false;
    }

}