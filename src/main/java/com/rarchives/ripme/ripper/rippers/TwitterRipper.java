package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterRipper extends AlbumRipper {

    private static final String DOMAIN = "twitter.com";
    private static final String HOST = "twitter";

    private static final int MAX_REQUESTS = Utils.getConfigInteger("twitter.max_requests", 10);
    private static final int WAIT_TIME = 2000;
    private static final String AUTHORIZATION = "Authorization";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded;charset=UTF-8";
    private static final String USER_AGENT_KEY = "User-agent";
    private static final String USER_AGENT_VALUE = "ripe and zipe";
    private static final String QUOT = "&quot;";

    // Base 64 of consumer key : consumer secret
    private String authKey;
    private String accessToken;

    private enum ALBUM_TYPE {
        ACCOUNT,
        SEARCH
    }

    private ALBUM_TYPE albumType;
    private String searchText;
    private String accountName;

    public TwitterRipper(URL url) throws IOException {
        super(url);
        authKey = Utils.getConfigString("twitter.auth", null);

        if (authKey == null)
            throw new IOException("Could not find twitter authentication key in configuration");
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(DOMAIN);
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        // https://twitter.com/search?q=from%3Apurrbunny%20filter%3Aimages&src=typd
        Pattern p = Pattern.compile("^https?://(m\\.)?twitter\\.com/search\\?q=([a-zA-Z0-9%\\-_]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches()) {
            albumType = ALBUM_TYPE.SEARCH;
            searchText = m.group(2);
            return url;
        }

        p = Pattern.compile("^https?://(m\\.)?twitter\\.com/([a-zA-Z0-9\\-_]{1,}).*$");
        m = p.matcher(url.toExternalForm());

        if (m.matches()) {
            albumType = ALBUM_TYPE.ACCOUNT;
            accountName = m.group(2);
            return url;
        }

        throw new MalformedURLException("Expected username or search string in url: " + url);
    }

    private void getAccessToken() throws IOException {
        Document doc = Http.url("https://api.twitter.com/oauth2/token")
                .ignoreContentType().header(AUTHORIZATION, "Basic " + authKey)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .header(USER_AGENT_KEY, USER_AGENT_VALUE).data("grant_type", "client_credentials").post();

        String body = doc.body().html().replaceAll(QUOT, "\"");

        try {
            JSONObject json = new JSONObject(body);
            accessToken = json.getString("access_token");
        } catch (JSONException e) {
            // Fall through
            throw new IOException("Failure while parsing JSON: " + body, e);
        }
    }

    private void checkRateLimits(String resource, String api) throws IOException {
        Document doc = Http.url("https://api.twitter.com/1.1/application/rate_limit_status.json?resources=" + resource)
                .ignoreContentType().header(AUTHORIZATION, "Bearer " + accessToken)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .header(USER_AGENT_KEY, USER_AGENT_VALUE).get();

        String body = doc.body().html().replaceAll(QUOT, "\"");

        try {
            JSONObject json = new JSONObject(body);
            JSONObject stats = json.getJSONObject("resources").getJSONObject(resource).getJSONObject(api);

            int remaining = stats.getInt("remaining");
            LOGGER.info("    Twitter " + resource + " calls remaining: " + remaining);

            if (remaining < 20) {
                LOGGER.error("Twitter API calls exhausted: " + stats.toString());
                throw new IOException("Less than 20 API calls remaining; not enough to rip.");
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException: ", e);
            throw new IOException("Error while parsing JSON: " + body, e);
        }
    }

    private String getApiURL(Long maxID) {
        StringBuilder req = new StringBuilder();
        if (albumType == ALBUM_TYPE.ACCOUNT) {
            req.append("https://api.twitter.com/1.1/statuses/user_timeline.json").append("?screen_name=")
                    .append(this.accountName).append("&include_entities=true")
                    .append("&exclude_replies=true").append("&trim_user=true")
                    .append("&include_rts=false").append("&count=" + 200);

        } else if (albumType == ALBUM_TYPE.SEARCH) {
            req.append("https://api.twitter.com/1.1/search/tweets.json").append("?q=")
                    .append(this.searchText).append("&include_entities=true")
                    .append("&result_type=recent").append("&count=100");

        }

        if (maxID > 0)
            req.append("&max_id=").append(Long.toString(maxID));

        return req.toString();
    }

    private List<JSONObject> getTweets(String url) throws IOException {
        List<JSONObject> tweets = new ArrayList<>();
        LOGGER.info("    Retrieving " + url);

        Document doc = Http.url(url).ignoreContentType()
                .header(AUTHORIZATION, "Bearer " + accessToken)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .header(USER_AGENT_KEY, USER_AGENT_VALUE).get();

        String body = doc.body().html().replaceAll(QUOT, "\"");
        Object jsonObj = new JSONTokener(body).nextValue();
        JSONArray statuses;

        if (jsonObj instanceof JSONObject) {
            JSONObject json = (JSONObject) jsonObj;

            if (json.has("errors")) {
                String msg = json.getJSONObject("errors").getString("message");
                throw new IOException("Twitter responded with errors: " + msg);
            }

            statuses = json.getJSONArray("statuses");
        } else
            statuses = (JSONArray) jsonObj;

        for (int i = 0; i < statuses.length(); i++)
            tweets.add((JSONObject) statuses.get(i));

        return tweets;
    }

    private boolean parseTweet(JSONObject tweet) throws MalformedURLException {
        if (!tweet.has("entities")) {
            LOGGER.error("XXX Tweet doesn't have entitites");
            return false;
        }

        JSONObject entities = tweet.getJSONObject("entities");

        if (entities.has("media")) {
            JSONArray medias = entities.getJSONArray("media");
            StringBuilder url = new StringBuilder();
            JSONObject media;

            for (int i = 0; i < medias.length(); i++) {
                media = (JSONObject) medias.get(i);
                url.append(media.getString("media_url"));

                if (url.toString().contains(".twimg.com/")) {
                    url.append(":orig");
                    addURLToDownload(new URL(url.toString()));
                    return true;
                } else
                    LOGGER.debug("Unexpected media_url: " + url.toString());
            }
        }

        /*
        if (entities.has("urls")) {
            JSONArray urls = entities.getJSONArray("urls");
            JSONObject url;
            for (int i = 0; i < urls.length(); i++) {
                url = (JSONObject) urls.get(i);
                if (url.get("expanded_url") != null) {
                    handleTweetedURL(url.getString("url"));
                } else {
                    handleTweetedURL(url.getString("expanded_url"));
                }
            }
        }
        */
        return false;
    }

    @Override
    public void rip() throws IOException {
        getAccessToken();

        if (albumType == ALBUM_TYPE.ACCOUNT)
            checkRateLimits("statuses", "/statuses/user_timeline");
        else if (albumType == ALBUM_TYPE.SEARCH)
            checkRateLimits("search", "/search/tweets");

        Long lastMaxID = 0L;
        int parsedCount = 0;

        for (int i = 0; i < MAX_REQUESTS; i++) {
            List<JSONObject> tweets = getTweets(getApiURL(lastMaxID - 1));

            if (tweets.isEmpty()) {
                LOGGER.info("   No more tweets found.");
                break;
            }

            LOGGER.debug("Twitter response #" + (i + 1) + " Tweets:\n" + tweets);
            if (tweets.size() == 1 && Objects.equals(lastMaxID.toString(), tweets.get(0).getString("id_str"))) {
                LOGGER.info("   No more tweet found.");
                break;
            }

            for (JSONObject tweet : tweets) {
                lastMaxID = tweet.getLong("id");
                if (parseTweet(tweet))
                    parsedCount++;

                if (isStopped() || (isThisATest() && parsedCount > 0))
                    break;
            }

            if (isStopped() || (isThisATest() && parsedCount > 0))
                break;

            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                LOGGER.error("[!] Interrupted while waiting to load more results", e);
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
        if (albumType == ALBUM_TYPE.ACCOUNT) {
            return "account_" + accountName;
        } else if (albumType == ALBUM_TYPE.SEARCH) {
            StringBuilder gid = new StringBuilder();

            for (int i = 0; i < searchText.length(); i++) {
                char c = searchText.charAt(i);
                // Ignore URL-encoded chars
                if (c == '%') {
                    gid.append('_');
                    i += 2;
                    continue;
                    // Ignore non-alphanumeric chars
                } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                    gid.append(c);
                }
            }
            return "search_" + gid.toString();
        }
        throw new MalformedURLException("Could not decide type of URL (search/account): " + url);
    }

}