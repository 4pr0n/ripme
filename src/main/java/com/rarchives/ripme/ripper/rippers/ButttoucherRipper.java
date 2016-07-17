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

public class ButttoucherRipper extends AbstractHTMLRipper {

    public ButttoucherRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "butttoucher";
    }

    @Override
    public String getDomain() {
        return "butttoucher.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*butttoucher.com/users/([a-zA-Z0-9_\\-]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException(
                "Expected butttoucher.com gallery format: butttoucher.com/users/<username> Got: " + url
        );
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(this.url).get();
    }

    @Override
    public List<String> getURLsFromPage(Document page) {
        List<String> thumbs = new ArrayList<>();

        for (Element thumb : page.select(".thumb img")) {
            if (!thumb.hasAttr("src"))
                continue;

            String smallImage = thumb.attr("src");
            thumbs.add(smallImage.replace("m.", "."));
        }

        return thumbs;
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }

}