package com.rarchives.ripme.utils;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.ripper.rippers.EroShareRipper;
import com.rarchives.ripme.ripper.rippers.ImgurRipper;
import com.rarchives.ripme.ripper.rippers.ImgurRipper.ImgurAlbum;
import com.rarchives.ripme.ripper.rippers.ImgurRipper.ImgurImage;
import com.rarchives.ripme.ripper.rippers.VidbleRipper;
import com.rarchives.ripme.ripper.rippers.video.GfycatRipper;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RipUtils {

    private static final Logger LOGGER = Logger.getLogger(RipUtils.class);
    private static final String IMGUR_COM = "imgur.com";
    private static final String HTTP = "http://";

    private RipUtils() {
    }

    public static List<URL> getFilesFromURL(URL url) {
        List<URL> result = new ArrayList<URL>();

        LOGGER.debug("Checking " + url);
        // Imgur album
        if ((url.getHost().endsWith(IMGUR_COM)) && url.toExternalForm().contains("imgur.com/a/")) {
            try {
                LOGGER.debug("Fetching imgur album at " + url);
                ImgurAlbum imgurAlbum = ImgurRipper.getImgurAlbum(url);
                for (ImgurImage imgurImage : imgurAlbum.images) {
                    LOGGER.debug("Got imgur image: " + imgurImage.url);
                    result.add(imgurImage.url);
                }
            } catch (IOException e) {
                LOGGER.error("[!] Exception while loading album: " + url, e);
            }

            return result;
        } else if (url.getHost().endsWith(IMGUR_COM) && url.toExternalForm().contains(",")) {
            // Imgur image series.
            try {
                LOGGER.debug("Fetching imgur series at " + url);
                ImgurAlbum imgurAlbum = ImgurRipper.getImgurSeries(url);
                for (ImgurImage imgurImage : imgurAlbum.images) {
                    LOGGER.debug("Got imgur image: " + imgurImage.url);
                    result.add(imgurImage.url);
                }
            } catch (IOException e) {
                LOGGER.error("[!] Exception while loading album: " + url, e);
            }
        } else if (url.getHost().endsWith("gfycat.com")) {
            try {
                LOGGER.debug("Fetching gfycat page: " + url);
                String videoURL = GfycatRipper.getVideoURL(url);
                LOGGER.debug("Got gfycat URL: " + videoURL);
                result.add(new URL(videoURL));
            } catch (IOException e) {
                // Do nothing
                LOGGER.warn("Exception while retrieving gfycat page: " + url, e);
            }

            return result;
        } else if (url.toExternalForm().contains("vidble.com/album/") || url.toExternalForm().contains("vidble.com/show/")) {
            try {
                LOGGER.info("Getting vidble album " + url);
                result.addAll(VidbleRipper.getURLsFromPage(url));
            } catch (IOException e) {
                // Do nothing
                LOGGER.warn("Exception while retrieving vidble page: " + url, e);
            }

            return result;
        } else if (url.toExternalForm().contains("eroshare.com")) {
            try {
                LOGGER.info("Getting eroshare album " + url);
                result.addAll(EroShareRipper.getURLs(url));
            } catch (IOException e) {
                // Do nothing
                LOGGER.warn("Exception while retrieving eroshare page: " + url, e);
            }

            return result;
        }

        Pattern p = Pattern.compile("https?://i.reddituploads.com/([a-zA-Z0-9]+)\\?.*");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            LOGGER.info("URL: " + url.toExternalForm());
            String u = url.toExternalForm().replaceAll("&amp;", "&");

            try {
                result.add(new URL(u));
            } catch (MalformedURLException e) {
                LOGGER.error("[!] Not a valid URL: '" + u + "'", e);
            }

            return result;
        }

        // Direct link to image
        p = Pattern.compile("(https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(/\\S*)\\.(jpg|jpeg|gif|png|mp4)(\\?.*)?)");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            try {
                URL singleURL = new URL(m.group(1));
                LOGGER.debug("Found single URL: " + singleURL);
                result.add(singleURL);
                return result;
            } catch (MalformedURLException e) {
                LOGGER.error("[!] Not a valid URL: '" + url + "'", e);
            }
        }

        if (url.getHost().equals(IMGUR_COM) || "m.imgur.com".equals(url.getHost())) {
            try {
                // Fetch the page
                Document doc = Jsoup.connect(url.toExternalForm()).userAgent(AbstractRipper.USER_AGENT).get();

                for (Element el : doc.select("meta")) {
                    if ("twitter:image:src".equals(el.attr("name")) || "twitter:image".equals(el.attr("name"))) {
                        result.add(new URL(el.attr("content")));
                        return result;
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("[!] Error", ex);
            }
        }

        LOGGER.error("[!] Unable to rip URL: " + url);
        return result;
    }

    public static Pattern getURLRegex() {
        return Pattern.compile("(https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(/\\S*))");
    }

    public static String urlFromDirectoryName(String dir) {
        String url = urlFromImgurDirectoryName(dir);

        if (url == null)
            url = urlFromImagefapDirectoryName(dir);
        if (url == null)
            url = urlFromDeviantartDirectoryName(dir);
        if (url == null)
            url = urlFromRedditDirectoryName(dir);
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "bfcakes", "http://www.bcfakes.com/celebritylist/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "butttoucher", "http://butttoucher.com/users/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "cheeby", "http://cheeby.com/u/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "datwin", "http://datw.in/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "drawcrowd", "http://drawcrowd.com/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir.replace("-", "/"), "ehentai", "http://g.e-hentai.org/g/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "fapproved", "http://fapproved.com/users/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "vinebox", "http://finebox.co/u/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "imgbox", "http://imgbox.com/g/", "");
        if (url == null)
            url = urlFromSiteDirectoryName(dir, "modelmayhem", "http://www.modelmayhem.com/", "");

        //if (url == null) url = urlFromSiteDirectoryName(dir, "8muses",      "http://www.8muses.com/index/category/", "");
        return url;
    }

    private static String urlFromSiteDirectoryName(String dir, String site, String before, String after) {
        if (!dir.startsWith(site + "_"))
            return null;

        String directory = dir.substring((site + "_").length());
        return before + directory + after;
    }

    private static String urlFromRedditDirectoryName(String dir) {
        if (!dir.startsWith("reddit_"))
            return null;

        String url = null;
        String[] fields = dir.split("_");

        if ("sub".equals(fields[0]))
            url = "http://reddit.com/r/" + dir;
        else if ("user".equals(fields[0]))
            url = "http://reddit.com/user/" + dir;
        else if ("post".equals(fields[0]))
            url = "http://reddit.com/comments/" + dir;

        return url;
    }

    private static String urlFromImagefapDirectoryName(String dir) {
        if (!dir.startsWith("imagefap"))
            return null;

        String url;
        String directory = dir.substring("imagefap_".length());
        if (NumberUtils.isDigits(directory))
            url = "http://www.imagefap.com/gallery.php?gid=" + directory;
        else
            url = "http://www.imagefap.com/gallery.php?pgid=" + directory;

        return url;
    }

    private static String urlFromDeviantartDirectoryName(String dir) {
        if (!dir.startsWith("deviantart"))
            return null;

        String directory = dir.substring("deviantart_".length());
        String url;

        if (!directory.contains("_")) {
            url = HTTP + directory + ".deviantart.com/";
        } else {
            String[] fields = directory.split("_");
            url = HTTP + fields[0] + ".deviantart.com/gallery/" + fields[1];
        }
        return url;
    }

    private static String urlFromImgurDirectoryName(String dir) {
        if (!dir.startsWith("imgur_"))
            return null;

        String directory = dir;
        if (dir.contains(" "))
            directory = dir.substring(0, dir.indexOf(' '));

        List<String> fields = Arrays.asList(directory.split("_"));
        String album = fields.get(1);
        String url = HTTP;

        if (containsNewOrTop(fields) && (containsWeekMonthYear(fields) || fields.contains("all"))) {
            // Subreddit
            fields.remove(0); // "imgur"
            String sub = "";

            while (fields.size() > 2) {
                if (!"".equals(sub)) {
                    sub += "_";
                }
                sub = fields.remove(0); // Subreddit that may contain "_"
            }

            url += "imgur.com/r/" + sub + "/";
            url += fields.remove(0) + "/";
            url += fields.remove(0);
        } else if (album.contains("-")) {
            // Series of images
            url += "imgur.com/" + album.replaceAll("-", ",");
        } else if (album.length() == 5 || album.length() == 6) {
            // Album
            url += "imgur.com/a/" + album;
        } else {
            // User account
            url += album + ".imgur.com/";
            if (fields.size() > 2) {
                url += fields.get(2);
            }
        }

        return url;
    }

    /**
     * Method to verify, if the options below, contains in the parameter sent
     *
     * @param fields String field list
     * @return True if contains or false if  doesn't contain
     */
    private static boolean containsNewOrTop(List<String> fields) {
        return fields.contains("top") || fields.contains("new");
    }

    /**
     * Method to verify, if the options below, contains in the parameter sent
     *
     * @param fields String field list
     * @return True if contains or false if  doesn't contain
     */
    private static boolean containsWeekMonthYear(List<String> fields) {
        return fields.contains("year") || fields.contains("month") || fields.contains("week");
    }

}