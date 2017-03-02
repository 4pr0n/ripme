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

    private HashMap<String, Document> docs = new HashMap<String, Document>();

    public XhamsterRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://[wmde.]*xhamster\\.com/photos/gallery/[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public void rip() throws IOException {
        int index = 0;
        String nextURL = this.url.toExternalForm();
        while (nextURL != null) {
            logger.info("    Retrieving " + nextURL);
            Document doc = downloadAndSaveHTML(nextURL);
            for (Element thumb : doc.select("table.iListing div.img img")) {
                if (!thumb.hasAttr("src")) {
                    continue;
                }
                String image = thumb.attr("src");
                image = image.replaceAll(
                        "https://upt.xhcdn\\.",
                        "http://up.xhamster.");
                image = image.replaceAll(
                        "_160\\.",
                        "_1000.");
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

    private Document downloadAndSaveHTML(String url) throws IOException {
        Document doc = docs.get(url);
        if (doc == null) {
            doc = Http.url(url).header("User-Agent", USER_AGENT).referrer(url).cookies(Utils.getCookies(HOST)).get();
            docs.put(url, doc);
        }
        String filename = url.replaceFirst("^http://.*/", "");
        if (filename.contains("?") && filename.contains(".")) {
            int periodIdx = filename.lastIndexOf('.');
            int questionMarkIdx = filename.indexOf('?');
            String params = filename.substring(questionMarkIdx + 1).replaceAll("=", "-").replaceAll("&", "_");
            filename = filename.substring(0, periodIdx) + "_" + params + filename.substring(periodIdx, questionMarkIdx);
        }
        if (getWorkingDir() != null) {
            Files.write(Paths.get(getWorkingDir().getCanonicalPath() + File.separator + filename), doc.toString().getBytes());
        }
        return doc;
    }

    /**
     * @todo prefix with uploader username
     */
    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        String title = HOST + "_";
        Document doc = null;
        try {
            doc = downloadAndSaveHTML(url.toString());
        } catch (IOException e) {
            logger.error("Exception retrieving url=" + url + ": " + e.getMessage());
            title += getGID(url);
        }
        if (doc != null) {
            // Find username.
            Element link = doc.select("#galleryUser .item a").first();
            if (link != null) {
                title += link.text() + "_";
            }
        }
        String galleryLink = url.toExternalForm();
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
