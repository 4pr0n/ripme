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
                        "http://p[0-9]*\\.",
                        "http://up.");
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
            doc = Http.url(url).get();
            docs.put(url, doc);
        }
        String filename = url.replaceFirst("^http://.*/", "");
        if (filename.contains("?") && filename.contains(".")) {
            int periodIdx = filename.lastIndexOf('.');
            int questionMarkIdx = filename.indexOf('?');
            String params = filename.substring(questionMarkIdx + 1).replaceAll("=", "-").replaceAll("&", "_");
            filename = filename.substring(0, periodIdx) + "_" + params + filename.substring(periodIdx, questionMarkIdx);
        }
        Files.write(Paths.get(getWorkingDir().getCanonicalPath() + File.separator + filename), doc.toString().getBytes());
        return doc;
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
                doc = Http.url(url).get(); // downloadAndSaveHTML(url.toString());
                docs.put(url.toString(), doc);
            } catch (IOException e) {
                logger.error("Failed to download url=" + url + ": " + e.getMessage());
            }
        }
        if (doc != null) {
            for (Element node : doc.select("#galleryUser .item a")) {
                title += node.text() + "_";
                break;
            }
        }
        title += url.toString().replaceFirst("^http.*/photos/gallery/([^?#]+).*$", "$1").replace('/', '-');
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
