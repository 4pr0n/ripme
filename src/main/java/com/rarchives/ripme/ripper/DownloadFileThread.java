package com.rarchives.ripme.ripper;

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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

/**
 * Thread for downloading files.
 * Includes retry logic, observer notifications, and other goodies.
 */
public class DownloadFileThread extends Thread {

    private static final Logger logger = Logger.getLogger(DownloadFileThread.class);

    private String referrer = "";
    private Map<String,String> cookies = new HashMap<String,String>();

    private URL url;
    private URL originalURL;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;

    private final int TIMEOUT;

    public DownloadFileThread(URL url, File saveAs, AbstractRipper observer) {
        super();
        this.url = url;
        this.originalURL = url;
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
                logger.info("[!] Deleting existing file" + prettySaveAs);
                saveAs.delete();
            } else {
                logger.info("[!] Skipping " + url + " -- file already exists: " + prettySaveAs);
                observer.downloadProblem(url, "File already exists: " + prettySaveAs);
                return;
            }
        }

        int tries = 0; // Number of attempts to download
        do {
            tries += 1;
            InputStream bis = null; OutputStream fos = null;
            try {
                logger.info("    Downloading file: " + url + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // Setup HTTP request
                HttpURLConnection huc = httpRequest();

                int statusCode = huc.getResponseCode();
                if (statusCode / 100 == 4) { // 4xx errors
                    //HF format hack
                	if (statusCode == 404 && this.url.toString().matches("http://pictures\\.hentai-foundry\\.com//./[A-z0-9_\\-]*/[0-9]*\\.jpg")) {
                    	String pngVersion = this.url.toString().replaceAll("\\.jpg", ".png");
                    	logger.error("HF 404 on the .jpg, trying .png : " + this.url.toExternalForm());                    	
                    	this.url = new URL(pngVersion);
                    	this.saveAs = new File(this.saveAs.toString().replaceAll("\\.jpg", ".png"));                    	
                    	//throw new IOException("HF 404 on the .jpg, trying .png : " + url.toExternalForm());
                    	huc = httpRequest();
                    	statusCode = huc.getResponseCode();
                    	
                    	if (statusCode == 404) {
	                    	String gifVersion = url.toString().replaceAll("\\.png", ".gif");
	                    	logger.error("HF 404 on the .png, trying .gif : " + this.url.toExternalForm());
	                    	this.url = new URL(gifVersion);
	                    	this.saveAs = new File(this.saveAs.toString().replaceAll("\\.png", ".gif"));
	                    	huc = httpRequest();
	                    	statusCode = huc.getResponseCode();
	                    
	                    	if (statusCode == 404) {
	                        	String swfVersion = url.toString().replaceAll("\\.gif", ".swf");
	                        	logger.error("HF 404 on the .png, trying .swf : " + this.url.toExternalForm());
	                        	this.url = new URL(swfVersion);
	                        	this.saveAs = new File(this.saveAs.toString().replaceAll("\\.gif", ".swf"));
	                        	huc = httpRequest();
		                    	statusCode = huc.getResponseCode();
	                    	
								if (statusCode == 404) {
		                        	logger.error("HF 404 on the .swf, quitting : " + this.url.toExternalForm());
		    	                    observer.downloadErrored(originalURL, "HF 404 on the .swf, quitting : " + url.toExternalForm());
		    	                    return; // Not retriable, drop out.
	                        	}
							}
						}
                    	if (statusCode == 200) {                  
                    		observer.downloadCompleted(originalURL,saveAs);
                    		logger.info("HF 404 on the .jpg, other format succesful: " + url.toExternalForm());
                    	}
                    	
	                 } else {
	                	logger.error("[!] Non-retriable status code " + statusCode + " while downloading from " + url);
	                    observer.downloadErrored(url, "Non-retriable status code " + statusCode + " while downloading " + url.toExternalForm());
	                    return; // Not retriable, drop out.
                    }
                }
                if (statusCode / 100 == 5) { // 5xx errors
                    observer.downloadErrored(url, "Retriable status code " + statusCode + " while downloading " + url.toExternalForm());
                    // Throw exception so download can be retried
                    throw new IOException("Retriable status code " + statusCode);
                }
                if (huc.getContentLength() == 503 && url.getHost().endsWith("imgur.com")) {
                    // Imgur image with 503 bytes is "404"
                    logger.error("[!] Imgur image is 404 (503 bytes long): " + url);
                    observer.downloadErrored(url, "Imgur image is 404: " + url.toExternalForm());
                    return;
                }

                // Save file
                bis = new BufferedInputStream(huc.getInputStream());
                fos = new FileOutputStream(saveAs);
                IOUtils.copy(bis, fos);
                break; // Download successful: break out of infinite loop
            } catch (HttpStatusException hse) {
                logger.error("[!] HTTP status " + hse.getStatusCode() + " while downloading from " + url);
                if (hse.getStatusCode() == 404 && Utils.getConfigBoolean("errors.skip404", false)) {
                    observer.downloadErrored(url, "HTTP status code " + hse.getStatusCode() + " while downloading " + url.toExternalForm());
                    return;
                }
            } catch (IOException e) {
                logger.error("[!] Exception while downloading file: " + url + " - " + e.getMessage(), e);
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
                logger.error("[!] Exceeded maximum retries (" + this.retries + ") for URL " + url);
                observer.downloadErrored(url, "Failed to download " + url.toExternalForm());
                return;
            }
        } while (true);
        if (this.url == originalURL)
        	observer.downloadCompleted(url, saveAs);
        logger.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }
    
    private HttpURLConnection httpRequest() throws IOException {
    	// Setup HTTP request
        HttpURLConnection huc = (HttpURLConnection) this.url.openConnection();
        huc.setConnectTimeout(TIMEOUT);
        huc.setRequestProperty("accept",  "*/*");
        huc.setRequestProperty("Referer", referrer); // Sic
        huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
        String cookie = "";
        for (String key : cookies.keySet()) {
            if (!cookie.equals("")) {
                cookie += "; ";
            }
            cookie += key + "=" + cookies.get(key);
        }
        huc.setRequestProperty("Cookie", cookie);
        huc.connect();
        return huc;
    }

}
