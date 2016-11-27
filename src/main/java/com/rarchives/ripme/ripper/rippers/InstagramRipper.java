package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractJSONRipper;
import com.rarchives.ripme.utils.Http;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramRipper extends AbstractJSONRipper {

    private String userID;

    private static final String INSTAGRAM_LINK = "http://instagram.com/";

    public InstagramRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "instagram";
    }

    @Override
    public String getDomain() {
        return "instagram.com";
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith("instagram.com");
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://instagram.com/([^/]+)");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException("Unable to find user in " + url);
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*instagram\\.com/([a-zA-Z0-9\\-_.]{3,}).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return new URL(INSTAGRAM_LINK + m.group(1));

        throw new MalformedURLException("Expected username in URL (instagram.com/username and not " + url);
    }

    private String getUserID(URL url) throws IOException {
        Pattern p = Pattern.compile("^https?://instagram\\.com/([^/]+)");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new IOException("Unable to find userID at " + this.url);
    }

    @Override
    public JSONObject getFirstPage() throws IOException {
        userID = getUserID(url);
        String baseURL = INSTAGRAM_LINK + userID + "/media";
        LOGGER.info("Loading " + baseURL);

        try {
            return Http.url(baseURL).getJSON();
        } catch (JSONException e) {
            throw new IOException("Could not get instagram user via: " + baseURL, e);
        }
    }

    @Override
    public JSONObject getNextPage(JSONObject json) throws IOException {
        boolean nextPageAvailable;

        try {
            nextPageAvailable = json.getBoolean("more_available");
        } catch (Exception e) {
            throw new IOException("No additional pages found");
        }

        if (nextPageAvailable) {
            JSONArray items = json.getJSONArray("items");
            JSONObject last_item = items.getJSONObject(items.length() - 1);
            String nextMaxID = last_item.getString("id");

            String baseURL = INSTAGRAM_LINK + userID + "/media/?max_id=" + nextMaxID;
            LOGGER.info("Loading " + baseURL);
            sleep(1000);

            return Http.url(baseURL).getJSON();
        } else
            throw new IOException("No more images found");

    }

    @Override
    public List<String> getURLsFromJSON(JSONObject json) {
        List<String> imageURLs = new ArrayList<>();
        JSONArray datas = json.getJSONArray("items");

        for (int i = 0; i < datas.length(); i++) {
            JSONObject data = (JSONObject) datas.get(i);
            String imageURL;

            if (data.has("videos"))
                imageURL = data.getJSONObject("videos").getJSONObject("standard_resolution").getString("url");
            else if (data.has("images"))
                imageURL = data.getJSONObject("images").getJSONObject("standard_resolution").getString("url");
            else
                continue;

            imageURL = imageURL.replaceAll("scontent.cdninstagram.com/hphotos-", "igcdn-photos-d-a.akamaihd.net/hphotos-ak-");
            imageURL = imageURL.replaceAll("s640x640/", "");
            imageURL = imageURL.replaceAll("\\?ig_cache_key.+$", "");
            imageURLs.add(imageURL);

            if (isThisATest())
                break;
        }

        return imageURLs;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url);
    }

}