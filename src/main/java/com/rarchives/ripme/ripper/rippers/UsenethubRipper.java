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

public class UsenethubRipper extends AbstractHTMLRipper {

    private Document albumDoc = null;

    public UsenethubRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "usenethub";
    }
    @Override
    public String getDomain() {
        return "adult.usenethub.com";
    }
    
    @Override
    public Document getFirstPage() throws IOException {
        if (albumDoc == null) {
            albumDoc = Http.url(url).get();
        }
        return albumDoc;
    }
    
    @Override
    public Document getNextPage(Document doc) throws IOException {
        String nextURL = null;
        for (Element a : doc.select("a.paging_next")) {
            if (a.text().contains("→")) {
                nextURL = "http://adult.usenethub.com" + a.attr("href");
                break;
            }
        }
        if (nextURL == null) {
            throw new IOException("No next page found");
        }
        sleep(1000);
        return Http.url(nextURL).get();
    }
    
    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> imageURLs = new ArrayList<String>();
        for (Element thumb : doc.select("#classic img")) {

        	if (!thumb.hasAttr("src") || !thumb.hasAttr("alt")) {
                continue;
            }

        	if (thumb.attr("alt").length() == 0 && thumb.hasAttr("width") && thumb.hasAttr("height")) {
        		continue;
        	}
            
            String image = thumb.attr("src");
            image = image.replaceAll(
                    "http://usebin.org/image/",
                    "http://usebin.org/source/");
            imageURLs.add(image);
        }
        return imageURLs;
    }
    
    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }

    /*
 	@Override
	public String getGID(URL url) throws MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}
	*/
 	
    @Override
    public String getGID(URL url) throws MalformedURLException {

    	String inUrl = url.toExternalForm();
    	String sUrl;
    	
        if (inUrl.endsWith("/"))
        	sUrl = inUrl.substring(0, inUrl.length()-1);
        else
        	sUrl = inUrl;

        String id = sUrl.substring(sUrl.lastIndexOf('/') + 1);

        if (id != null && id.length() > 0)
        	return id;

        throw new MalformedURLException(
                "Expected usenethub.com gallery formats: "
                        + "imagefap.com/gallery.php?gid=####... or "
                        + "imagefap.com/pictures/####..."
                        + " Got: " + url);
    }
 	

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        try {
            // Attempt to use album title as GID
            String title = getFirstPage().title();
            Pattern p = Pattern.compile("^(.*) \\(Usenet Download\\)$");
            Matcher m = p.matcher(title);
            if (m.matches()) {
                return getHost() + "_" + m.group(1);
            }
        } catch (IOException e) {
            // Fall back to default album naming convention
        }
        return super.getAlbumTitle(url);
    }

}