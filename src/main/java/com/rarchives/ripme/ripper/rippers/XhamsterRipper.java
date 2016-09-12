package com.rarchives.ripme.ripper.rippers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class XhamsterRipper extends AlbumRipper {

    private static final String HOST = "xhamster";

    private static Pattern xhPattern = Pattern.compile("^https?://[wmde.]*xhamster\\.com/photos/(?:gallery/([0-9]+).*|view/([0-9]+)-([0-9]+)\\.html(?:.*)?)$");

    private HashMap<String, Document> docs = new HashMap<String, Document>();

    public XhamsterRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public boolean canRip(URL url) {
        Matcher m = xhPattern.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public void rip() throws IOException {
        if (isGallery(url)) {
            ripGallery();
        } else {
            ripPhoto();
        }
    }

    private static boolean isGallery(URL url) {
        Matcher m = xhPattern.matcher(url.toExternalForm());
        if (!m.matches()) {
            return false;
        }
        return m.group(3) == null || m.group(3).length() == 0; // Is a gallery.
    }

    private void ripPhoto() throws IOException {
        Document doc = downloadAndSaveHTML(this.url);
        for (Element element : doc.select("img#imgSized")) {
            String image = cleanImageSrc(element.attr("src"));
            addURLToDownload(new URL(image), "");
        }
        waitForThreads();
    }

    private void ripGallery() throws IOException {
        int index = 0;
        String nextURL = this.url.toExternalForm();
        while (nextURL != null) {
            logger.info("    Retrieving " + nextURL);
            Document doc = downloadAndSaveHTML(new URL(nextURL));
            for (Element thumb : doc.select("table.iListing div.img img")) {
                if (!thumb.hasAttr("src")) {
                    continue;
                }
                String image = cleanImageSrc(thumb.attr("src"));
                index += 1;
                String prefix = "";
                if (Utils.getConfigBoolean("download.save_order", true)) {
                    prefix = String.format("%03d_", index);
                }
                addURLToDownload(new URL(image), prefix);
                if (isThisATest()) {
                    break;
                }
            }
            if (isThisATest()) {
                break;
            }
            nextURL = null;
            for (Element element : doc.select("a.last")) {
                nextURL = element.attr("href");
                break;
            }
        }
        waitForThreads();
    }

    private String cleanImageSrc(String imageSrc) {
        imageSrc = imageSrc.replaceAll("https?://p[0-9]*\\.", "https?://up.");
        imageSrc = imageSrc.replaceAll("_160\\.", "_1000.");
        return imageSrc;
    }

    private Document downloadAndSaveHTML(URL url) throws IOException {
        Document doc = docs.get(url);
        if (doc == null) {
            doc = Http.url(url.toExternalForm()).get();
            docs.put(url.toExternalForm(), doc);
        }
        String filename = urlToFilename(url);
        Files.write(Paths.get(getWorkingDir().getCanonicalPath() + File.separator + filename), doc.toString().getBytes());
        return doc;
    }

    private static String urlToFilename(URL url) {
        String filename = url.toExternalForm().replaceFirst("^https?://.*/", "").replaceFirst("#.*$", "");
        if (filename.contains("?") && filename.contains(".")) {
            int periodIdx = filename.lastIndexOf('.');
            int questionMarkIdx = filename.indexOf('?');
            String params = filename.substring(questionMarkIdx + 1).replaceAll("=", "-").replaceAll("&", "_");
            filename = filename.substring(0, periodIdx) + "_" + params + filename.substring(periodIdx, questionMarkIdx);
        }
        return filename;
    }

    /**
     * @todo prefix with uploader username
     */
    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        String title = HOST + "_";
        Document doc = docs.get(url);
        if (doc == null) {
            try {
                doc = Http.url(url).get();
                docs.put(url.toString(), doc);
            } catch (IOException e) {
                logger.error("Failed to download url=" + url + ": " + e.getMessage());
            }
        }
        if (doc != null) {
            for (Element link : doc.select("#galleryUser .item a")) {
                title += link.text() + "_";
                break;
            }
            String galleryLink = "";
            if (isGallery(url)) {
                galleryLink = url.toExternalForm();
            } else {
                for (Element link : doc.select("#viewBox a")) {
                    String href = link.attr("href");
                    if (href.length() > 0 && !href.startsWith("#")) {
                        galleryLink = href;
                        break;
                    }
                }
            }
            title += galleryLink.toString()
                .replaceFirst("^http.*/photos/gallery/([^?#]+).*$", "$1")
                .replace('/', '-')
                .replace(".html", "");
        }
        return title;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://([a-z0-9.]*?)xhamster\\.com/photos/gallery/([0-9]{1,})/.*\\.html");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(2);
        }
        throw new MalformedURLException(
                "Expected xhamster.com gallery formats: "
                        + "xhamster.com/photos/gallery/#####/xxxxx..html"
                        + " Got: " + url);
    }

}
