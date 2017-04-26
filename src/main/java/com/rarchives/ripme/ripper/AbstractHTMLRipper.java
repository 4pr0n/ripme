package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Simplified ripper, designed for ripping from sites by parsing HTML.
 */
public abstract class AbstractHTMLRipper extends AlbumRipper {

    public AbstractHTMLRipper(URL url) throws IOException {
        super(url);
    }

    public abstract String getDomain();

    public abstract Document getFirstPage() throws IOException;

    public Document getNextPage(Document doc) throws IOException {
        return null;
    }

    public abstract List<String> getURLsFromPage(Document page);

    public List<String> getDescriptionsFromPage(Document doc) throws IOException {
        // Do I do this or make an abstract function
        throw new IOException("getDescriptionsFromPage not implemented");
    }

    public abstract void downloadURL(URL url, int index);

    public DownloadThreadPool getThreadPool() {
        return null;
    }

    public boolean keepSortOrder() {
        return true;
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(getDomain());
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    public boolean hasDescriptionSupport() {
        return false;
    }

    public String getDescription(String page) throws IOException {
        throw new IOException("getDescription not implemented"); // Do I do this or make an abstract function?
    }

    public int descSleepTime() {
        return 0;
    }

    @Override
    public void rip() throws IOException {
        int index = 0;
        int textindex = 0;
        LOGGER.info("Retrieving " + this.url);
        sendUpdate(STATUS.LOADING_RESOURCE, this.url.toExternalForm());
        Document doc = getFirstPage();

        while (doc != null) {
            List<String> imageURLs = getURLsFromPage(doc);
            // Remove all but 1 image
            if (isThisATest()) {
                while (imageURLs.size() > 1)
                    imageURLs.remove(1);
            }

            if (imageURLs.isEmpty())
                throw new IOException("No images found at " + doc.location());

            for (String imageURL : imageURLs) {
                index += 1;
                LOGGER.debug("Found image url #" + index + ": " + imageURL);
                downloadURL(new URL(imageURL), index);

                if (isStopped())
                    break;
            }

            if (hasDescriptionSupport() && Utils.getConfigBoolean("descriptions.save", false)) {
                LOGGER.debug("Fetching description(s) from " + doc.location());
                List<String> textURLs = getDescriptionsFromPage(doc);

                if (!textURLs.isEmpty()) {
                    LOGGER.debug("Found description link(s) from " + doc.location());

                    for (String textURL : textURLs) {
                        if (isStopped())
                            break;

                        textindex += 1;
                        LOGGER.debug("Getting description from " + textURL);
                        sleep(descSleepTime());
                        String tempDesc = getDescription(textURL);

                        if (tempDesc != null) {
                            LOGGER.debug("Got description: " + tempDesc);
                            saveText(new URL(textURL), "", tempDesc, textindex);
                        }
                    }
                }
            }

            if (isStopped() || isThisATest())
                break;

            try {
                sendUpdate(STATUS.LOADING_RESOURCE, "next page");
                doc = getNextPage(doc);
            } catch (IOException e) {
                LOGGER.info("Can't get next page: " + e.getMessage(), e);
                break;
            }
        }

        // If they're using a thread pool, wait for it.
        if (getThreadPool() != null) {
            LOGGER.debug("Waiting for threadpool " + getThreadPool().getClass().getName());
            getThreadPool().waitForThreads();
        }
        waitForThreads();
    }

    public boolean saveText(URL url, String subdirectory, String text, int index) {
        // Not the best for some cases, like FurAffinity. Overridden there.
        try {
            stopCheck();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }

        String saveAs = url.toExternalForm();
        saveAs = saveAs.substring(saveAs.lastIndexOf('/') + 1);

        if (saveAs.indexOf('?') >= 0)
            saveAs = saveAs.substring(0, saveAs.indexOf('?'));
        if (saveAs.indexOf('#') >= 0)
            saveAs = saveAs.substring(0, saveAs.indexOf('#'));
        if (saveAs.indexOf('&') >= 0)
            saveAs = saveAs.substring(0, saveAs.indexOf('&'));
        if (saveAs.indexOf(':') >= 0)
            saveAs = saveAs.substring(0, saveAs.indexOf(':'));

        File saveFileAs;
        FileOutputStream fileOutputStream = null;

        try {
            // Not sure about this part
            if (!subdirectory.isEmpty())
                subdirectory = File.separator + subdirectory;

            // TODO Get prefix working again, probably requires reworking a lot of stuff! (Might be fixed now)
            saveFileAs = new File(workingDir.getCanonicalPath() + subdirectory + File.separator + getPrefix(index) +
                    saveAs + ".txt");

            // Write the file
            fileOutputStream = new FileOutputStream(saveFileAs);
            fileOutputStream.write(text.getBytes());
            Utils.closeResource(fileOutputStream);
        } catch (IOException e) {
            LOGGER.error("[!] Error creating save file path for description '" + url + "':", e);
            Utils.closeResource(fileOutputStream);
            return false;
        }

        LOGGER.debug("Downloading " + url + "'s description to " + saveFileAs);

        if (!saveFileAs.getParentFile().exists()) {
            LOGGER.info("[+] Creating directory: " + Utils.removeCWD(saveFileAs.getParent()));
            saveFileAs.getParentFile().mkdirs();
        }
        return true;
    }

    public String getPrefix(int index) {
        String prefix = "";

        if (keepSortOrder() && Utils.getConfigBoolean("download.save_order", true))
            prefix = String.format("%03d_", index);

        return prefix;
    }

}