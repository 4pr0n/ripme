package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.storage.AbstractStorage;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lucas on 31/12/2015.
 */
public class DanbooruRipper extends AlbumRipper {
    private String tags = null;
    private Long page = 1L;
    private String login = null;
    private String apiKey = null;
    private Integer retries = 0;
    private static final Integer retryLimit = 6;
    private static final Integer limit = 100;
    private static final String HOST   = "danbooru";
    private static final String DOMAIN = "danbooru.donmai.us";

    public DanbooruRipper(URL url, AbstractStorage storage) throws IOException {
        super(url, storage);
        login = Utils.getConfigString("danbooru.login", null);
        apiKey = Utils.getConfigString("danbooru.apiKey", null);
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(DOMAIN);
    }

    private String parseTags(URL tagUrl) throws MalformedURLException{
        List<NameValuePair> pairs;
        try {
            pairs = URLEncodedUtils.parse(tagUrl.toURI(), Charset.defaultCharset().toString());
        } catch (URISyntaxException e) {
            throw new MalformedURLException();
        }
        for (NameValuePair pair : pairs) {
            if (pair.getName().equals("tags")){
                return pair.getValue();
            }
        }
        return null;
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        tags = parseTags(url);
        return url;
    }

    @Override
    public void rip() throws IOException {
        tags = parseTags(url);
        while(true) {
            JSONArray itemPage = getPage();
            if (itemPage.length() == 0) {
                break;
            }
            for (Integer i = 0; i < itemPage.length(); i++) {
                JSONObject item = itemPage.getJSONObject(i);
                String path;
                try {
                    if(item.getBoolean("is_deleted")) {
                        continue;
                    }
                    path = item.getString("file_url");
                } catch (JSONException e) {
                    logger.error("Item didn't have file_url: " + item.toString());
                    continue;
                }
                String name = item.getString("md5") + "." + item.getString("file_ext");

                URL url = new URL("https", DOMAIN, path);
                addURLToDownload(url);
            }
            page += 1;
            retries = 0;
        }
        waitForThreads();
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        tags = parseTags(url);
        if (tags != null) {
            return tags;
        } else {
            return "everything";
        }
    }

    public JSONArray getPage() throws IOException {
        if (retries >= retryLimit) {
            logger.error("getPage retried too many times.");
            //throw new IOException();
        }
        logger.info("Getting page " + page.toString());
        LinkedList<NameValuePair> queryParams = new LinkedList<NameValuePair>();
        queryParams.add(new BasicNameValuePair("page", page.toString()));
        queryParams.add(new BasicNameValuePair("limit", limit.toString()));
        if (tags != null) {
            queryParams.add(new BasicNameValuePair("tags", tags));
        }
        if (login != null && apiKey != null) {
            queryParams.add(new BasicNameValuePair("login", login));
            queryParams.add(new BasicNameValuePair("api_key", apiKey));
        }
        String query = URLEncodedUtils.format(queryParams, (Charset) null);
        URL apiUrl = new URL("https", DOMAIN, "/posts.json?" + query);
        try {
            return new JSONArray(new Http(apiUrl).ignoreContentType().response().body());
        } catch (Exception e) {
            retries++;
            sleep((int) (Math.min(Math.pow(4.0, (double) retries) * 1000.0, 1800000.0)));
            return getPage();
        }
    }
}
