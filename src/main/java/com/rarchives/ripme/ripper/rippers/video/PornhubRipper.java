package com.rarchives.ripme.ripper.rippers.video;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import com.rarchives.ripme.ripper.VideoRipper;
import com.rarchives.ripme.utils.AES;
import com.rarchives.ripme.utils.Http;

public class PornhubRipper extends VideoRipper {

    private static final String HOST = "pornhub";

    public PornhubRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://[wm.]*pornhub\\.com/view_video.php\\?viewkey=[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[wm.]*pornhub\\.com/view_video.php\\?viewkey=([0-9]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected pornhub format:"
                        + "pornhub.com/view_video.php?viewkey=####"
                        + " Got: " + url);
    }

    @Override
    public void rip() throws IOException {
        logger.info("    Retrieving " + this.url.toExternalForm());
        Document doc = Http.url(this.url).get();
        Pattern p = Pattern.compile("^.*'flashvars' : (.*});.*$", Pattern.DOTALL);
        Matcher m = p.matcher(doc.body().html());
        if (m.matches()) {
            String title = null,
                   encryptedUrl = null;
            try {
                JSONObject json = new JSONObject(m.group(1));

                title = json.getString("video_title");
                title = title.replaceAll("\\+", " ");

                encryptedUrl = null;
                for (String quality : new String[] {"quality_1080p", "quality_720p", "quality_480p", "quality_240p"}) {
                    if (json.has(quality)) {
                        encryptedUrl = json.getString(quality);
                        break;
                    }
                }
                if (encryptedUrl == null) {
                    throw new IOException("Unable to find encrypted video URL at " + this.url);
                }
                encryptedUrl = URLDecoder.decode(encryptedUrl, "UTF-8");
                String vidUrl = AES.decrypt(encryptedUrl, title, 256);
                addURLToDownload(new URL(vidUrl), HOST + "_" + getGID(this.url));
            } catch (JSONException e) {
                logger.error("Error while parsing JSON at " + url, e);
                throw e;
            } catch (Exception e) {
                logger.error("Error while retrieving video URL at " + url, e);
                throw new IOException(e);
            }
        }
        else {
            throw new IOException("Failed to download " + this.url + " : could not find 'flashvars'");
        }
        waitForThreads();
    }
}