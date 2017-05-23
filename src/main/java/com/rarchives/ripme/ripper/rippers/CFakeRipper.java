package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;

public class CFakeRipper extends AbstractHTMLRipper {

    public CFakeRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "cfake";
    }
    @Override
    public String getDomain() {
        return "cfake.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://cfake\\.com/picture/([a-zA-Z0-9]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected cfake.com URL format: " +
                        "cfake.com/picture/albumid - got " + url + "instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }
    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> imageURLs = new ArrayList<String>();
        for (Element thumb : doc.select("div.boxed-content > a > img")) {
            String image = thumb.attr("src")
                                .replaceAll("[-a-zA-Z0-9.]+s.imgbox.com",
                                            "i.imgbox.com");
            imageURLs.add(image);
        }
        return imageURLs;
    }
    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }
}
