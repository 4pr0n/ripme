package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GunsmokegamesRipper extends AbstractHTMLRipper {

    public GunsmokegamesRipper(URL url) throws IOException {
    super(url);
    }

    @Override
    public String getHost() {
        return "gunsmokegames";
    }

    @Override
    public String getDomain() {
        return "gunsmokegames.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://gunsmokegames.com/comics/([a-zA-Z0-9_-]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected gunsmokegames.com URL format: " +
                        "gunsmokegames.com/comics/comicName - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<String>();
        // We grab the image from meta because it's easier than trying to parse the rest of the page
        for (Element el : doc.select("div.tiled-gallery-item > a > img")) {
            String imageSource = el.attr("data-orig-file");
            result.add(imageSource);
        }
        return result;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }


}
