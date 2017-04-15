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

public class ImagearnRipper extends AbstractHTMLRipper {

    public ImagearnRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "imagearn";
    }

    @Override
    public String getDomain() {
        return "imagearn.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*imagearn.com/{1,}gallery.php\\?id=([0-9]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException(
                "Expected imagearn.com gallery formats: imagearn.com/gallery.php?id=####...  Got: " + url
        );
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*imagearn.com/{1,}image.php\\?id=[0-9]{1,}.*$");
        Matcher m = p.matcher(url.toExternalForm());
        URL urlUpdated = url;

        if (m.matches()) {
            // URL points to imagearn *image*, not gallery
            try {
                urlUpdated = getGalleryFromImage(url);
            } catch (Exception e) {
                LOGGER.error("[!] " + e.getMessage(), e);
            }
        }
        return urlUpdated;
    }

    private URL getGalleryFromImage(URL url) throws IOException {
        Document doc = Http.url(url).get();
        URL urlUpdated = url;

        for (Element link : doc.select("a[href~=^gallery\\.php.*$]")) {
            LOGGER.info("LINK: " + link.toString());
            if (link.hasAttr("href") && link.attr("href").contains("gallery.php")) {
                urlUpdated = new URL("http://imagearn.com/" + link.attr("href"));
                LOGGER.info("[!] Found gallery from given link: " + urlUpdated);
                return urlUpdated;
            }
        }
        throw new IOException("Failed to find gallery at URL " + urlUpdated);
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> imageURLs = new ArrayList<>();
        for (Element thumb : doc.select("img.border")) {
            String image = thumb.attr("src");
            image = image.replaceAll("thumbs[0-9]*\\.imagearn\\.com/", "img.imagearn.com/imags/");
            imageURLs.add(image);
        }
        return imageURLs;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
        sleep(1000);
    }

}