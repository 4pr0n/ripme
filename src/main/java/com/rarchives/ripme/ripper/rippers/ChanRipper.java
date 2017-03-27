package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.rippers.ripperhelpers.ChanSite;
import com.rarchives.ripme.utils.Http;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChanRipper extends AbstractHTMLRipper {

    private static final List<ChanSite> EXPLICIT_DOMAINS = Arrays.asList(
            new ChanSite(Collections.singletonList("boards.4chan.org"), Arrays.asList("4cdn.org", "is.4chan.org", "is2.4chan.org")),
            new ChanSite(Collections.singletonList("archive.moe"), Collections.singletonList("data.archive.moe")),
            new ChanSite(Collections.singletonList("4archive.org"), Collections.singletonList("imgur.com")),
            new ChanSite(Collections.singletonList("archive.4plebs.org"), Collections.singletonList("img.4plebs.org")),
            new ChanSite(Collections.singletonList("fgts.jp"), Collections.singletonList("dat.fgtsi.org"))
    );

    private static final List<String> URL_PIECE_BLACKLIST = Arrays.asList(
            "=http", "http://imgops.com/", "iqdb.org", "saucenao.com"
    );

    private ChanSite chanSite;
    private Boolean generalChanSite = true;

    public ChanRipper(URL url) throws IOException {
        super(url);

        for (ChanSite _chanSite : EXPLICIT_DOMAINS) {
            if (_chanSite.domains.contains(url.getHost())) {
                chanSite = _chanSite;
                generalChanSite = false;
            }
        }

        if (chanSite == null)
            chanSite = new ChanSite(Collections.singletonList(url.getHost()));
    }

    @Override
    public String getHost() {
        String host = this.url.getHost();
        host = host.substring(0, host.lastIndexOf('.'));

        if (host.contains(".")) {
            // Host has subdomain (www)
            host = host.substring(host.lastIndexOf('.') + 1);
        }

        String board = this.url.toExternalForm().split("/")[3];
        return host + "_" + board;
    }

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        try {
            // Attempt to use album title as GID
            Document doc = getFirstPage();
            String subject = doc.select(".post.op > .postinfo > .subject").first().text();
            return getHost() + "_" + getGID(url) + "_" + subject;
        } catch (Exception e) {
            // Fall back to default album naming convention
            LOGGER.warn("Failed to get album title from " + url, e);
        }

        return super.getAlbumTitle(url);
    }

    @Override
    public boolean canRip(URL url) {
        for (ChanSite _chanSite : EXPLICIT_DOMAINS) {
            if (_chanSite.domains.contains(url.getHost()))
                return true;
        }

        // Most chans or 4chan, archive.moe
        return url.toExternalForm().contains("/res/") || url.toExternalForm().contains("/thread/");
    }

    /**
     * For example the archives are all known. (Check 4chan-x)
     * Should be based on the software the specific chan uses.
     * FoolFuuka uses the same (url) layout as 4chan
     */
    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p;
        Matcher m;

        String u = url.toExternalForm();
        if (u.contains("/thread/") || u.contains("/res/")) {
            p = Pattern.compile("^.*\\.[a-z]{1,3}/[a-zA-Z0-9]+/(thread|res)/([0-9]+)(\\.html|\\.php)?.*$");
            m = p.matcher(u);

            if (m.matches())
                return m.group(2);

            // Drawchan is weird, has drawchan.net/dc/dw/res/####.html
            p = Pattern.compile("^.*\\.[a-z]{1,3}/[a-zA-Z0-9]+/[a-zA-Z0-9]+/res/([0-9]+)(\\.html|\\.php)?.*$");
            m = p.matcher(u);

            if (m.matches())
                return m.group(1);
        }

        throw new MalformedURLException("Expected *chan URL formats: .*/@/(res|thread)/####.html Got: " + u);
    }

    @Override
    public String getDomain() {
        return this.url.getHost();
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(this.url).get();
    }

    private boolean isURLBlacklisted(String url) {
        for (String blacklist_item : URL_PIECE_BLACKLIST) {
            if (url.contains(blacklist_item)) {
                LOGGER.debug("Skipping link that contains '" + blacklist_item + "': " + url);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getURLsFromPage(Document page) {
        List<String> imageURLs = new ArrayList<>();
        Pattern p;
        Matcher m;

        for (Element link : page.select("a")) {
            if (!link.hasAttr("href"))
                continue;

            String href = link.attr("href").trim();

            if (isURLBlacklisted(href))
                continue;

            //Check all blacklist items
            Boolean selfHosted = false;
            if (!generalChanSite) {
                for (String cdnDomain : chanSite.cdnDomains) {
                    if (href.contains(cdnDomain))
                        selfHosted = true;
                }
            }

            if (selfHosted || generalChanSite) {
                p = Pattern.compile("^.*\\.(jpg|jpeg|png|gif|apng|webp|tif|tiff|webm)$", Pattern.CASE_INSENSITIVE);
                m = p.matcher(href);

                if (m.matches()) {
                    if (href.startsWith("//"))
                        href = "http:" + href;

                    if (href.startsWith("/"))
                        href = "http://" + this.url.getHost() + href;

                    // Don't download the same URL twice
                    if (imageURLs.contains(href)) {
                        LOGGER.debug("Already attempted: " + href);
                        continue;
                    }

                    imageURLs.add(href);
                    if (isThisATest())
                        break;
                }
            } else {
                //TODO also grab imgur/flickr albums (And all other supported rippers) Maybe add a setting?
            }

            if (isStopped())
                break;
        }
        return imageURLs;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index), "", this.url.toString(), null);
    }
}