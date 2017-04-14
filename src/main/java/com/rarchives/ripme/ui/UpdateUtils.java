package com.rarchives.ripme.ui;

import com.rarchives.ripme.utils.Utils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class UpdateUtils {

    private static final Logger LOGGER = Logger.getLogger(UpdateUtils.class);
    private static final String DEFAULT_VERSION = "1.4.6";
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/4pr0n/ripme/master/ripme.json";
    private static final String MAIN_FILE_NAME = "ripme.jar";
    private static final String UPDATE_FILE_NAME = "ripme.jar.update";

    private static final String RIPME_UPDATER = "RipMe Updater";

    private UpdateUtils() {
    }

    public static String getUpdateJarURL(String latestVersion) {
        return "https://github.com/4pr0n/ripme/releases/download/" + latestVersion + "/ripme.jar";
    }

    public static String getThisJarVersion() {
        String thisVersion = UpdateUtils.class.getPackage().getImplementationVersion();
        if (thisVersion == null) {
            // Version is null if we're not running from the JAR
            thisVersion = DEFAULT_VERSION;
            // Super-high version number
        }
        return thisVersion;
    }

    public static void updateProgram(JLabel configUpdateLabel) {
        configUpdateLabel.setText("Checking for update...");

        Document doc;

        try {
            LOGGER.debug("Retrieving " + UpdateUtils.UPDATE_JSON_URL);
            doc = Jsoup.connect(UpdateUtils.UPDATE_JSON_URL).timeout(10_000).ignoreContentType(true).get();
        } catch (IOException e) {
            LOGGER.error("Error while fetching update: ", e);
            JOptionPane.showMessageDialog(null,
                    "<html><font color=\"red\">Error while fetching update: " + e.getMessage() + "</font></html>",
                    RIPME_UPDATER, JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            configUpdateLabel.setText("Current version: " + getThisJarVersion());
        }

        String jsonString = doc.body().html().replaceAll("&quot;", "\"");
        JSONObject json = new JSONObject(jsonString);
        JSONArray jsonChangeList = json.getJSONArray("changeList");
        StringBuilder changeList = new StringBuilder();

        for (int i = 0; i < jsonChangeList.length(); i++) {
            String change = jsonChangeList.getString(i);

            if (change.startsWith(getThisJarVersion() + ":"))
                break;

            changeList.append("<br>  + ").append(change);
        }

        String latestVersion = json.getString("latestVersion");
        if (UpdateUtils.isNewerVersion(latestVersion)) {
            LOGGER.info("Found newer version: " + latestVersion);
            int result = JOptionPane.showConfirmDialog(
                    null, "<html><font color=\"green\">New version (" + latestVersion + ") is available!</font>"
                            + "<br><br>Recent changes:" + changeList.toString()
                            + "<br><br>Do you want to download and run the newest version?</html>",
                    RIPME_UPDATER, JOptionPane.YES_NO_OPTION);

            if (result != JOptionPane.YES_OPTION) {
                configUpdateLabel.setText("<html>Current Version: " + getThisJarVersion()
                        + "<br><font color=\"green\">Latest version: " + latestVersion + "</font></html>");
                return;
            }
            configUpdateLabel.setText("<html><font color=\"green\">Downloading new version...</font></html>");
            LOGGER.info("New version found, downloading...");

            try {
                UpdateUtils.downloadJarAndLaunch(getUpdateJarURL(latestVersion));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error while updating: " + e.getMessage(), RIPME_UPDATER,
                        JOptionPane.ERROR_MESSAGE);
                configUpdateLabel.setText("");
                LOGGER.error("Error while updating: ", e);
            }
        } else {
            LOGGER.debug("This version (" + getThisJarVersion() +
                    ") is the same or newer than the website's version (" + latestVersion + ")");
            configUpdateLabel.setText("<html><font color=\"green\">v" + getThisJarVersion() +
                    " is the latest version</font></html>");
            LOGGER.debug("Running latest version: " + getThisJarVersion());
        }
    }

    private static boolean isNewerVersion(String latestVersion) {
        int[] oldVersions = versionStringToInt(getThisJarVersion());
        int[] newVersions = versionStringToInt(latestVersion);

        if (oldVersions.length < newVersions.length) {
            System.err.println("Calculated: " + Arrays.toString(oldVersions) + " < " + latestVersion);
            return true;
        }

        for (int i = 0; i < oldVersions.length; i++) {
            if (newVersions[i] > oldVersions[i]) {
                LOGGER.debug("oldVersion " + getThisJarVersion() + " < latestVersion" + latestVersion);
                return true;
            } else if (newVersions[i] < oldVersions[i]) {
                LOGGER.debug("oldVersion " + getThisJarVersion() + " > latestVersion " + latestVersion);
                return false;
            }
        }

        // At this point, the version numbers are exactly the same.
        // Assume any additional changes to the version text means a new version
        return !(latestVersion.equals(getThisJarVersion()));
    }

    private static int[] versionStringToInt(String version) {
        String strippedVersion = version.split("-")[0];
        String[] strVersions = strippedVersion.split("\\.");
        int[] intVersions = new int[strVersions.length];

        for (int i = 0; i < strVersions.length; i++)
            intVersions[i] = Integer.parseInt(strVersions[i]);

        return intVersions;
    }

    private static void downloadJarAndLaunch(String updateJarURL) throws IOException {
        Response response = Jsoup.connect(updateJarURL).ignoreContentType(true)
                .timeout(Utils.getConfigInteger("download.timeout", 60_000))
                .maxBodySize(1024 ^ 2 * 100).execute();

        downloadNewVersion(response);

        // Setup updater script
        final String batchFile;
        final String script;
        final String[] batchExec;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            batchFile = "update_ripme.bat";
            String batchPath = new File(batchFile).getAbsolutePath();
            script = "@echo off\r\n"
                    + "timeout 1" + "\r\n"
                    + "copy " + UPDATE_FILE_NAME + " " + MAIN_FILE_NAME + "\r\n"
                    + "del " + UPDATE_FILE_NAME + "\r\n"
                    + "ripme.jar" + "\r\n"
                    + "del " + batchPath + "\r\n";
            batchExec = new String[]{batchPath};
        } else {
            // Mac / Linux
            batchFile = "update_ripme.sh";
            String batchPath = new File(batchFile).getAbsolutePath();
            script = "#!/bin/sh\n"
                    + "sleep 1" + "\n"
                    + "cd " + new File(MAIN_FILE_NAME).getAbsoluteFile().getParent() + "\n"
                    + "cp -f " + UPDATE_FILE_NAME + " " + MAIN_FILE_NAME + "\n"
                    + "rm -f " + UPDATE_FILE_NAME + "\n"
                    + "java -jar \"" + new File(MAIN_FILE_NAME).getAbsolutePath() + "\" &\n"
                    + "sleep 1" + "\n"
                    + "rm -f " + batchPath + "\n";
            batchExec = new String[]{"sh", batchPath};
        }

        createUpdaterScript(batchFile, script);

        // Run updater script on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("Executing: " + batchFile);
                Runtime.getRuntime().exec(batchExec);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }));

        LOGGER.info("Exiting older version, should execute update script (" + batchFile + ") during exit");
        System.exit(0);
    }

    /**
     * Method that generates the script updater
     *
     * @param batchFile Path where the script will be saved
     * @param script    Script content
     */
    private static void createUpdaterScript(String batchFile, String script) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(batchFile))) {
            // Create updater script
            bw.write(script);
            bw.flush();
            bw.close();
            LOGGER.info("Saved update script to " + batchFile);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Method that receives a response from the server with the new version of the application
     * and downloads the file
     *
     * @param response Response coming from the Update Server
     */
    private static void downloadNewVersion(Response response) {
        try (FileOutputStream out = new FileOutputStream(UPDATE_FILE_NAME)) {
            out.write(response.bodyAsBytes());
            out.close();
            LOGGER.info("Download of new version complete; saved to " + UPDATE_FILE_NAME);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}