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

public class MyhentaicomicsRipper extends AbstractHTMLRipper {

    public MyhentaicomicsRipper(URL url) throws IOException {
    super(url);
    }

    @Override
    public String getHost() {
        return "myhentaicomics";
    }

    @Override
    public String getDomain() {
        return "myhentaicomics.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://myhentaicomics.com/index.php/([a-zA-Z0-9-]*)$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected myhentaicomics.com URL format: " +
                        "myhentaicomics.com/index.php/albumName - got " + url + " instead");
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
        Element elem = doc.select("a.ui-icon-right").first();
            String nextPage = elem.attr("href");
            Pattern p = Pattern.compile("/index.php/[a-zA-Z0-9_-]*\\?page=\\d");
            Matcher m = p.matcher(nextPage);
            if (m.matches()) {
                nextUrl = "http://myhentaicomics.com" + m.group(0);
                }
            if (nextUrl == "") {
                throw new IOException("No more pages");
            }
            // Sleep for half a sec to avoid getting IP banned
            sleep(500);
            return Http.url(nextUrl).get();
        }


    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<String>();
        for (Element el : doc.select("img")) {
            String imageSource = el.attr("src");
            // This bool is here so we don't try and download the site logo
            boolean b = imageSource.startsWith("http");
            if (b == false) {
            // We replace thumbs with resizes so we can the full sized images
            imageSource = imageSource.replace("thumbs", "resizes");
            result.add("http://myhentaicomics.com/" + imageSource);
            }
        }
        return result;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }


}
