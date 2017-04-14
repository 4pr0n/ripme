package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ui.RipStatusMessage;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AlbumRipper extends AbstractRipper {

    protected Map<URL, File> itemsPending = Collections.synchronizedMap(new HashMap<>());
    protected Map<URL, File> itemsCompleted = Collections.synchronizedMap(new HashMap<>());
    protected Map<URL, String> itemsErrored = Collections.synchronizedMap(new HashMap<>());

    public AlbumRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public abstract boolean canRip(URL url);

    public boolean allowDuplicates() {
        return false;
    }

    @Override
    public int getCount() {
        return itemsCompleted.size() + itemsErrored.size();
    }

    @Override
    public boolean addURLToDownload(URL url, File saveAs, String referrer, Map<String, String> cookies) {
        // Only download one file if this is a test.
        if (super.isThisATest() && (itemsPending.size() > 0 || itemsCompleted.size() > 0 || itemsErrored.size() > 0)) {
            stop();
            return false;
        }

        if (!allowDuplicates() && (itemsPending.containsKey(url) || itemsCompleted.containsKey(url) || itemsErrored.containsKey(url))) {
            // Item is already downloaded/downloading, skip it.
            LOGGER.info("[!] Skipping " + url + " -- already attempted: " + Utils.removeCWD(saveAs));
            return false;
        }

        if (Utils.getConfigBoolean("urls_only.save", false)) {
            // Output URL to file
            String urlFile = this.workingDir + File.separator + "urls.txt";

            try (FileWriter fw = new FileWriter(urlFile, true)) {
                fw.write(url.toExternalForm());
                fw.write("\n");
                RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, urlFile);
                itemsCompleted.put(url, new File(urlFile));
                observer.update(this, msg);
            } catch (IOException e) {
                LOGGER.error("Error while writing to " + urlFile, e);
            }
        } else {
            itemsPending.put(url, saveAs);
            DownloadFileThread dft = new DownloadFileThread(url, saveAs, this);

            if (referrer != null)
                dft.setReferrer(referrer);

            if (cookies != null)
                dft.setCookies(cookies);

            threadPool.addThread(dft);
        }
        return true;
    }

    @Override
    public boolean addURLToDownload(URL url, File saveAs) {
        return addURLToDownload(url, saveAs, null, null);
    }

    /**
     * Queues image to be downloaded and saved.
     * Uses filename from URL to decide filename.
     *
     * @param url URL to download
     * @return True on success
     */
    public boolean addURLToDownload(URL url) {
        // Use empty prefix and empty subdirectory
        return addURLToDownload(url, "", "");
    }

    @Override
    public void downloadCompleted(URL url, File saveAs) {
        if (observer == null)
            return;

        try {
            String path = Utils.removeCWD(saveAs);
            RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, path);
            itemsPending.remove(url);
            itemsCompleted.put(url, saveAs);
            observer.update(this, msg);

            checkIfComplete();
        } catch (Exception e) {
            LOGGER.error("Exception while updating observer: ", e);
        }
    }

    @Override
    public void downloadErrored(URL url, String reason) {
        if (observer == null)
            return;

        itemsPending.remove(url);
        itemsErrored.put(url, reason);
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_ERRORED, url + " : " + reason));

        checkIfComplete();
    }

    @Override
    public void downloadExists(URL url, File file) {
        if (observer == null)
            return;

        itemsPending.remove(url);
        itemsCompleted.put(url, file);
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_WARN, url + " already saved as " + file.getAbsolutePath()));

        checkIfComplete();
    }

    /**
     * Notifies observers and updates state if all files have been ripped.
     */
    @Override
    protected void checkIfComplete() {
        if (observer == null)
            return;

        if (itemsPending.isEmpty())
            super.checkIfComplete();
    }

    /**
     * Sets directory to save all ripped files to.
     *
     * @param url URL to define how the working directory should be saved.
     * @throws IOException
     */
    @Override
    public void setWorkingDir(URL url) throws IOException {
        String path = Utils.getWorkingDirectory().getCanonicalPath();
        if (!path.endsWith(File.separator))
            path += File.separator;

        String title;
        if (Utils.getConfigBoolean("album_titles.save", true))
            title = getAlbumTitle(this.url);
        else
            title = super.getAlbumTitle(this.url);

        LOGGER.debug("Using album title '" + title + "'");
        title = Utils.filesystemSafe(title);
        path += title + File.separator;
        this.workingDir = new File(path);

        if (!this.workingDir.exists()) {
            LOGGER.info("[+] Creating directory: " + Utils.removeCWD(this.workingDir));
            this.workingDir.mkdirs();
        }
        LOGGER.debug("Set working directory to: " + this.workingDir);
    }

    /**
     * @return Integer between 0 and 100 defining the progress of the album rip.
     */
    @Override
    public int getCompletionPercentage() {
        double total = (double) itemsPending.size() + itemsErrored.size() + itemsCompleted.size();
        return (int) (100 * ((total - itemsPending.size()) / total));
    }

    /**
     * @return Human-readable information on the status of the current rip.
     */
    @Override
    public String getStatusText() {
        return String.valueOf(getCompletionPercentage()) + "% - Pending: " + itemsPending.size() + ", Completed: " +
                itemsCompleted.size() + ", Errored: " + itemsErrored.size();
    }
}