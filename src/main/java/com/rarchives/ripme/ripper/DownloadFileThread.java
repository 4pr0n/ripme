package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread for downloading files.
 * Includes retry logic, observer notifications, and other goodies.
 */
public class DownloadFileThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(DownloadFileThread.class);

    private String referrer = "";
    private Map<String,String> cookies = new HashMap<String,String>();

    private URL url;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;

    private final int TIMEOUT;

    public DownloadFileThread(URL url, File saveAs, AbstractRipper observer) {
        super();
        this.url = url;
        this.saveAs = saveAs;
        this.prettySaveAs = Utils.removeCWD(saveAs);
        this.observer = observer;
        this.retries = Utils.getConfigInteger("download.retries", 1);
        this.TIMEOUT = Utils.getConfigInteger("download.timeout", 60000);
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
    public void setCookies(Map<String,String> cookies) {
        this.cookies = cookies;
    }

    /**
     * Attempts to download the file. Retries as needed.
     * Notifies observers upon completion/error/warn.
     */
    public void run() {
        try {
            observer.stopCheck();
        } catch (IOException e) {
            observer.downloadErrored(url, "Download interrupted");
            return;
        }
        if (saveAs.exists()) {
            if (Utils.getConfigBoolean("file.overwrite", false)) {
                LOGGER.info("[!] Deleting existing file" + prettySaveAs);
                saveAs.delete();
            } else {
                LOGGER.info("[!] Skipping " + url + " -- file already exists: " + prettySaveAs);
                observer.downloadExists(url, saveAs);
                return;
            }
        }

        URL urlToDownload = this.url;
        boolean redirected = false;
        int tries = 0; // Number of attempts to download
        do {
            tries += 1;
            InputStream bis = null; OutputStream fos = null;
            try {
                LOGGER.info("    Downloading file: " + urlToDownload + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // Setup HTTP request
                HttpURLConnection huc;
                if (this.url.toString().startsWith("https")) {
                    huc = (HttpsURLConnection) urlToDownload.openConnection();
                }
                else {
                    huc = (HttpURLConnection) urlToDownload.openConnection();
                }
                huc.setInstanceFollowRedirects(true);
                huc.setConnectTimeout(TIMEOUT);
                huc.setRequestProperty("accept",  "*/*");
                if (!referrer.equals("")) {
                    huc.setRequestProperty("Referer", referrer); // Sic
                }
                huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
                String cookie = "";
                for (String key : cookies.keySet()) {
                    if (!cookie.equals("")) {
                        cookie += "; ";
                    }
                    cookie += key + "=" + cookies.get(key);
                }
                huc.setRequestProperty("Cookie", cookie);
                LOGGER.debug("Request properties: " + huc.getRequestProperties());
                huc.connect();

                int statusCode = huc.getResponseCode();
                LOGGER.debug("Status code: " + statusCode);
                if (statusCode  / 100 == 3) { // 3xx Redirect
                    if (!redirected) {
                        // Don't increment retries on the first redirect
                        tries--;
                        redirected = true;
                    }
                    String location = huc.getHeaderField("Location");
                    urlToDownload = new URL(location);
                    // Throw exception so download can be retried
                    throw new IOException("Redirect status code " + statusCode + " - redirect to " + location);
                }
                if (statusCode / 100 == 4) { // 4xx errors
                    LOGGER.error("[!] Non-retriable status code " + statusCode + " while downloading from " + url);
                    observer.downloadErrored(url, "Non-retriable status code " + statusCode + " while downloading " + url.toExternalForm());
                    return; // Not retriable, drop out.
                }
                if (statusCode / 100 == 5) { // 5xx errors
                    observer.downloadErrored(url, "Retriable status code " + statusCode + " while downloading " + url.toExternalForm());
                    // Throw exception so download can be retried
                    throw new IOException("Retriable status code " + statusCode);
                }
                if (huc.getContentLength() == 503 && urlToDownload.getHost().endsWith("imgur.com")) {
                    // Imgur image with 503 bytes is "404"
                    LOGGER.error("[!] Imgur image is 404 (503 bytes long): " + url);
                    observer.downloadErrored(url, "Imgur image is 404: " + url.toExternalForm());
                    return;
                }

                // Save file
                bis = new BufferedInputStream(huc.getInputStream());
                fos = new FileOutputStream(saveAs);
                IOUtils.copy(bis, fos);
                break; // Download successful: break out of infinite loop
            } catch (HttpStatusException hse) {
                LOGGER.debug("HTTP status exception", hse);
                LOGGER.error("[!] HTTP status " + hse.getStatusCode() + " while downloading from " + urlToDownload);
                if (hse.getStatusCode() == 404 && Utils.getConfigBoolean("errors.skip404", false)) {
                    observer.downloadErrored(url, "HTTP status code " + hse.getStatusCode() + " while downloading " + url.toExternalForm());
                    return;
                }
            } catch (IOException e) {
                LOGGER.debug("IOException", e);
                LOGGER.error("[!] Exception while downloading file: " + url + " - " + e.getMessage());
            } finally {
                // Close any open streams
                try {
                    if (bis != null) { bis.close(); }
                } catch (IOException e) { }
                try {
                    if (fos != null) { fos.close(); }
                } catch (IOException e) { }
            }
            if (tries > this.retries) {
                LOGGER.error("[!] Exceeded maximum retries (" + this.retries + ") for URL " + url);
                observer.downloadErrored(url, "Failed to download " + url.toExternalForm());
                return;
            }
        } while (true);
        observer.downloadCompleted(url, saveAs);
        LOGGER.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }

}
