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

public class PicasaRipper extends AbstractHTMLRipper {

    private Document albumDoc = null;

    public PicasaRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "picasa";
    }
    @Override
    public String getDomain() {
        return "picasaweb.google.com";
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
        return null;
    }
    
    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> imageURLs = new ArrayList<String>();
        //for (Element thumb : doc.select("#lhid_content img")) 
        for (Element thumb : doc.select("img"))
        {
        	if (!thumb.hasAttr("src")) {
                continue;
            }
        	
        	if (thumb.hasAttr("id") || thumb.hasAttr("width") || thumb.hasAttr("height"))
        		continue;
        	
        	/*
        	String cls = thumb.attr("class");
        	if (cls == null || !cls.equals("goog-icon-list-icon-img"))
        			continue;
        	*/

            String image = thumb.attr("src");
            image = image.replaceAll(
                    "/s128/",
                    "/d/");
            imageURLs.add(image);
        }
        return imageURLs;
    }
    
    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }
    
    @Override
    public String getGID(URL url) throws MalformedURLException {

        Pattern p; Matcher m;

        p = Pattern.compile("^.*picasaweb.google.com/([0-9]+).*$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected picasaweb.google.com gallery formats: "
                        + "picasaweb.google.com/<ID>/... "
                        + " Got: " + url);
    }
 	
    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
    	
    	try {
        	String inUrl = url.toExternalForm();
        	String sUrl;
        	
            if (inUrl.endsWith("/"))
            	sUrl = inUrl.substring(0, inUrl.length()-1);
            else
            	sUrl = inUrl;

            String id = sUrl.substring(sUrl.lastIndexOf('/') + 1);
        	id = id.replaceAll("noredirect=1", "");
        	
            if (id.endsWith("?"))
            	id = id.substring(0, id.length()-1);

        	return getHost() + "_" + getGID(url) + "_" + id;
			
        } catch (Exception e) {
            // Fall back to default album naming convention
        }
    	
    	return super.getAlbumTitle(url);
    }

}