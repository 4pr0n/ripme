package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EroshareRipper extends AbstractHTMLRipper {

    public EroshareRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "eroshare";
    }

    @Override
    public String getDomain() {
        return "eroshare.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://eroshare\\.com/([a-zA-Z0-9]+).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException("Expected eroshare.com URL format: " +
                "eroshare.com/id - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> urls = new ArrayList<String>();

        for (Element div : doc.select(".black-bg")) {
            Element img = div.select("> img").first();
            String url;

            // If null, then it is a video
            if (img == null) {
                Element vid = div.select("video").first().select("source").first();
                url = vid.attr("src");
            } else {
                url = "http:" + img.attr("src");
            }

            // If url is empty, use noscript img
            if (url.equals("http:")) {
                img = div.select("noscript").first().select("img").first();
                url = "http:" + img.attr("src");
            }

            urls.add(url);
        }

        return urls;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }
}
