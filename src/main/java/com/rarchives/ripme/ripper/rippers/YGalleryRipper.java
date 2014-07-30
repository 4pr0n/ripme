package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Base64;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class YGalleryRipper extends AbstractHTMLRipper {

    private Map<String,String> cookies = new HashMap<String,String>();
    private Map<String,String> imageNames = new HashMap<String,String>();
    private List<String> subGalleries = new ArrayList<String>();
    
	public YGalleryRipper(URL url) throws IOException {
		super(url);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDomain() {
		return "y-gallery.net";
	}

	@Override
	public String getHost() {
		return "y-gallery";
	}

    @Override
    public void rip() throws IOException {
        int index = 0;
        logger.info("Retrieving " + this.url);
        sendUpdate(STATUS.LOADING_RESOURCE, this.url.toExternalForm());
        Document doc = getFirstPage();

        while (doc != null) {
            List<String> imageURLs = getURLsFromPage(doc);

            if (imageURLs.size() == 0) {
                throw new IOException("No images found at " + this.url);
            }

            for (String imageURL : imageURLs) {
                if (isStopped()) {
                    break;
                }
                index += 1;
                downloadURL(new URL(imageURL), index);
            }

            if (isStopped()) {
                break;
            }

            try {
                sendUpdate(STATUS.LOADING_RESOURCE, "next page");
                doc = getNextPage(doc);
            } catch (IOException e) {
                logger.info("Can't get next page: " + e.getMessage());
                break;
            }
        }
        doc = null;
        for (String subGalURL : subGalleries) 
        {
        	doc = getSubgalleryFirstPage(subGalURL);
            while (doc != null) {
                List<String> imageURLs = getURLsFromPage(doc);

                if (imageURLs.size() == 0) {
                    throw new IOException("No images found at " + this.url);
                }

                for (String imageURL : imageURLs) {
                    if (isStopped()) {
                        break;
                    }
                    index += 1;
                    downloadURL(new URL(imageURL), index);
                }

                if (isStopped()) {
                    break;
                }

                try {
                    sendUpdate(STATUS.LOADING_RESOURCE, "next page");
                    doc = getNextPage(doc);
                } catch (IOException e) {
                    logger.info("Can't get next page: " + e.getMessage());
                    break;
                }
            }
        }

        // If they're using a thread pool, wait for it.
        if (getThreadPool() != null) {
            getThreadPool().waitForThreads();
        }
        waitForThreads();
    }
    
	@Override
	public Document getFirstPage() throws IOException {
		Response resp = Http.url("http://www.y-gallery.net/").response();
        cookies = resp.cookies();
		try {
            cookies.putAll(loginToYGallery());
        } catch (Exception e) {
            logger.warn("Failed to login: ", e);
        }
        sleep(500);
        resp = Http.url(url)
                   .referrer("http://www.y-gallery.net/")
                   .cookies(cookies)
                   .response();
        cookies.putAll(resp.cookies());
     
        return resp.parse();
	}
	
    @Override
    public Document getNextPage(Document doc) throws IOException {
    	Elements nextLinks = doc.select("span.smalltext img[src=http://www.y-gallery.net/images/emoticons/nav-next.png]");
    	if (doc.select("span.smalltext img[src=http://www.y-gallery.net/images/emoticons/nav-next.png]").size() == 0) {
            // Last page
    		throw new IOException("No more pages");
        }
    	Element nextLink = nextLinks.first().parent().parent();
        String nextURL = nextLink.attr("href");
        return Http.url(nextURL)
                   .referrer(url)
                   .cookies(cookies)
                   .get();
    }
    
    public Document getSubgalleryFirstPage(String subGalURL) throws IOException {
        return Http.url(subGalURL)
                .referrer(url)
                .cookies(cookies)
                .get();
    }

	@Override
	public List<String> getURLsFromPage(Document doc) {
		String username = doc.select("div.header_title").first().textNodes().get(0).toString().replace(" ", "");
		List<String> imageURLs = new ArrayList<String>();
        Pattern imgRegex = Pattern.compile(".*/files/thumbs/(\\d*)/(\\d*)/(\\d*)\\-(\\d*).jpg");
        for (Element thumb : doc.select("div.gallery_col > div")) {
        	if (isStopped()) {
                break;
            }
            Matcher imgMatcher = imgRegex.matcher(thumb.select("a > img").get(0).attr("src"));
            if (!imgMatcher.matches()) {
                logger.info("Thumbnail parse error for " + thumb.select("a > img").get(0).attr("src"));
                continue;
            }
            String fileName = thumb.select("div > div").first().textNodes().get(0).toString()
            		.replaceAll("  ", "").replaceAll("[\\\\/\\*\\?\\<\\>\\.]", "").replaceAll("\\:", "-").replaceAll("\"", "'").replaceAll("\\|", " ").replaceAll("\\&acute\\;", "'").replaceAll("\\&[A-z0-9]*\\;", "").replace("\n", "").replace("\r", "");
            String firstDir = imgMatcher.group(1),
                secondDir = imgMatcher.group(2),
                imageNum = imgMatcher.group(3),
                imageDate = imgMatcher.group(4);
            String image = "http://www.y-gallery.net/files/data/" + firstDir + "/" + secondDir + "/" + imageNum + "-" + imageDate + ".jpg";
            imageURLs.add(image);
            imageNames.put(imageNum, fileName);
        }
     	
        Pattern galleryRegex = Pattern.compile("^.*y-gallery\\.net/gallery/([a-zA-Z0-9\\-_]+).*/([a-zA-Z0-9\\-_]+).*$");
		for (Element subGal : doc.select("div[style=padding-top: 35px] > a")) {
        	if (isStopped()) {
                break;
            }
            Matcher subGalMatcher = galleryRegex.matcher(subGal.attr("href"));
            if (!subGalMatcher.matches()) {
                logger.info("Subgallery parse error for " + subGal.attr("href"));
                continue;
            }
            subGalleries.add(subGal.attr("href"));
        }
        return imageURLs;
	}

	@Override
	public void downloadURL(URL url, int index) {
		Pattern imgRegex = Pattern.compile(".*/files/data/(\\d*)/(\\d*)/(\\d*)\\-(\\d*).jpg");
        Matcher imgMatcher = imgRegex.matcher(url.toExternalForm());
        imgMatcher.matches();
        String fileName = imageNames.get(imgMatcher.group(3));
		addURLToDownload(url, getPrefix(index) + fileName + " - ");
	}
    
	@Override
	public String getGID(URL url) throws MalformedURLException {
		//Check for subgallery first
		Pattern p1 = Pattern.compile("^.*y-gallery\\.net/gallery/([a-zA-Z0-9\\-_]+)/([a-zA-Z0-9\\-_]+).*$");
		Matcher m1 = p1.matcher(url.toExternalForm());
		if (m1.matches()) {
			return m1.group(1) + "_" + m1.group(2);
		}
        Pattern p = Pattern.compile("^.*y-gallery\\.net/gallery/([a-zA-Z0-9\\-_]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        //Collabs page
        Pattern p2 = Pattern.compile("^.*y-gallery\\.net/collabs/([a-zA-Z0-9\\-_]+).*$");
        Matcher m2 = p2.matcher(url.toExternalForm());
        if (m2.matches()) {
            return m2.group(1) + "_collabs";
        }
        //From others page
        Pattern p3 = Pattern.compile("^.*y-gallery\\.net/fromothers/([a-zA-Z0-9\\-_]+).*$");
        Matcher m3 = p3.matcher(url.toExternalForm());
        if (m3.matches()) {
            return m3.group(1) + "_fromothers";
        }
        throw new MalformedURLException(
                "Expected y-gallery gallery format: "
                        + "y-gallery.net/gallery/USERNAME[/subgallery/]"
                        + " Got: " + url);
	}
	
    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        String u = url.toExternalForm();
        u = u.replaceAll("\\?.*", "");
        return new URL(u);
    }

    private Map<String, String> loginToYGallery() throws IOException {
        // Populate postData fields
        Map<String,String> postData = new HashMap<String,String>();
        //TODO: provide default Username
        String username = Utils.getConfigString("ygallery.username", null); 
        //TODO: provide default password
        String password = Utils.getConfigString("ygallery.password", null);
        if (username == null || password == null) {
            throw new IOException("could not find username or password in config");
        }
        Response resp = Http.url("http://www.y-gallery.net/")
        					.cookies(cookies)
        					.referrer("http://www.y-gallery.net/")
                            .response();
        postData.put("username", username);
        postData.put("password", password);
        postData.put("persistent", "1");

        // Send login request
        resp = Http.url("http://www.y-gallery.net/")
                    .userAgent(USER_AGENT)
                    .data(postData)
                    .cookies(resp.cookies())
                    .method(Method.POST)
                    .response();

        // Assert we are logged in
        if (!resp.parse().select("div#error").isEmpty()) {
        	throw new IOException("Failed to login");
        }
        // We are logged in, save the cookies
        return resp.cookies();
    }
}
