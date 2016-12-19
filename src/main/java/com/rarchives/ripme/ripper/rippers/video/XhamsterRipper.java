package com.rarchives.ripme.ripper.rippers.video;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rarchives.ripme.utils.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.VideoRipper;
import com.rarchives.ripme.utils.Http;

public class XhamsterRipper extends VideoRipper {

    private static final String HOST = "xhamster";

    public XhamsterRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://.*xhamster\\.com/movies/[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }
    
    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        String gid = url.toExternalForm().replaceFirst("^https?://.*" + HOST + "\\.com/movies/([0-9]+)/.*$", "$1");
        if (gid.length() == 0) {
            throw new MalformedURLException(
                "Expected xhamster format:"
                + "xhamster.com/movies/####"
                + " Got: " + url);

        }
        return gid;
    }

    @Override
    public void rip() throws IOException {
        logger.info("Retrieving " + url);
        Document doc = Http.url(url).header("User-Agent", USER_AGENT).referrer("http://" + HOST + ".com/").cookies(Utils.getCookies(HOST)).get();
        Elements videos = doc.select("a.mp4Thumb");
        if (videos.size() == 0) {
            throw new IOException("Could not find Embed code at " + url);
        }
        String vidUrl = videos.attr("href");
        addURLToDownload(new URL(vidUrl), getVideoName(), "", url.toExternalForm(), Utils.getCookies(HOST));
        waitForThreads();
    }

    private String getVideoName() throws IOException {
        String title = HOST + "_";
        try {
            Document doc = Http.url(url).header("User-Agent", USER_AGENT).referrer(url).cookies(Utils.getCookies(HOST)).get();
            Element link = doc.select("#videoUser a").first();
            if (link != null) {
                title += link.text() + "_";
            }
        } catch (IOException e) {
            logger.error("Exception retrieving url=" + url + ": " + e.getMessage());
            try {
                title += getGID(url);
            } catch (MalformedURLException malformedEx) {
                throw new IOException(malformedEx.getMessage());
            }
        }
        title += url.toExternalForm()
            .replaceFirst("^https?://.*" + HOST + "\\.com/movies/([0-9]+)/([^\\.]+).*$", "$1_$2_")
            .replaceAll("_+", "_");
        return title;
    }

}
