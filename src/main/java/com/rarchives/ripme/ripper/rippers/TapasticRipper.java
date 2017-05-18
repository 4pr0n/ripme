package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TapasticEpisode {
    protected int index, id;
    protected String title, filename;
    public TapasticEpisode(int index, int id, String title) {
        this.index = index;
        this.id    = id;
        this.title = title;
        this.filename = Utils.filesystemSafe(title);
    }
}

public class TapasticRipper extends AbstractHTMLRipper {

    private List<TapasticEpisode> episodes=new ArrayList<TapasticEpisode>();

    public TapasticRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getDomain() {
        return "tapastic.com";
    }

    @Override
    public String getHost() {
        return "tapastic";
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<String> getURLsFromPage(Document page) {
        List<String> urls = new ArrayList<String>();
        String html = page.data();
        if (!html.contains("episodeList : ")) {
            LOGGER.error("No 'episodeList' found at " + this.url);
            return urls;
        }
        String jsonString = Utils.between(html, "episodeList : ", ",\n").get(0);
        JSONArray json = new JSONArray(jsonString);
        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            TapasticEpisode episode = new TapasticEpisode(i, obj.getInt("id"), obj.getString("title"));
            episodes.add(episode);
            urls.add("http://tapastic.com/episode/" + episode.id);
        }
        return urls;
    }

    @Override
    public void downloadURL(URL url, int index) {
        try {
            Document doc = Http.url(url).get();
            Elements images = doc.select("article.ep-contents img");
            // Find maximum # of images for optimal filename indexing
            int epiLog = (int) (Math.floor(Math.log10(episodes.size())) + 1),
                imgLog = (int) (Math.floor(Math.log10(images.size()  )) + 1);
            for (int i = 0; i < images.size(); i++) {
                String link = images.get(i).attr("src");
                TapasticEpisode episode = episodes.get(index - 1);
                // Build elaborate filename prefix
                StringBuilder prefix = new StringBuilder();
                prefix.append(String.format("ep%0" + epiLog + "d", index));
                prefix.append(String.format("-%0" + imgLog + "dof%0" + imgLog + "d-", i + 1, images.size()));
                prefix.append(episode.filename.replace(" ", "-"));
                prefix.append("-");
                addURLToDownload(new URL(link), prefix.toString());
                if (isThisATest()) {
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[!] Exception while downloading " + url, e);
        }

    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^http://tapastic.com/series/([^/?]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return "series_ " + m.group(1);
        }
        p = Pattern.compile("^http://tapastic.com/episode/([^/?]+).*$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return "ep_" + m.group(1);
        }
        throw new MalformedURLException("Expected tapastic.com URL format: "
                + "tapastic.com/[series|episode]/name - got " + url + " instead");
    }
}
