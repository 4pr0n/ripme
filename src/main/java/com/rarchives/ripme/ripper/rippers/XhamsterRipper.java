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

    private static Pattern xhPattern = Pattern.compile("^https?://[a-z.]*" + HOST + "\\.com/photos/(?:gallery/([0-9]+).*|view/([0-9]+)-([0-9]+)\\.html(?:.*)?)$");

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
        Document doc = downloadAndSaveHTML(url);
        for (Element element : doc.select("img#imgSized")) {
            String image = cleanImageSrc(element.attr("src"));
            addURLToDownload(new URL(image), "", "", url.toExternalForm(), Utils.getCookies(HOST));
        }
        waitForThreads();
    }

    private void ripGallery() throws IOException {
        int index = 0;
        String nextURL = url.toExternalForm();
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
        String urlString = url.toExternalForm();
        Document doc = docs.get(urlString);
        if (doc == null) {
            doc = Http.url(url).header("User-Agent", USER_AGENT).referrer(url).cookies(Utils.getCookies(HOST)).get();
            docs.put(urlString, doc);
        }
        String filename = urlToFilename(url);
        if (getWorkingDir() != null) {
            Files.write(Paths.get(getWorkingDir().getCanonicalPath() + File.separator + filename), doc.toString().getBytes());
        }
        return doc;
    }

    private static String urlToFilename(URL url) {
        String filename = url.toExternalForm().replaceFirst("^https?://.*/", "").replaceFirst("[#&:].*$", "");
        if (filename.contains("?") && filename.contains(".")) {
            int periodIdx = filename.lastIndexOf('.');
            int questionMarkIdx = filename.indexOf('?');
            String params = filename.substring(questionMarkIdx + 1).replaceAll("=", "-").replaceAll("&", "_");
            filename = filename.substring(0, periodIdx) + "_" + params + filename.substring(periodIdx, questionMarkIdx);
        }
        return filename;
    }

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        String title = HOST + "_";
        Document doc = null;
        try {
            doc = downloadAndSaveHTML(url);
        } catch (IOException e) {
            logger.error("Exception retrieving url=" + url + ": " + e.getMessage());
            title += getGID(url);
        }
        if (doc != null) {
            // Find username.
            Element link = doc.select("#galleryUser .item a").first();
            if (link != null) {
                title += link.text() + "_";
            } else {
                logger.warn("No username was found in the contents of url=" + url);
            }
        } else {
            logger.warn("No username could be retrieved for url=" + url);
        }
        String galleryLink = url.toExternalForm();
        if (!isGallery(url) && doc != null) {
            for (Element link : doc.select("#viewBox a")) {
                if (link != null) {
                    String href = link.attr("href");
                    if (href.length() > 0 && !href.startsWith("#")) {
                        galleryLink = href;
                        break;
                    }
                }
            }
            if (galleryLink == url.toExternalForm()) {
                logger.warn("No gallery title link was found for url=" + url);
            }
        }
        title += galleryLink
            .replaceFirst("^http.*/photos/(?:gallery/([^?#:&]+)|view/([^-]+)-).*$", "$1$2")
            .replace('/', '-')
            .replace(".html", "");
        return title;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        String gid = url.toExternalForm().replaceFirst("^https?://(?:[a-z0-9.]*?)" + HOST + "\\.com/photos/(?:gallery/([0-9]{1,})/.*\\.html|view/([^-]+)-).*$", "$1$2");
        if (gid.length() == 0) {
            throw new MalformedURLException(
                "Expected xhamster.com gallery formats: "
                + "http://xhamster.com/photos/gallery/#####/xxxxx..html or http://xhamster.com/photos/view/####-####.html"
                + " Got: " + url);
        }
        return gid;
    }

}
