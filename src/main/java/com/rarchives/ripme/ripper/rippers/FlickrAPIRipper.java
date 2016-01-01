package com.rarchives.ripme.ripper.rippers;

import com.github.scribejava.apis.FlickrApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuthService;
import com.rarchives.ripme.ripper.AbstractJSONRipper;
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
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lucas on 31/12/2015.
 */
public class FlickrAPIRipper extends AbstractJSONRipper {
    private String apiKey;
    private String apiSecret;
    private Boolean signed;
    private static final Integer perPage = 500;
    private static final String PROTECTED_RESOURCE_URL = "https://api.flickr.com/services/rest/";
    private Integer page = 1;
    private Integer numPages = null;
    private Integer urlType = -1;
    private String userId;
    private String userAlbumId;
    private String groupId;
    private String galleryId;
    private String title;
    private List<String> urlSizes;
    OAuthService service;
    Token accessToken;

    public FlickrAPIRipper(URL url, AbstractStorage storage) throws IOException {
        super(url, storage);

        urlSizes = new LinkedList<String>();
        urlSizes.add("url_o");
        urlSizes.add("url_h");
        urlSizes.add("url_l");
        urlSizes.add("url_c");
        urlSizes.add("url_z");
        urlSizes.add("url_m");
        urlSizes.add("url_n");
        urlSizes.add("url_s");
        urlSizes.add("url_t");
        urlSizes.add("url_q");
        urlSizes.add("url_sq");

        if (!canRip(url)) {
            throw new IOException("Can't rip.");
        }

        apiKey = Utils.getConfigString("flickr.apiKey", null);
        apiSecret = Utils.getConfigString("flickr.apiSecret", null);

        if (apiKey == null || apiSecret == null) {
            logger.error("No Flickr API key or secret specified.");
            throw new IOException();
        }

        signed = Utils.getConfigBoolean("flickr.signed", false);

        if (signed) {
            String accessTokenToken = Utils.getConfigString("flickr.accessTokenToken", null);
            String accessTokenSecret = Utils.getConfigString("flickr.accessTokenSecret", null);

            service = new ServiceBuilder().provider(FlickrApi.class).apiKey(apiKey).apiSecret(apiSecret).build();

            if (accessTokenToken == null || accessTokenSecret == null) {
                String verifierString = Utils.getConfigString("flickr.verifier", null);
                if (verifierString == null) {
                    Token requestToken = service.getRequestToken();
                    Utils.setConfigString("flickr.requestTokenToken", requestToken.getToken());
                    Utils.setConfigString("flickr.requestTokenSecret", requestToken.getSecret());
                    Utils.saveConfig();
                    logger.error("No access token was specified, get a verifier here and add it to the config as flickr.verifier: "
                            + service.getAuthorizationUrl(requestToken));
                    throw new IOException();
                } else {
                    logger.info("Got verifier!");
                    Verifier verifier = new Verifier(verifierString);
                    Token requestToken = new Token(
                            Utils.getConfigString("flickr.requestTokenToken", null),
                            Utils.getConfigString("flickr.requestTokenSecret", null)
                    );
                    accessToken = service.getAccessToken(requestToken, verifier);
                    Utils.setConfigString("flickr.verifier", null);
                    Utils.setConfigString("flickr.requestTokenToken", null);
                    Utils.setConfigString("flickr.requestTokenSecret", null);
                    Utils.setConfigString("flickr.accessTokenToken", accessToken.getToken());
                    Utils.setConfigString("flickr.accessTokenSecret", accessToken.getSecret());
                    Utils.saveConfig();
                }
            } else {
                accessToken = new Token(accessTokenToken, accessTokenSecret);
            }

            List<NameValuePair> queryParams = new LinkedList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("method", "flickr.test.login"));
            queryParams.add(new BasicNameValuePair("format", "json"));
            queryParams.add(new BasicNameValuePair("nojsoncallback", "1"));
            String urlString = PROTECTED_RESOURCE_URL + "?" + URLEncodedUtils.format(queryParams, (String) null);
            JSONObject resObj = new JSONObject(makeRequest(new URL(urlString)));
            if (resObj.getString("stat").equals("ok")) {
                logger.info("Authenticated Flickr.");
            }
        }
        try {
            urlType();
        } catch (IOException e) {
            logger.error("Couldn't get URL Type");
            e.printStackTrace();
            throw e;
        }

        if (urlType == -1) {
            logger.error("Flickr got a Flickr URL but couldn't parse it.");
            throw new IOException();
        }
    }

    private void urlType() throws MalformedURLException, IOException {
        List<String> methods = new LinkedList<String>();
        methods.add("flickr.urls.lookupUser");
        methods.add("flickr.urls.lookupGallery");
        methods.add("flickr.urls.lookupGroup");
        for (int i = 0; i < methods.size(); i++) {
            String method = methods.get(i);
            List<NameValuePair> queryParams = new LinkedList<NameValuePair>();
            queryParams.add(new BasicNameValuePair("method", method));
            queryParams.add(new BasicNameValuePair("format", "json"));
            queryParams.add(new BasicNameValuePair("nojsoncallback", "1"));
            queryParams.add(new BasicNameValuePair("url", url.toString()));
            String urlString = PROTECTED_RESOURCE_URL + "?" + URLEncodedUtils.format(queryParams, (String) null);
            JSONObject resObj = new JSONObject(makeRequest(new URL(urlString)));
            if (resObj.has("stat") && resObj.getString("stat").equals("ok")) {
                logger.info("Found type with method " + method);
                switch(i) {
                    case 0:
                        JSONObject user = resObj.getJSONObject("user");
                        userId = user.getString("id");
                        if (url.getPath().contains("/albums/")) {
                            urlType = 3;
                            String path = url.getPath();
                            if (path.endsWith("/")) {
                                path = path.substring(0, path.length() - 1);
                            }
                            String[] splitPath = path.split("/");
                            userAlbumId = splitPath[splitPath.length - 1];
                            String newTitle = getAlbumTitle();
                            if (newTitle == null) {
                                logger.error("Couldn't get album info.");
                                throw new IOException();
                            } else {
                                title = newTitle;
                            }
                        } else {
                            urlType = 0;
                            title = user.getJSONObject("username").getString("_content");
                        }
                        return;
                    case 1:
                        JSONObject gallery = resObj.getJSONObject("gallery");
                        galleryId = gallery.getString("id");
                        title = gallery.getJSONObject("title").getString("_content");
                        urlType = 1;
                        return;
                    case 2:
                        JSONObject group = resObj.getJSONObject("group");
                        groupId = group.getString("id");
                        title = group.getJSONObject("groupname").getString("_content");
                        urlType = 2;
                        return;
                }
            }
        }
    }

    private String getAlbumTitle(){
        List<NameValuePair> queryParams = new LinkedList<NameValuePair>();
        queryParams.add(new BasicNameValuePair("format", "json"));
        queryParams.add(new BasicNameValuePair("nojsoncallback", "1"));
        queryParams.add(new BasicNameValuePair("method", "flickr.photosets.getPhotos"));
        queryParams.add(new BasicNameValuePair("user_id", userId));
        queryParams.add(new BasicNameValuePair("photoset_id", userAlbumId));
        String urlString = PROTECTED_RESOURCE_URL + "?" + URLEncodedUtils.format(queryParams, (String) null);
        JSONObject albumInfo;
        try {
             albumInfo = new JSONObject(makeRequest(new URL(urlString)));
        } catch (IOException e) {
            return null;
        }
        return albumInfo.getJSONObject("photoset").getJSONObject("title").getString("_content");
    }

    public String makeRequest(URL url) throws IOException {
        if (signed) {
            OAuthRequest testRequest = new OAuthRequest(Verb.GET, url.toString(), service);
            service.signRequest(accessToken, testRequest);
            Response response = testRequest.send();
            return response.getBody();
        } else {
            return new Http(url).ignoreContentType().response().body();
        }
    }

    @Override
    public String getDomain() {
        return "flickr.com";
    }

    @Override
    public String getHost() {
        return "flickr";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        return title;
    }

    public JSONObject getPage() throws IOException {
        if (numPages != null && page > numPages) {
            return null;
        }
        logger.info("Getting page "+ page);
        List<NameValuePair> queryParams = new LinkedList<NameValuePair>();
        queryParams.add(new BasicNameValuePair("format", "json"));
        queryParams.add(new BasicNameValuePair("nojsoncallback", "1"));
        queryParams.add(new BasicNameValuePair("page", page.toString()));
        queryParams.add(new BasicNameValuePair("per_page", perPage.toString()));
        queryParams.add(new BasicNameValuePair("safe_search", "3"));
        String extrasString = urlSizes.get(0);
        for (String size : urlSizes.subList(1, urlSizes.size())) {
            extrasString += "," + size;
        }
        queryParams.add(new BasicNameValuePair("extras", extrasString));
        switch (urlType) {
            case 0:
                queryParams.add(new BasicNameValuePair("method", "flickr.people.getPhotos"));
                queryParams.add(new BasicNameValuePair("user_id", userId));
                break;
            case 3:
                queryParams.add(new BasicNameValuePair("method", "flickr.photosets.getPhotos"));
                queryParams.add(new BasicNameValuePair("user_id", userId));
                queryParams.add(new BasicNameValuePair("photoset_id", userAlbumId));
                break;
            case 1:
                queryParams.add(new BasicNameValuePair("method", "flickr.galleries.getPhotos"));
                queryParams.add(new BasicNameValuePair("gallery_id", galleryId));
                break;
            case 2:
                queryParams.add(new BasicNameValuePair("method", "flickr.groups.pools.getPhotos"));
                queryParams.add(new BasicNameValuePair("group_id", groupId));
                break;
            default:
                throw new IOException("-1 isn't a valid type.");
        }
        String urlString = PROTECTED_RESOURCE_URL + "?" + URLEncodedUtils.format(queryParams, (String) null);
        page += 1;
        JSONObject response = new JSONObject(makeRequest(new URL(urlString)));
        String rootKey = null;
        for (Object key : response.keySet()) {
            if (!((String) key).equals("stat")) {
                rootKey = (String) key;
            }
        }
        numPages = response.getJSONObject(rootKey).getInt("pages");
        return response;
    }

    @Override
    public JSONObject getFirstPage() throws IOException {
        return getPage();
    }

    @Override
    public JSONObject getNextPage(JSONObject doc) throws IOException {
        return getPage();
    }

    @Override
    public List<String> getURLsFromJSON(JSONObject json) {
        JSONArray photos;
        String rootKey = "photos";
        if (urlType == 3) {
            rootKey = "photoset";
        }
        try {
            photos = json.getJSONObject(rootKey).getJSONArray("photo");
        } catch (JSONException e) {
            logger.error("Couldn't get " + rootKey + ".photos. Message was " + json.toString());
            throw e;
        }
        LinkedList<String> urls = new LinkedList<String>();
        for(Integer i = 0; i < photos.length(); i++) {
            JSONObject item = photos.getJSONObject(i);
            for (String urlSize : urlSizes) {
                if(item.has(urlSize)) {
                    urls.add(item.getString(urlSize));
                    break;
                }
            }
        }
        return urls;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url);
    }
}
