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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyhentaicomicsRipper extends AbstractHTMLRipper {

    private static final String HTTP_MYHENTAICOMICS_COM = "http://myhentaicomics.com/";
    private boolean isTag;

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
        Pattern p = Pattern.compile("^https?://myhentaicomics.com/index.php/([a-zA-Z0-9-]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            isTag = false;
            return m.group(1);
        }

        Pattern pa = Pattern.compile("^https?://myhentaicomics.com/index.php/search\\?q=([a-zA-Z0-9-]*)([a-zA-Z0-9=&]*)?$");
        Matcher ma = pa.matcher(url.toExternalForm());
        if (ma.matches()) {
            isTag = true;
            return ma.group(1);
        }

        Pattern pat = Pattern.compile("^http://myhentaicomics.com/index.php/tag/([0-9]*)/?([a-zA-Z%0-9+?=:]*)?$");
        Matcher mat = pat.matcher(url.toExternalForm());
        if (mat.matches()) {
            isTag = true;
            return mat.group(1);
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

        if (m.matches())
            nextUrl = "http://myhentaicomics.com" + m.group(0);

        if (Objects.equals(nextUrl, ""))
            throw new IOException("No more pages");

        // Sleep for half a sec to avoid getting IP banned
        sleep(500);
        return Http.url(nextUrl).get();
    }

    // This replaces getNextPage when downloading from searchs and tags
    private List<String> getNextAlbumPage(String pageUrl) {
        List<String> albumPagesList = new ArrayList<>();
        int pageNumber = 1;
        albumPagesList.add("http://myhentaicomics.com/index.php/" + pageUrl.split("\\?")[0] + "?page=" + Integer.toString(pageNumber));

        while (true) {
            String urlToGet = "http://myhentaicomics.com/index.php/" + pageUrl.split("\\?")[0] + "?page=" + Integer.toString(pageNumber);
            Document nextAlbumPage;

            try {
                LOGGER.info("Grabbing " + urlToGet);
                nextAlbumPage = Http.url(urlToGet).get();
            } catch (IOException e) {
                LOGGER.warn("Failed to log link in Jsoup", e);
                nextAlbumPage = null;
            }

            if (nextAlbumPage != null) {
                Element elem = nextAlbumPage.select("a.ui-icon-right").first();
                String nextPage = elem.attr("href");
                pageNumber = pageNumber + 1;

                if (Objects.equals(nextPage, "")) {
                    LOGGER.info("Got " + pageNumber + " pages");
                    break;
                } else {
                    LOGGER.info(nextPage);
                    albumPagesList.add(nextPage);
                    LOGGER.info("Adding " + nextPage);
                }
            }
        }
        return albumPagesList;
    }


    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<>();
        List<String> pagesToRip;

        // Checks if this is a comic page or a page of albums
        if (doc.toString().contains("class=\"g-item g-album\"")) {
            for (Element elem : doc.select("li.g-album > a")) {
                String link = elem.attr("href");
                LOGGER.info("Grabbing album " + link);

                pagesToRip = getNextAlbumPage(link);
                LOGGER.info(pagesToRip);
                for (String element : pagesToRip) {
                    Document albumDoc;

                    try {
                        LOGGER.info("grabbing " + element + " with jsoup");
                        boolean startsWithhttp = element.startsWith("http");

                        if (!startsWithhttp)
                            albumDoc = Http.url(HTTP_MYHENTAICOMICS_COM + element).get();
                        else
                            albumDoc = Http.url(element).get();
                    } catch (IOException e) {
                        LOGGER.warn("Failed to log link in Jsoup", e);
                        albumDoc = null;
                    }

                    if (albumDoc != null)
                        result = searchImages(albumDoc, result);
                }

            }
        } else
            result = searchImages(doc, result);

        return result;
    }

    private List<String> searchImages(Document document, List<String> result) {
        for (Element element : document.select("img")) {
            String imageSource = element.attr("src");
            // This bool is here so we don't try and download the site logo
            boolean b = imageSource.startsWith("http");

            if (!b) {
                // We replace thumbs with resizes so we can the full sized images
                imageSource = imageSource.replace("thumbs", "resizes");
                result.add(HTTP_MYHENTAICOMICS_COM + imageSource);
            }
        }

        return result;
    }

    @Override
    public void downloadURL(URL url, int index) {
        String urlString = url.toExternalForm();
        urlString = urlString.replace("%20", "_");
        urlString = urlString.replace("%27", "");
        urlString = urlString.replace("%28", "_");
        urlString = urlString.replace("%29", "_");
        urlString = urlString.replace("%2C", "_");

        if (isTag) {
            LOGGER.info("Downloading from a tag or search");
            addURLToDownload(url, getPrefix(index), urlString.split("/")[6]);
        } else
            addURLToDownload(url, getPrefix(index));

    }

}