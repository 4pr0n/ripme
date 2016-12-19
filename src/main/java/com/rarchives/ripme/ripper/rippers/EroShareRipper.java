/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Method;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Http;

/**
 *
 * @author losipher
 */
public class EroShareRipper extends AbstractHTMLRipper {
    public static String HOST = "eroshare";

    public EroShareRipper (URL url) throws IOException {
        super(url);
    }
    
    @Override
    public String getDomain() {
            return HOST + ".com";
    }

    @Override
    public String getHost() {
            return HOST;
    }
    
    @Override
    public void downloadURL(URL url, int index){
        addURLToDownload(url);
    }

    private static String cleanURL(String url) {
        if (url.length() == 0) {
            return "";
        }
        if (url.startsWith("//")) {
            url = "https:" + url;
        } else {
            String urlLower = url.toLowerCase();
            if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
                url = "https://" + url;
            }
        }
        return url;
    }

    @Override
    public List<String> getURLsFromPage(Document doc){
        List<String> URLs = new ArrayList<String>();
        //Pictures
        Elements imgs = doc.getElementsByTag("img");
        for (Element img : imgs){
            if (img.hasClass("album-image")){
                String imageURL = img.attr("src");
                imageURL = cleanURL(imageURL);
                if (imageURL.length() > 0) {
                    URLs.add(imageURL);
                }
            }
        }
        //Videos
        Elements vids = doc.getElementsByTag("video");
        for (Element vid : vids){
            if (vid.hasClass("album-video")){
                Elements source = vid.getElementsByTag("source");
                String videoURL = source.first().attr("src");
                videoURL = cleanURL(videoURL);
                if (videoURL.length() > 0) {
                    URLs.add(videoURL);
                }
            }
        }
        
        return URLs;
    }
    
    @Override
    public Document getFirstPage() throws IOException {
        Response resp = Http.url(this.url)
                            .ignoreContentType()
                            .response();

        Document doc = resp.parse();
        
        return doc;
    }
    
    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[w.]*eroshare.com/([a-zA-Z0-9\\-_]+)/?.*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("eroshare album not found in " + url + ", expected https://eroshare.com/album");
    }
    
    public static List<URL> getURLs(URL url) throws IOException{
        
        Response resp = Http.url(url)
                            .ignoreContentType()
                            .response();

        Document doc = resp.parse();

        List<URL> URLs = new ArrayList<URL>();
        //Pictures
        Elements imgs = doc.getElementsByTag("img");
        for (Element img : imgs){
            if (img.hasClass("album-image")){
                String imageURL = img.attr("src");
                imageURL = cleanURL(imageURL);
                if (imageURL.length() > 0) {
                    URLs.add(new URL(imageURL));
                }
            }
        }
        //Videos
        Elements vids = doc.getElementsByTag("video");
        for (Element vid : vids){
            if (vid.hasClass("album-video")){
                Elements source = vid.getElementsByTag("source");
                String videoURL = source.first().attr("src");
                if (videoURL.length() > 0) {
                    URLs.add(new URL(videoURL));
                }
            }
        }
        
        return URLs;
    }

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        String title = HOST;
        Document doc = null;
        try {
            doc = downloadAndSaveHTML(url);
        } catch (IOException e) {
            title += "_" + getGID(url);
            logger.error("Exception retrieving url=" + url + ": " + e.getMessage());
        }
        if (doc != null) {
            // Find username.
            Element element = doc.select(".album-info-container a.avatar").first();
            if (element != null) {
                title += "_" + (element.attr("href").length() > 0 ? element.attr("href").replaceAll("^/u/", "") : "");
            } else {
                logger.warn("No username was found in the contents of url=" + url);
            }
            title += "_" + getGID(url);
            element = doc.select(".album-info-container .center-mobile").first();
            if (element != null) {
                title += "-" + element.text().replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_\\[\\]()\\.-]+", "");
            } else {
                logger.warn("No title was found in the contents of url=" + url);
            }
        } else {
            logger.warn("No username or title could be retrieved for url=" + url);
        }
        title = title.replaceAll("_+$", "");
        return title;
    }
}
