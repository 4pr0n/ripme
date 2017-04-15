package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Http;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VineRipper extends AlbumRipper {

    private static final String DOMAIN = "vine.co";
    private static final String HOST = "vine";

    public VineRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(DOMAIN);
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return new URL("http://vine.co/u/" + getGID(url));
    }

    @Override
    public void rip() throws IOException {
        int page = 0;
        String baseURL = "https://vine.co/api/timelines/users/" + getGID(this.url);
        JSONObject json;

        while (true) {
            page++;
            String theURL = baseURL;
            if (page > 1)
                theURL += "?page=" + page;

            try {
                LOGGER.info("    Retrieving " + theURL);
                sendUpdate(STATUS.LOADING_RESOURCE, theURL);
                json = Http.url(theURL).getJSON();
            } catch (HttpStatusException e) {
                LOGGER.debug("Hit end of pages at page " + page, e);
                break;
            }

            JSONArray records = json.getJSONObject("data").getJSONArray("records");

            for (int i = 0; i < records.length(); i++) {
                String videoURL = records.getJSONObject(i).getString("videoUrl");
                addURLToDownload(new URL(videoURL));

                if (isThisATest())
                    break;
            }
            if (isThisATest())
                break;

            if (records.length() == 0) {
                LOGGER.info("Zero records returned");
                break;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                LOGGER.error("[!] Interrupted while waiting to load next page", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        waitForThreads();
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://(www\\.)?vine\\.co/u/([0-9]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (!m.matches())
            throw new MalformedURLException("Expected format: http://vine.co/u/######");

        return m.group(m.groupCount());
    }

}