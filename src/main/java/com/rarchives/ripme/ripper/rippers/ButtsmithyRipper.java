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

public class ButtsmithyRipper extends AbstractHTMLRipper {

    public ButtsmithyRipper(URL url) throws IOException {
    super(url);
    }

    @Override
    public String getHost() {
        return "buttsmithy";
    }

    @Override
    public String getDomain() {
        return "buttsmithy.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://buttsmithy.com/archives/comic/([a-zA-Z0-9-]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected buttsmithy.com URL format: " +
                        "https?://buttsmithy.com/archives/comic/ - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public Document getNextPage(Document doc) throws IOException {
        // Find next page
        String nextUrl = "";
        // We use omic-nav-next to find the next page
        // TODO Should comic-nav-next be in the default getNextPage? It seems pretty common
        Element elem = doc.select("a.comic-nav-next").first();
            if (elem == null) {
                throw new IOException("No more pages");
            }
            String nextPage = elem.attr("href");
            // Sleep for half a sec to avoid getting IP banned
            sleep(500);
            return Http.url(nextPage).get();
        }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<String>();
        // We grab the image from meta because it's easier than trying to parse the rest of the page
        for (Element el : doc.select("meta[property=og:image]")) {
            String imageSource = el.attr("content");
            result.add(imageSource);
        }
        return result;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }


}
