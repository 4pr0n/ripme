package com.rarchives.ripme.ripper.rippers.video;

import com.rarchives.ripme.ripper.VideoRipper;
import com.rarchives.ripme.utils.Http;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VkRipper extends VideoRipper {

    private static final String HOST = "vk";
    private static final String[] QUALITIES = {"1080", "720", "480", "240"};

    public VkRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://[wm.]*vk\\.com/video[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[wm.]*vk\\.com/video([0-9]+).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException("Expected vk video URL format: vk.com/videos#### Got: " + url);
    }

    @Override
    public void rip() throws IOException {
        LOGGER.info("    Retrieving " + this.url);
        String videoURL = getVideoURLAtPage(this.url.toExternalForm());
        addURLToDownload(new URL(videoURL), HOST + "_" + getGID(this.url));
        waitForThreads();
    }

    public static String getVideoURLAtPage(String url) throws IOException {
        Document doc = Http.url(url).userAgent(USER_AGENT).get();
        String html = doc.outerHtml();
        String videoURL = null;

        for (String quality : QUALITIES) {
            quality = "url" + quality + "\\\":\\\"";
            if (html.contains(quality)) {
                videoURL = html.substring(html.indexOf(quality) + quality.length());
                videoURL = videoURL.substring(0, videoURL.indexOf('"'));
                videoURL = videoURL.replace("\\", "");
                break;
            }
        }

        if (videoURL == null)
            throw new IOException("Could not find video URL at " + url);

        return videoURL;
    }
}