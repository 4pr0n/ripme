package com.rarchives.ripme.ui;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.utils.RipUtils;
import com.rarchives.ripme.utils.Utils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Everything UI-related starts and ends here.
 */
public class MainWindow implements Runnable, RipStatusHandler {

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class);

    private static final Color GREEN = Color.decode("#00897B");
    private static final Color RED = Color.decode("#F44336");
    private static final String CLIPBOARD_AUTORIP = "clipboard.autorip";
    private static final String ALBUM_TITLES_SAVE = "album_titles.save";
    private static final String URLS_ONLY_SAVE = "urls_only.save";
    private static final String LOG_SAVE = "log.save";
    private static final String DOWNLOAD_SAVE_ORDER = "download.save_order";
    private static final String AUTO_UPDATE = "auto.update";
    private static final String FILE_OVERWRITE = "file.overwrite";
    private static final String DESCRIPTIONS_SAVE = "descriptions.save";
    private static final String PREFER_MP4 = "prefer.mp4";
    private static final String RIPPER = "Ripper";
    private static final String HIDE = "Hide";
    private static final String SHOW = "Show";
    private static final String HISTORY_JSON = "history.json";

    private boolean isRipping = false; // Flag to indicate if we're ripping something

    private static JFrame mainFrame;
    private static JTextField ripTextfield;
    private static JButton ripButton;
    private JButton stopButton;

    private JLabel statusLabel;
    private JButton openButton;
    private JProgressBar statusProgress;

    // Log
    private JButton optionLog;
    private JPanel logPanel;
    private JTextPane logText;

    // History
    private JButton optionHistory;
    private static final History HISTORY = new History();
    private JPanel historyPanel;
    private JTable historyTable;
    private AbstractTableModel historyTableModel;
    private JButton historyButtonRemove;
    private JButton historyButtonClear;
    private JButton historyButtonRerip;

    // Queue
    static JButton optionQueue;
    private JPanel queuePanel;
    private DefaultListModel queueListModel;

    // Configuration
    private JButton optionConfiguration;
    private JPanel configurationPanel;
    private JButton configUpdateButton;
    private JLabel configUpdateLabel;
    private JTextField configTimeoutText;
    private JTextField configThreadsText;
    private JCheckBox configOverwriteCheckbox;
    private JLabel configSaveDirLabel;
    private JButton configSaveDirButton;
    private JTextField configRetriesText;
    private JCheckBox configAutoupdateCheckbox;
    private JComboBox configLogLevelCombobox;
    private JCheckBox configPlaySound;
    private JCheckBox configSaveOrderCheckbox;
    private JCheckBox configShowPopup;
    private JCheckBox configSaveLogs;
    private JCheckBox configSaveURLsOnly;
    private JCheckBox configSaveAlbumTitles;
    private JCheckBox configClipboardAutorip;
    private JCheckBox configSaveDescriptions;
    private JCheckBox configPreferMp4;

    private static TrayIcon trayIcon;
    private static MenuItem trayMenuMain;
    private static CheckboxMenuItem trayMenuAutorip;

    private static Image mainIcon;

    private static AbstractRipper ripper;

    public MainWindow() {
        mainFrame = new JFrame("RipMe v" + UpdateUtils.getThisJarVersion());
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setLayout(new GridBagLayout());

        createUI(mainFrame.getContentPane());
        loadHistory();
        setupHandlers();

        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                shutdownCleanup();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        if (Utils.getConfigBoolean(AUTO_UPDATE, true)) {
            upgradeProgram();
        }

        boolean autoripEnabled = Utils.getConfigBoolean(CLIPBOARD_AUTORIP, false);
        ClipboardUtils.setClipboardAutoRip(autoripEnabled);
        trayMenuAutorip.setState(autoripEnabled);
    }

    private void upgradeProgram() {
        if (!configurationPanel.isVisible())
            optionConfiguration.doClick();

        Runnable r = () -> UpdateUtils.updateProgram(configUpdateLabel);
        new Thread(r).start();
    }

    @Override
    public void run() {
        pack();
        mainFrame.setIconImage(getImageIcon("icon").getImage());
        restoreWindowPosition(mainFrame);
        mainFrame.setVisible(true);
    }

    private void shutdownCleanup() {
        Utils.setConfigBoolean(FILE_OVERWRITE, configOverwriteCheckbox.isSelected());
        Utils.setConfigInteger("threads.size", Integer.parseInt(configThreadsText.getText()));
        Utils.setConfigInteger("download.retries", Integer.parseInt(configRetriesText.getText()));
        Utils.setConfigInteger("download.timeout", Integer.parseInt(configTimeoutText.getText()));
        Utils.setConfigBoolean(CLIPBOARD_AUTORIP, ClipboardUtils.getClipboardAutoRip());
        Utils.setConfigBoolean(AUTO_UPDATE, configAutoupdateCheckbox.isSelected());
        Utils.setConfigString("log.level", configLogLevelCombobox.getSelectedItem().toString());
        Utils.setConfigBoolean("play.sound", configPlaySound.isSelected());
        Utils.setConfigBoolean(DOWNLOAD_SAVE_ORDER, configSaveOrderCheckbox.isSelected());
        Utils.setConfigBoolean("download.show_popup", configShowPopup.isSelected());
        Utils.setConfigBoolean(LOG_SAVE, configSaveLogs.isSelected());
        Utils.setConfigBoolean(URLS_ONLY_SAVE, configSaveURLsOnly.isSelected());
        Utils.setConfigBoolean(ALBUM_TITLES_SAVE, configSaveAlbumTitles.isSelected());
        Utils.setConfigBoolean(CLIPBOARD_AUTORIP, configClipboardAutorip.isSelected());
        Utils.setConfigBoolean(DESCRIPTIONS_SAVE, configSaveDescriptions.isSelected());
        Utils.setConfigBoolean(PREFER_MP4, configPreferMp4.isSelected());
        saveWindowPosition(mainFrame);
        saveHistory();
        Utils.saveConfig();
    }

    private void status(String text) {
        statusWithColor(text, Color.BLACK);
    }

    private void error(String text) {
        statusWithColor(text, RED);
    }

    private void statusWithColor(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
        pack();
    }

    private void pack() {
        SwingUtilities.invokeLater(mainFrame::pack);
    }

    private void createUI(Container pane) {
        setupTrayIcon();

        EmptyBorder emptyBorder = new EmptyBorder(5, 5, 5, 5);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        gbc.weightx = 2;
        gbc.ipadx = 2;
        gbc.gridx = 0;

        gbc.weighty = 2;
        gbc.ipady = 2;
        gbc.gridy = 0;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.error("[!] Exception setting system theme:", e);
        }

        ripTextfield = new JTextField("", 20);
        ripTextfield.addMouseListener(new ContextMenuMouseListener());
        ImageIcon ripIcon = new ImageIcon(mainIcon);
        ripButton = new JButton("<html><font size=\"5\"><b>Rip</b></font></html>", ripIcon);

        stopButton = new JButton("<html><font size=\"5\"><b>Stop</b></font></html>");
        stopButton.setEnabled(false);
        stopButton.setIcon(getImageIcon("stop"));

        JPanel ripPanel = new JPanel(new GridBagLayout());
        ripPanel.setBorder(emptyBorder);

        gbc.gridx = 0;
        ripPanel.add(new JLabel("URL:", JLabel.RIGHT), gbc);

        gbc.gridx = 1;
        ripPanel.add(ripTextfield, gbc);

        gbc.gridx = 2;
        ripPanel.add(ripButton, gbc);

        gbc.gridx = 3;
        ripPanel.add(stopButton, gbc);

        statusLabel = new JLabel("Inactive");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        openButton = new JButton();
        openButton.setVisible(false);
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setBorder(emptyBorder);

        gbc.gridx = 0;
        statusPanel.add(statusLabel, gbc);

        gbc.gridy = 1;
        statusPanel.add(openButton, gbc);
        gbc.gridy = 0;

        JPanel progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setBorder(emptyBorder);
        statusProgress = new JProgressBar(0, 100);
        progressPanel.add(statusProgress, gbc);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(emptyBorder);
        optionLog = new JButton("Log");
        optionHistory = new JButton("History");
        optionQueue = new JButton("Queue");
        optionConfiguration = new JButton("Configuration");

        optionLog.setIcon(getImageIcon("comment"));
        optionHistory.setIcon(getImageIcon("time"));
        optionQueue.setIcon(getImageIcon("list"));
        optionConfiguration.setIcon(getImageIcon("gear"));

        gbc.gridx = 0;
        optionsPanel.add(optionLog, gbc);
        gbc.gridx = 1;
        optionsPanel.add(optionHistory, gbc);
        gbc.gridx = 2;
        optionsPanel.add(optionQueue, gbc);
        gbc.gridx = 3;
        optionsPanel.add(optionConfiguration, gbc);

        logText = new JTextPaneNoWrap();
        JScrollPane logTextScroll = new JScrollPane(logText);

        logPanel = new JPanel(new GridBagLayout());
        logPanel.setBorder(emptyBorder);
        logText = new JTextPaneNoWrap();
        logTextScroll = new JScrollPane(logText);
        logPanel.setVisible(false);
        logPanel.setPreferredSize(new Dimension(300, 250));
        logPanel.add(logTextScroll, gbc);

        historyPanel = new JPanel(new GridBagLayout());
        historyPanel.setBorder(emptyBorder);
        historyPanel.setVisible(false);
        historyPanel.setPreferredSize(new Dimension(300, 250));
        historyTableModel = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getColumnName(int col) {
                return HISTORY.getColumnName(col);
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            @Override
            public Object getValueAt(int row, int col) {
                return HISTORY.getValueAt(row, col);
            }

            @Override
            public int getRowCount() {
                return HISTORY.toList().size();
            }

            @Override
            public int getColumnCount() {
                return HISTORY.getColumnCount();
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 4;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                if (col == 4) {
                    HISTORY.get(row).selected = (Boolean) value;
                    historyTableModel.fireTableDataChanged();
                }
            }
        };
        historyTable = new JTable(historyTableModel);
        historyTable.addMouseListener(new HistoryMenuMouseListener());
        historyTable.setAutoCreateRowSorter(true);

        for (int i = 0; i < historyTable.getColumnModel().getColumnCount(); i++) {
            int width = 130; // Default
            switch (i) {
                case 0: // URL
                    width = 270;
                    break;
                case 3:
                    width = 40;
                    break;
                case 4:
                    width = 15;
                    break;
            }
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        JScrollPane historyTableScrollPane = new JScrollPane(historyTable);
        historyButtonRemove = new JButton("Remove");
        historyButtonClear = new JButton("Clear");
        historyButtonRerip = new JButton("Re-rip Checked");
        gbc.gridx = 0;

        // History List Panel
        JPanel historyTablePanel = new JPanel(new GridBagLayout());
        historyTablePanel.add(historyTableScrollPane, gbc);
        gbc.ipady = 180;
        historyPanel.add(historyTablePanel, gbc);
        gbc.ipady = 0;

        JPanel historyButtonPanel = new JPanel(new GridBagLayout());
        historyButtonPanel.setPreferredSize(new Dimension(300, 10));
        historyButtonPanel.setBorder(emptyBorder);

        gbc.gridx = 0;
        historyButtonPanel.add(historyButtonRemove, gbc);

        gbc.gridx = 1;
        historyButtonPanel.add(historyButtonClear, gbc);

        gbc.gridx = 2;
        historyButtonPanel.add(historyButtonRerip, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        historyPanel.add(historyButtonPanel, gbc);

        queuePanel = new JPanel(new GridBagLayout());
        queuePanel.setBorder(emptyBorder);
        queuePanel.setVisible(false);
        queuePanel.setPreferredSize(new Dimension(300, 250));
        queueListModel = new DefaultListModel();

        JList queueList = new JList(queueListModel);
        queueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        queueList.addMouseListener(new QueueMenuMouseListener());

        JScrollPane queueListScroll = new JScrollPane(queueList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        for (String item : Utils.getConfigList("queue")) {
            queueListModel.addElement(item);
        }

        if (!queueListModel.isEmpty())
            optionQueue.setText("Queue (" + queueListModel.size() + ")");
        else
            optionQueue.setText("Queue");

        gbc.gridx = 0;
        JPanel queueListPanel = new JPanel(new GridBagLayout());
        queueListPanel.add(queueListScroll, gbc);
        queuePanel.add(queueListPanel, gbc);
        gbc.ipady = 0;

        configurationPanel = new JPanel(new GridBagLayout());
        configurationPanel.setBorder(emptyBorder);
        configurationPanel.setVisible(false);
        configurationPanel.setPreferredSize(new Dimension(300, 250));
        // TODO Configuration components
        configUpdateButton = new JButton("Check for updates");
        configUpdateLabel = new JLabel("Current version: " + UpdateUtils.getThisJarVersion(), JLabel.RIGHT);

        JLabel configThreadsLabel = new JLabel("Maximum download threads:", JLabel.RIGHT);
        configThreadsLabel.setLabelFor(configThreadsText);

        JLabel configTimeoutLabel = new JLabel("Timeout (in milliseconds):", JLabel.RIGHT);
        configTimeoutLabel.setLabelFor(configTimeoutText);

        JLabel configRetriesLabel = new JLabel("Retry download count:", JLabel.RIGHT);
        configRetriesLabel.setLabelFor(configRetriesText);

        configThreadsText = new JTextField(Integer.toString(Utils.getConfigInteger("threads.size", 3)));
        configTimeoutText = new JTextField(Integer.toString(Utils.getConfigInteger("download.timeout", 60000)));
        configRetriesText = new JTextField(Integer.toString(Utils.getConfigInteger("download.retries", 3)));

        configOverwriteCheckbox = new JCheckBox("Overwrite existing files?", Utils.getConfigBoolean(FILE_OVERWRITE, false));
        configOverwriteCheckbox.setHorizontalAlignment(JCheckBox.RIGHT);
        configOverwriteCheckbox.setHorizontalTextPosition(JCheckBox.LEFT);

        configAutoupdateCheckbox = new JCheckBox("Auto-update?", Utils.getConfigBoolean(AUTO_UPDATE, true));
        configAutoupdateCheckbox.setHorizontalAlignment(JCheckBox.RIGHT);
        configAutoupdateCheckbox.setHorizontalTextPosition(JCheckBox.LEFT);

        configLogLevelCombobox = new JComboBox(new String[]{"Log level: Error", "Log level: Warn", "Log level: Info", "Log level: Debug"});
        configLogLevelCombobox.setSelectedItem(Utils.getConfigString("log.level", "Log level: Debug"));
        setLogLevel(configLogLevelCombobox.getSelectedItem().toString());

        configPlaySound = new JCheckBox("Sound when rip completes", Utils.getConfigBoolean("play.sound", false));
        configPlaySound.setHorizontalAlignment(JCheckBox.RIGHT);
        configPlaySound.setHorizontalTextPosition(JCheckBox.LEFT);

        configSaveOrderCheckbox = new JCheckBox("Preserve order", Utils.getConfigBoolean(DOWNLOAD_SAVE_ORDER, true));
        configSaveOrderCheckbox.setHorizontalAlignment(JCheckBox.RIGHT);
        configSaveOrderCheckbox.setHorizontalTextPosition(JCheckBox.LEFT);

        configShowPopup = new JCheckBox("Notification when rip starts", Utils.getConfigBoolean("download.show_popup", false));
        configShowPopup.setHorizontalAlignment(JCheckBox.RIGHT);
        configShowPopup.setHorizontalTextPosition(JCheckBox.LEFT);

        configSaveLogs = new JCheckBox("Save logs", Utils.getConfigBoolean(LOG_SAVE, false));
        configSaveLogs.setHorizontalAlignment(JCheckBox.RIGHT);
        configSaveLogs.setHorizontalTextPosition(JCheckBox.LEFT);

        configSaveURLsOnly = new JCheckBox("Save URLs only", Utils.getConfigBoolean(URLS_ONLY_SAVE, false));
        configSaveURLsOnly.setHorizontalAlignment(JCheckBox.RIGHT);
        configSaveURLsOnly.setHorizontalTextPosition(JCheckBox.LEFT);

        configSaveAlbumTitles = new JCheckBox("Save album titles", Utils.getConfigBoolean(ALBUM_TITLES_SAVE, true));
        configSaveAlbumTitles.setHorizontalAlignment(JCheckBox.RIGHT);
        configSaveAlbumTitles.setHorizontalTextPosition(JCheckBox.LEFT);

        configClipboardAutorip = new JCheckBox("Autorip from Clipboard", Utils.getConfigBoolean(CLIPBOARD_AUTORIP, false));
        configClipboardAutorip.setHorizontalAlignment(JCheckBox.RIGHT);
        configClipboardAutorip.setHorizontalTextPosition(JCheckBox.LEFT);

        configSaveDescriptions = new JCheckBox("Save descriptions", Utils.getConfigBoolean(DESCRIPTIONS_SAVE, true));
        configSaveDescriptions.setHorizontalAlignment(JCheckBox.RIGHT);
        configSaveDescriptions.setHorizontalTextPosition(JCheckBox.LEFT);

        configPreferMp4 = new JCheckBox("Prefer MP4 over GIF", Utils.getConfigBoolean(PREFER_MP4, false));
        configPreferMp4.setHorizontalAlignment(JCheckBox.RIGHT);
        configPreferMp4.setHorizontalTextPosition(JCheckBox.LEFT);

        configWindowPosition = new JCheckBox("Restore window position", Utils.getConfigBoolean("window.position", true));
        configWindowPosition.setHorizontalAlignment(JCheckBox.RIGHT);
        configWindowPosition.setHorizontalTextPosition(JCheckBox.LEFT);
        configSaveDirLabel = new JLabel();
        configSaveDirLabel.setHorizontalAlignment(JLabel.RIGHT);
        try {
            String workingDir = Utils.shortenPath(Utils.getWorkingDirectory());
            configSaveDirLabel.setText(workingDir);
            configSaveDirLabel.setForeground(Color.BLUE);
            configSaveDirLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        configSaveDirLabel.setToolTipText(configSaveDirLabel.getText());

        configSaveDirButton = new JButton("Select Save Directory...");
        gbc.gridy = 0;
        gbc.gridx = 0;
        configurationPanel.add(configUpdateLabel, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configUpdateButton, gbc);
        gbc.gridy = 1;
        gbc.gridx = 0;
        configurationPanel.add(configAutoupdateCheckbox, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configLogLevelCombobox, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        configurationPanel.add(configThreadsLabel, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configThreadsText, gbc);
        gbc.gridy = 3;
        gbc.gridx = 0;
        configurationPanel.add(configTimeoutLabel, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configTimeoutText, gbc);
        gbc.gridy = 4;
        gbc.gridx = 0;
        configurationPanel.add(configRetriesLabel, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configRetriesText, gbc);
        gbc.gridy = 5;
        gbc.gridx = 0;
        configurationPanel.add(configOverwriteCheckbox, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configSaveOrderCheckbox, gbc);
        gbc.gridy = 6;
        gbc.gridx = 0;
        configurationPanel.add(configPlaySound, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configSaveLogs, gbc);
        gbc.gridy = 7;
        gbc.gridx = 0;
        configurationPanel.add(configShowPopup, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configSaveURLsOnly, gbc);
        gbc.gridy = 8;
        gbc.gridx = 0;
        configurationPanel.add(configClipboardAutorip, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configSaveAlbumTitles, gbc);
        gbc.gridy = 9;
        gbc.gridx = 0;
        configurationPanel.add(configSaveDescriptions, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configPreferMp4, gbc);
        gbc.gridy = 10;
        gbc.gridx = 0;
        configurationPanel.add(configSaveDirLabel, gbc);
        gbc.gridx = 1;
        configurationPanel.add(configSaveDirButton, gbc);

        gbc.gridy = 0;
        pane.add(ripPanel, gbc);
        gbc.gridy = 1;
        pane.add(statusPanel, gbc);
        gbc.gridy = 2;
        pane.add(progressPanel, gbc);
        gbc.gridy = 3;
        pane.add(optionsPanel, gbc);
        gbc.gridy = 4;
        pane.add(logPanel, gbc);
        gbc.gridy = 5;
        pane.add(historyPanel, gbc);
        gbc.gridy = 5;
        pane.add(queuePanel, gbc);
        gbc.gridy = 5;
        pane.add(configurationPanel, gbc);
    }
    
    private void setupHandlers() {
        ripButton.addActionListener(new RipButtonHandler());
        ripTextfield.addActionListener(new RipButtonHandler());
        ripTextfield.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                try {
                    String urlText = ripTextfield.getText().trim();
                    if (urlText.trim().isEmpty())
                        return;

                    if (!urlText.startsWith("http"))
                        urlText = "http://" + urlText;

                    URL url = new URL(urlText);
                    AbstractRipper ripper = AbstractRipper.getRipper(url);
                    statusWithColor(ripper.getHost() + " album detected", GREEN);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    statusWithColor("Can't rip this URL: " + e.getMessage(), RED);
                }
            }
        });

        stopButton.addActionListener(event -> {
            if (ripper != null) {
                ripper.stop();
                isRipping = false;
                stopButton.setEnabled(false);
                statusProgress.setValue(0);
                statusProgress.setVisible(false);
                pack();
                statusProgress.setValue(0);
                status("Ripping interrupted");
                appendLog("Ripper interrupted", RED);
            }
        });

        optionLog.addActionListener(event -> {
            logPanel.setVisible(!logPanel.isVisible());
            historyPanel.setVisible(false);
            queuePanel.setVisible(false);
            configurationPanel.setVisible(false);
            optionLog.setFont(optionLog.getFont().deriveFont(Font.BOLD));
            optionHistory.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionQueue.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionConfiguration.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            pack();
        });

        optionHistory.addActionListener(event -> {
            logPanel.setVisible(false);
            historyPanel.setVisible(!historyPanel.isVisible());
            queuePanel.setVisible(false);
            configurationPanel.setVisible(false);
            optionLog.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionHistory.setFont(optionLog.getFont().deriveFont(Font.BOLD));
            optionQueue.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionConfiguration.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            pack();
        });

        optionQueue.addActionListener(event -> {
            logPanel.setVisible(false);
            historyPanel.setVisible(false);
            queuePanel.setVisible(!queuePanel.isVisible());
            configurationPanel.setVisible(false);
            optionLog.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionHistory.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionQueue.setFont(optionLog.getFont().deriveFont(Font.BOLD));
            optionConfiguration.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            pack();
        });

        optionConfiguration.addActionListener(event -> {
            logPanel.setVisible(false);
            historyPanel.setVisible(false);
            queuePanel.setVisible(false);
            configurationPanel.setVisible(!configurationPanel.isVisible());
            optionLog.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionHistory.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionQueue.setFont(optionLog.getFont().deriveFont(Font.PLAIN));
            optionConfiguration.setFont(optionLog.getFont().deriveFont(Font.BOLD));
            pack();
        });

        historyButtonRemove.addActionListener(event -> {
            int[] indices = historyTable.getSelectedRows();

            for (int i = indices.length - 1; i >= 0; i--) {
                int modelIndex = historyTable.convertRowIndexToModel(indices[i]);
                HISTORY.remove(modelIndex);
            }

            try {
                historyTableModel.fireTableDataChanged();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            saveHistory();
        });

        historyButtonClear.addActionListener(event -> {
            HISTORY.clear();
            try {
                historyTableModel.fireTableDataChanged();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            saveHistory();
        });
        
        // Re-rip all history
        historyButtonRerip.addActionListener(event -> {
            if (HISTORY.toList().isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "There are no history entries to re-rip. Rip some albums first",
                        "RipMe Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int added = 0;
            for (HistoryEntry entry : HISTORY.toList()) {
                if (entry.selected) {
                    added++;
                    queueListModel.addElement(entry.url);
                }
            }

            if (added == 0) {
                JOptionPane.showMessageDialog(mainFrame,
                        "No history entries have been 'Checked'\n" +
                                "Check an entry by clicking the checkbox to the right of the URL or Right-click a URL to check/uncheck all items",
                        "RipMe Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        configUpdateButton.addActionListener(arg0 -> {
            Thread t = new Thread() {
                @Override
                public void run() {
                    UpdateUtils.updateProgram(configUpdateLabel);
                }
            };
            t.start();
        });

        configLogLevelCombobox.addActionListener(arg0 -> {
            String level = ((JComboBox) arg0.getSource()).getSelectedItem().toString();
            setLogLevel(level);
        });

        configSaveDirButton.addActionListener(arg0 -> {
            JFileChooser jfc = new JFileChooser(Utils.getWorkingDirectory());
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = jfc.showDialog(null, "select directory");

            if (returnVal != JFileChooser.APPROVE_OPTION)
                return;

            File chosenFile = jfc.getSelectedFile();
            String chosenPath = null;

            try {
                chosenPath = chosenFile.getCanonicalPath();
            } catch (Exception e) {
                LOGGER.error("Error while getting selected path: ", e);
                return;
            }

            configSaveDirLabel.setText(Utils.shortenPath(chosenPath));
            Utils.setConfigString("rips.directory", chosenPath);

            configSaveDirLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                File file = new File(Utils.getWorkingDirectory().toString());
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.open(file);
                } catch (Exception e1) { }
            }
        });
        configSaveDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser jfc = new JFileChooser(Utils.getWorkingDirectory());
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = jfc.showDialog(null, "select directory");
                if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File chosenFile = jfc.getSelectedFile();
                String chosenPath = null;
                try {
                    chosenPath = chosenFile.getCanonicalPath();
                } catch (Exception e) {
                    logger.error("Error while getting selected path: ", e);
                    return;
                }
                configSaveDirLabel.setText(Utils.shortenPath(chosenPath));
                Utils.setConfigString("rips.directory", chosenPath);
            }
        });

        configOverwriteCheckbox.addActionListener(arg0 -> Utils.setConfigBoolean(FILE_OVERWRITE, configOverwriteCheckbox.isSelected()));
        configSaveOrderCheckbox.addActionListener(arg0 -> Utils.setConfigBoolean(DOWNLOAD_SAVE_ORDER, configSaveOrderCheckbox.isSelected()));

        configSaveLogs.addActionListener(arg0 -> {
            Utils.setConfigBoolean(LOG_SAVE, configSaveLogs.isSelected());
            Utils.configureLogger();
        });

        configSaveURLsOnly.addActionListener(arg0 -> {
            Utils.setConfigBoolean(URLS_ONLY_SAVE, configSaveURLsOnly.isSelected());
            Utils.configureLogger();
        });

        configSaveAlbumTitles.addActionListener(arg0 -> {
            Utils.setConfigBoolean(ALBUM_TITLES_SAVE, configSaveAlbumTitles.isSelected());
            Utils.configureLogger();
        });

        configClipboardAutorip.addActionListener(arg0 -> {
            Utils.setConfigBoolean(CLIPBOARD_AUTORIP, configClipboardAutorip.isSelected());
            ClipboardUtils.setClipboardAutoRip(configClipboardAutorip.isSelected());
            trayMenuAutorip.setState(configClipboardAutorip.isSelected());
            Utils.configureLogger();
        });

        configSaveDescriptions.addActionListener(arg0 -> {
            Utils.setConfigBoolean(DESCRIPTIONS_SAVE, configSaveDescriptions.isSelected());
            Utils.configureLogger();
        });

        configPreferMp4.addActionListener(arg0 -> {
            Utils.setConfigBoolean(PREFER_MP4, configPreferMp4.isSelected());
            Utils.configureLogger();
        });

            configWindowPosition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Utils.setConfigBoolean("window.position", configWindowPosition.isSelected());
                Utils.configureLogger();
            }
        });

        queueListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent arg0) {
                if (!queueListModel.isEmpty())
                    optionQueue.setText("Queue (" + queueListModel.size() + ")");
                else
                    optionQueue.setText("Queue");

                if (!isRipping)
                    ripNextAlbum();
            }

            @Override
            public void contentsChanged(ListDataEvent arg0) {
            }

            @Override
            public void intervalRemoved(ListDataEvent arg0) {
            }
        });
    }

    private void setLogLevel(String level) {
        Level newLevel = Level.ERROR;
        level = level.substring(level.lastIndexOf(' ') + 1);

        switch (level) {
            case "Debug":
                newLevel = Level.DEBUG;
                break;
            case "Info":
                newLevel = Level.INFO;
                break;
            case "Warn":
                newLevel = Level.WARN;
                break;
            case "Error":
                newLevel = Level.ERROR;
                break;
        }

        Logger.getRootLogger().setLevel(newLevel);
        LOGGER.setLevel(newLevel);
        ConsoleAppender ca = (ConsoleAppender) Logger.getRootLogger().getAppender("stdout");

        if (ca != null)
            ca.setThreshold(newLevel);

        FileAppender fa = (FileAppender) Logger.getRootLogger().getAppender("FILE");
        if (fa != null)
            fa.setThreshold(newLevel);

    }

    private void setupTrayIcon() {
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                trayMenuMain.setLabel(HIDE);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                trayMenuMain.setLabel(SHOW);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                trayMenuMain.setLabel(HIDE);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                trayMenuMain.setLabel(SHOW);
            }
        });

        PopupMenu trayMenu = new PopupMenu();
        trayMenuMain = new MenuItem(HIDE);
        trayMenuMain.addActionListener(arg0 -> toggleTrayClick());

        MenuItem trayMenuAbout = new MenuItem("About " + mainFrame.getTitle());
        trayMenuAbout.addActionListener(arg0 -> {
            StringBuilder about = new StringBuilder();
            about.append("<html><h1>").append(mainFrame.getTitle()).append("</h1>");
            about.append("Download albums from various websites:");

            try {
                List<String> rippers = Utils.getListOfAlbumRippers();
                about.append("<ul>");

                for (String ripper : rippers) {
                    about.append("<li>");
                    ripper = ripper.substring(ripper.lastIndexOf('.') + 1);

                    if (ripper.contains(RIPPER))
                        ripper = ripper.substring(0, ripper.indexOf(RIPPER));

                    about.append(ripper);
                    about.append("</li>");
                }

                about.append("</ul>");
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            about.append("<br>And download videos from video sites:");
            try {
                List<String> rippers = Utils.getListOfVideoRippers();
                about.append("<ul>");

                for (String ripper : rippers) {
                    about.append("<li>");
                    ripper = ripper.substring(ripper.lastIndexOf('.') + 1);

                    if (ripper.contains(RIPPER))
                        ripper = ripper.substring(0, ripper.indexOf(RIPPER));

                    about.append(ripper);
                    about.append("</li>");
                }

                about.append("</ul>");
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            about.append("Do you want to visit the project homepage on Github?");
            about.append("</html>");
            int response = JOptionPane.showConfirmDialog(null, about.toString(), mainFrame.getTitle(),
                    JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon(mainIcon));

            if (response == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().browse(URI.create("http://github.com/4pr0n/ripme"));
                } catch (IOException e) {
                    LOGGER.error("Exception while opening project home page", e);
                }
            }
        });

        MenuItem trayMenuExit = new MenuItem("Exit");
        trayMenuExit.addActionListener(arg0 -> System.exit(0));
        trayMenuAutorip = new CheckboxMenuItem("Clipboard Autorip");
        trayMenuAutorip.addItemListener(arg0 -> {
            ClipboardUtils.setClipboardAutoRip(trayMenuAutorip.getState());
            configClipboardAutorip.setSelected(trayMenuAutorip.getState());
        });

        trayMenu.add(trayMenuMain);
        trayMenu.add(trayMenuAbout);
        trayMenu.addSeparator();
        trayMenu.add(trayMenuAutorip);
        trayMenu.addSeparator();
        trayMenu.add(trayMenuExit);

        try {
            mainIcon = getImageIcon("icon").getImage();
            trayIcon = new TrayIcon(mainIcon);
            trayIcon.setToolTip(mainFrame.getTitle());
            trayIcon.setImageAutoSize(true);
            trayIcon.setPopupMenu(trayMenu);
            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleTrayClick();
                    if (mainFrame.getExtendedState() != JFrame.NORMAL) {
                        mainFrame.setExtendedState(JFrame.NORMAL);
                    }
                    mainFrame.setAlwaysOnTop(true);
                    mainFrame.setAlwaysOnTop(false);
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void toggleTrayClick() {
        if (mainFrame.getExtendedState() == JFrame.ICONIFIED || !mainFrame.isActive() || !mainFrame.isVisible()) {
            mainFrame.setVisible(true);
            mainFrame.setAlwaysOnTop(true);
            mainFrame.setAlwaysOnTop(false);
            trayMenuMain.setLabel(HIDE);
        } else {
            mainFrame.setVisible(false);
            trayMenuMain.setLabel(SHOW);
        }
    }
    
    private void appendLog(final String text, final Color color) {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setForeground(sas, color);
        StyledDocument sd = logText.getStyledDocument();

        try {
            synchronized (this) {
                sd.insertString(sd.getLength(), text + "\n", sas);
            }
        } catch (BadLocationException e) {
            LOGGER.error(e.getMessage(), e);
        }

        logText.setCaretPosition(sd.getLength());
    }

    private void loadHistory() {
        File historyFile = new File(HISTORY_JSON);
        HISTORY.clear();

        if (historyFile.exists()) {
            try {
                LOGGER.info("Loading history from history.json");
                HISTORY.fromFile(HISTORY_JSON);
            } catch (IOException e) {
                LOGGER.error("Failed to load history from file " + historyFile, e);
                JOptionPane.showMessageDialog(null,
                        "RipMe failed to load the history file at " + historyFile.getAbsolutePath() + "\n\n" +
                                "Error: " + e.getMessage() + "\n\n" +
                                "Closing RipMe will automatically overwrite the contents of this file,\n" +
                                "so you may want to back the file up before closing RipMe!",
                        "RipMe - history load failure",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            LOGGER.info("Loading history from configuration");
            HISTORY.fromList(Utils.getConfigList("download.history"));
            if (HISTORY.toList().isEmpty()) {
                // Loaded from config, still no entries.
                // Guess rip history based on rip folder
                String[] dirs = Utils.getWorkingDirectory()
                        .list((dir, file) -> new File(dir.getAbsolutePath() + File.separator + file).isDirectory());

                for (String dir : dirs) {
                    String url = RipUtils.urlFromDirectoryName(dir);
                    if (url != null) {
                        // We found one, add it to history
                        HistoryEntry entry = new HistoryEntry();
                        entry.url = url;
                        HISTORY.add(entry);
                    }
                }
            }
        }
    }

    private void saveHistory() {
        try {
            HISTORY.toFile(HISTORY_JSON);
            Utils.setConfigList("download.history", Collections.emptyList());
        } catch (IOException e) {
            LOGGER.error("Failed to save history to file history.json", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void ripNextAlbum() {
        isRipping = true;

        // Save current state of queue to configuration.
        Utils.setConfigList("queue", (Enumeration<Object>) queueListModel.elements());

        if (queueListModel.isEmpty()) {
            // End of queue
            isRipping = false;
            return;
        }

        String nextAlbum = (String) queueListModel.remove(0);
        if (queueListModel.isEmpty()) {
            optionQueue.setText("Queue");
        } else {
            optionQueue.setText("Queue (" + queueListModel.size() + ")");
        }

        Thread t = ripAlbum(nextAlbum);
        if (t == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                LOGGER.error("Interrupted while waiting to rip next album", ie);
            }
            ripNextAlbum();
        } else {
            t.start();
        }
    }

    private Thread ripAlbum(String urlString) {
        //shutdownCleanup();
        if (!logPanel.isVisible())
            optionLog.doClick();

        urlString = urlString.trim();
        if (urlString.toLowerCase().startsWith("gonewild:"))
            urlString = "http://gonewild.com/user/" + urlString.substring(urlString.indexOf(':') + 1);

        if (!urlString.startsWith("http"))
            urlString = "http://" + urlString;

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOGGER.error("[!] Could not generate URL for '" + urlString + "'", e);
            error("Given URL is not valid, expecting http://website.com/page/...");
            return null;
        }

        stopButton.setEnabled(true);
        statusProgress.setValue(100);
        openButton.setVisible(false);
        statusLabel.setVisible(true);
        pack();
        boolean failed = false;

        try {
            ripper = AbstractRipper.getRipper(url);
            ripper.setup();
        } catch (Exception e) {
            failed = true;
            LOGGER.error("Could not find ripper for URL " + url, e);
            error(e.getMessage());
        }

        if (!failed) try {
            mainFrame.setTitle("Ripping - RipMe v" + UpdateUtils.getThisJarVersion());
            status("Starting rip...");
            ripper.setObserver(this);
            Thread t = new Thread(ripper);

            if (configShowPopup.isSelected() && (!mainFrame.isVisible() || !mainFrame.isActive())) {
                mainFrame.toFront();
                mainFrame.setAlwaysOnTop(true);
                trayIcon.displayMessage(mainFrame.getTitle(), "Started ripping " + ripper.getURL().toExternalForm(), MessageType.INFO);
                mainFrame.setAlwaysOnTop(false);
            }
            return t;
        } catch (Exception e) {
            LOGGER.error("[!] Error while ripping: " + e.getMessage(), e);
            error("Unable to rip this URL: " + e.getMessage());
        }

        stopButton.setEnabled(false);
        statusProgress.setValue(0);
        pack();
        return null;
    }

    class RipButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (!queueListModel.contains(ripTextfield.getText()) && !ripTextfield.getText().isEmpty()) {
                queueListModel.add(queueListModel.size(), ripTextfield.getText());
                ripTextfield.setText("");
            } else if (!isRipping)
                ripNextAlbum();
        }
    }
    
    private class StatusEvent implements Runnable {
        private final AbstractRipper ripper;
        private final RipStatusMessage msg;

        public StatusEvent(AbstractRipper ripper, RipStatusMessage msg) {
            this.ripper = ripper;
            this.msg = msg;
        }

        @Override
        public void run() {
            handleEvent(this);
        }
    }
    
    private synchronized void handleEvent(StatusEvent evt) {
        if (ripper.isStopped())
            return;

        RipStatusMessage msg = evt.msg;

        int completedPercent = evt.ripper.getCompletionPercentage();
        statusProgress.setValue(completedPercent);
        statusProgress.setVisible(true);
        status(evt.ripper.getStatusText());

        switch (msg.getStatus()) {
            case LOADING_RESOURCE:
            case DOWNLOAD_STARTED:
                if (LOGGER.isEnabledFor(Level.INFO))
                    appendLog("Downloading " + msg.getObject(), Color.BLACK);

                break;
            case DOWNLOAD_COMPLETE:
                appendLog("Downloaded " + msg.getObject(), GREEN);
                break;
            case DOWNLOAD_ERRORED:
                if (LOGGER.isEnabledFor(Level.ERROR))
                    appendLog((String) msg.getObject(), RED);

                break;
            case DOWNLOAD_WARN:
                appendLog((String) msg.getObject(), Color.ORANGE);
                break;
            case RIP_ERRORED:
                if (LOGGER.isEnabledFor(Level.ERROR))
                    appendLog((String) msg.getObject(), RED);

                stopButton.setEnabled(false);
                statusProgress.setValue(0);
                statusProgress.setVisible(false);
                openButton.setVisible(false);
                pack();
                statusWithColor("Error: " + msg.getObject(), RED);
                break;

            case RIP_COMPLETE:
                RipStatusComplete rsc = (RipStatusComplete) msg.getObject();
                String url = ripper.getURL().toExternalForm();
                if (HISTORY.containsURL(url)) {
                    // TODO update "modifiedDate" of entry in HISTORY
                    HistoryEntry entry = HISTORY.getEntryByURL(url);
                    entry.count = rsc.count;
                    entry.modifiedDate = new Date();
                } else {
                    HistoryEntry entry = new HistoryEntry();
                    entry.url = url;
                    entry.dir = rsc.getDir();
                    entry.count = rsc.count;

                    try {
                        entry.title = ripper.getAlbumTitle(ripper.getURL());
                    } catch (MalformedURLException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    HISTORY.add(entry);
                    historyTableModel.fireTableDataChanged();
                }

                if (configPlaySound.isSelected())
                    Utils.playSound("camera.wav");

                saveHistory();
                stopButton.setEnabled(false);
                statusProgress.setValue(0);
                statusProgress.setVisible(false);

                mainFrame.setTitle("RipMe v" + UpdateUtils.getThisJarVersion());

                openButton.setVisible(true);
                File f = rsc.dir;
                String prettyFile = Utils.shortenPath(f);
                openButton.setText("Open " + prettyFile);
                openButton.setIcon(getImageIcon("folder"));

                appendLog("Rip complete, saved to " + f.getAbsolutePath(), GREEN);
                openButton.setActionCommand(f.toString());
                openButton.addActionListener(event -> {
                    try {
                        Desktop.getDesktop().open(new File(event.getActionCommand()));
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                });

                pack();
                ripNextAlbum();
                break;
            case COMPLETED_BYTES:
                // Update completed bytes
                break;
            case TOTAL_BYTES:
                // Update total bytes
                break;
            default:
                break;
        }
    }

    @Override
    public void update(AbstractRipper ripper, RipStatusMessage message) {
        StatusEvent event = new StatusEvent(ripper, message);
        SwingUtilities.invokeLater(event);
    }

    /** Simple TextPane that allows horizontal scrolling. */
    class JTextPaneNoWrap extends JTextPane {

    /**
     * Método para pegar uma imagem
     *
     * @param imageName - Nome da imagem
     * @return retorna uma imagem
     */
    private static ImageIcon getImageIcon(String imageName) {
        try {
            URL urlImage = MainWindow.class.getClassLoader().getResource(imageName + ".png");

            if (urlImage != null) {
                Image image = ImageIO.read(urlImage);
                return new ImageIcon(image);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Simple TextPane that allows horizontal scrolling.
     */
    private class JTextPaneNoWrap extends JTextPane {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }
    }

    public static void ripAlbumStatic(String url) {
        ripTextfield.setText(url.trim());
        ripButton.doClick();
    }

    public static void enableWindowPositioning() {
        Utils.setConfigBoolean("window.position", true);
    }

    public static void disableWindowPositioning() {
        Utils.setConfigBoolean("window.position", false);
    }

    public static boolean isWindowPositioningEnabled() {
        boolean isEnabled = Utils.getConfigBoolean("window.position", true);
        return isEnabled;
    }

    public static void saveWindowPosition(Frame frame) {
        if (!isWindowPositioningEnabled()) {
            return;
        }
        Point point;
        try {
            point = frame.getLocationOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                point = frame.getLocation();
            } catch (Exception e2) {
                e2.printStackTrace();
                return;
            }
        }
        int x = (int)point.getX();
        int y = (int)point.getY();
        int w = frame.getWidth();
        int h = frame.getHeight();
        Utils.setConfigInteger("window.x", x);
        Utils.setConfigInteger("window.y", y);
        Utils.setConfigInteger("window.w", w);
        Utils.setConfigInteger("window.h", h);
        logger.debug("Saved window position (x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + ")");
    }

    public static void restoreWindowPosition(Frame frame) {
        if (!isWindowPositioningEnabled()) {
            mainFrame.setLocationRelativeTo(null); // default to middle of screen
            return;
        }
        try {
            int x = Utils.getConfigInteger("window.x", -1);
            int y = Utils.getConfigInteger("window.y", -1);
            int w = Utils.getConfigInteger("window.w", -1);
            int h = Utils.getConfigInteger("window.h", -1);
            if (x < 0 || y < 0 || w <= 0 || h <= 0) {
                logger.debug("UNUSUAL: One or more of: x, y, w, or h was still less than 0 after reading config");
                mainFrame.setLocationRelativeTo(null); // default to middle of screen
                return;
            }
            frame.setBounds(x, y, w, h);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


}