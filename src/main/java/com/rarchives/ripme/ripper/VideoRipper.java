package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ui.RipStatusMessage;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public abstract class VideoRipper extends AbstractRipper {

    private int bytesTotal = 1;
    private int bytesCompleted = 1;

    public VideoRipper(URL url) throws IOException {
        super(url);
    }

    public abstract void rip() throws IOException;

    public abstract String getHost();

    public abstract String getGID(URL url) throws MalformedURLException;

    @Override
    public void setBytesTotal(int bytes) {
        this.bytesTotal = bytes;
    }

    @Override
    public void setBytesCompleted(int bytes) {
        this.bytesCompleted = bytes;
    }

    @Override
    public String getAlbumTitle(URL url) {
        return "videos";
    }

    @Override
    public boolean addURLToDownload(URL url, File saveAs) {
        if (Utils.getConfigBoolean("urls_only.save", false)) {
            // Output URL to file
            String urlFile = this.workingDir + File.separator + "urls.txt";

            FileWriter fw = null;
            try {
                fw = new FileWriter(urlFile, true);
                fw.write(url.toExternalForm());
                fw.write("\n");

                RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, urlFile);
                observer.update(this, msg);
            } catch (IOException e) {
                logger.error("Error while writing to " + urlFile, e);
                return false;
            } finally {
                close(fw);
            }
        } else {
            if (isThisATest()) {
                // Tests shouldn't download the whole video
                // Just change this.url to the download URL so the test knows we found it.
                logger.debug("Test rip, found URL: " + url);
                this.url = url;
                return true;
            }
            threadPool.addThread(new DownloadVideoThread(url, saveAs, this));
        }
        return true;
    }

    @Override
    public boolean addURLToDownload(URL url, File saveAs, String referrer, Map<String, String> cookies) {
        return addURLToDownload(url, saveAs);
    }

    @Override
    public void setWorkingDir(URL url) throws IOException {
        String path = Utils.getWorkingDirectory().getCanonicalPath();
        if (!path.endsWith(File.separator))
            path += File.separator;

        path += "videos" + File.separator;
        this.workingDir = new File(path);

        if (!this.workingDir.exists()) {
            logger.info("[+] Creating directory: " + Utils.removeCWD(this.workingDir));
            this.workingDir.mkdirs();
        }

        logger.debug("Set working directory to: " + this.workingDir);
    }

    @Override
    public int getCompletionPercentage() {
        return (int) (100 * (bytesCompleted / (float) bytesTotal));
    }

    @Override
    public void downloadCompleted(URL url, File saveAs) {
        if (observer == null)
            return;

        try {
            String path = Utils.removeCWD(saveAs);
            RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, path);
            observer.update(this, msg);

            checkIfComplete();
        } catch (Exception e) {
            logger.error("Exception while updating observer: ", e);
        }
    }

    @Override
    public void downloadErrored(URL url, String reason) {
        if (observer == null)
            return;

        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_ERRORED, url + " : " + reason));
        checkIfComplete();
    }

    @Override
    public void downloadExists(URL url, File file) {
        if (observer == null) {
            return;
        }
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_WARN, url + " already saved as " + file));
        checkIfComplete();
    }

    @Override
    public String getStatusText() {
        return String.valueOf(getCompletionPercentage()) +
                "% " +
                " - " +
                Utils.bytesToHumanReadable(bytesCompleted) +
                " / " +
                Utils.bytesToHumanReadable(bytesTotal);
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    /**
     * Notifies observers and updates state if all files have been ripped.
     */
    @Override
    protected void checkIfComplete() {
        if (observer == null) {
            return;
        }
        if (bytesCompleted >= bytesTotal) {
            super.checkIfComplete();
        }
    }

    /**
     * Method to execute close on any object that belongs to closable class
     *
     * @param closeable Object that belongs to closable class
     */
    private static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}