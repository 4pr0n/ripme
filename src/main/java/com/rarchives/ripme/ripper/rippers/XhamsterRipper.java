package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
            Document doc = Http.url(nextURL).header("User-Agent", USER_AGENT).referrer("http://" + HOST + ".com/").cookies(Utils.getCookies(HOST)).get();
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

    /**
     * @todo prefix with uploader username
     */
    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        String title = HOST + "_";
        Document doc = docs.get(url);;
        if (doc == null) {
            try {
                doc = Http.url(url).header("User-Agent", USER_AGENT).referrer(url).cookies(Utils.getCookies(HOST)).get();
                docs.put(url.toString(), doc);
            } catch (IOException e) {
                logger.error("Failed to download url=" + url + ": " + e.getMessage());
            }
        }
        // Find username.
        if (doc != null) {
            for (Element link : doc.select("#galleryUser .item a")) {
                title += link.text() + "_";
                break;
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
