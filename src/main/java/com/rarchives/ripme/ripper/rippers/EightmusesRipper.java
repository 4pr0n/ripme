package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Http;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EightmusesRipper extends AbstractHTMLRipper {

    private Document albumDoc = null;
    private Map<String, String> cookies = new HashMap<>();

    public EightmusesRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "8muses";
    }

    @Override
    public String getDomain() {
        return "8muses.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://(www\\.)?8muses\\.com/comix/album/([a-zA-Z0-9\\-_]+).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (!m.matches())
            throw new MalformedURLException("Expected URL format: http://www.8muses.com/index/category/albumname, got: " + url);

        return m.group(m.groupCount());
    }

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        try {
            // Attempt to use album title as GID
            Element titleElement = getFirstPage().select("meta[name=description]").first();
            String title = titleElement.attr("content");
            title = title.replace("A huge collection of free porn comics for adults. Read", "");
            title = title.replace("online for free at 8muses.com", "");
            return getHost() + "_" + title.trim();
        } catch (IOException e) {
            // Fall back to default album naming convention
            LOGGER.info("Unable to find title at " + url, e);
        }

        return super.getAlbumTitle(url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        if (albumDoc == null) {
            Response resp = Http.url(url).response();
            cookies.putAll(resp.cookies());
            albumDoc = resp.parse();
        }

        return albumDoc;
    }

    @Override
    public List<String> getURLsFromPage(Document page) {
        List<String> imageURLs = new ArrayList<>();

        if (!page.select(".preview > span").isEmpty()) {
            // Page contains subalbums (not images)
            Elements albumElements = page.select("a.preview");
            List<Element> albumsList = albumElements.subList(0, albumElements.size());
            Collections.reverse(albumsList);

            // Iterate over elements in reverse order
            for (Element subalbum : albumsList) {
                String subUrl = subalbum.attr("href");
                subUrl = subUrl.replaceAll("\\.\\./", "");

                if (subUrl.startsWith("//"))
                    subUrl = "http:";
                else if (!subUrl.startsWith("http://"))
                    subUrl = "http://www.8muses.com/" + subUrl;

                try {
                    LOGGER.info("Retrieving " + subUrl);
                    sendUpdate(STATUS.LOADING_RESOURCE, subUrl);
                    Document subPage = Http.url(subUrl).get();
                    // Get all images in subalbum, add to list.
                    List<String> subalbumImages = getURLsFromPage(subPage);
                    LOGGER.info("Found " + subalbumImages.size() + " images in subalbum");
                    imageURLs.addAll(subalbumImages);
                } catch (IOException e) {
                    LOGGER.warn("Error while loading subalbum " + subUrl, e);
                    continue;
                }
            }
        } else {
            // Page contains images
            for (Element thumb : page.select(".image")) {
                if (super.isStopped())
                    break;

                // Find thumbnail image source
                String image;

                if (thumb.hasAttr("data-cfsrc"))
                    image = thumb.attr("data-cfsrc");
                else {
                    StringBuilder parentHref = new StringBuilder();
                    parentHref.append(thumb.parent().attr("href"));

                    if (parentHref.toString().isEmpty())
                        continue;
                    if (parentHref.toString().startsWith("/")) {
                        String url = "https://www.8muses.com" + parentHref;
                        parentHref = new StringBuilder();
                        parentHref.append(url);
                    }

                    try {
                        LOGGER.info("Retrieving full-size image location from " + parentHref.toString());
                        image = getFullSizeImage(parentHref.toString());
                    } catch (IOException e) {
                        LOGGER.error("Failed to get full-size image from " + parentHref.toString(), e);
                        continue;
                    }
                }

                // Not hosted on 8muses.
                if (!image.contains("8muses.com"))
                    continue;

                imageURLs.add(image);

                if (isThisATest())
                    break;
            }
        }
        return imageURLs;
    }

    private String getFullSizeImage(String imageUrl) throws IOException {
        sendUpdate(STATUS.LOADING_RESOURCE, imageUrl);
        // Retrieve the webpage  of the image URL
        Document doc = new Http(imageUrl).get();
        // Select the "photo" element from the page (there should only be 1)
        Element fullSizeImage = doc.select(".photo").first();
        return "https://www.8muses.com/data/fu/" + fullSizeImage.children().select("#imageName").attr("value");
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index), "", this.url.toExternalForm(), cookies);
    }

    @Override
    public String getPrefix(int index) {
        return String.format("%03d_", index);
    }

}