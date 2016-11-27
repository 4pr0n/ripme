package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

public class PahealRipper extends AbstractHTMLRipper {
	private static Map<String, String> cookies = null;
	private static Pattern gidPattern = null;

	public PahealRipper(URL url) throws IOException {
		super(url);
	}

	private static Map<String, String> getCookies() {
		if (cookies == null) {
			cookies = new HashMap<>(1);
			cookies.put("ui-tnc-agreed", "true");
		}
		return cookies;
	}

	@Override
	public String getDomain() {
		return "rule34.paheal.net";
	}

	@Override
	public String getHost() {
		return "paheal";
	}

	@Override
	public Document getFirstPage() throws IOException {
		return Http.url("http://rule34.paheal.net/post/list/" + getTerm(url) + "/1").cookies(getCookies()).get();
	}

	@Override
	public Document getNextPage(Document page) throws IOException {
		for (Element e : page.select("#paginator a")) {
			if ("next".equals(e.text().toLowerCase()))
				return Http.url(e.absUrl("href")).cookies(getCookies()).get();
		}

		return null;
	}

	@Override
	public List<String> getURLsFromPage(Document page) {
		Elements elements = page.select(".shm-thumb.thumb>a").not(".shm-thumb-link");
		List<String> res = new ArrayList<>(elements.size());

		for (Element e : elements)
			res.add(e.absUrl("href"));

		return res;
	}

	@Override
	public void downloadURL(URL url, int index) {
		try {
			String name = url.getPath();
			String ext = ".png";

			name = name.substring(name.lastIndexOf('/') + 1);
			if (name.indexOf('.') >= 0) {
				ext = name.substring(name.lastIndexOf('.'));
				name = name.substring(0, name.length() - ext.length());
			}

			addURLToDownload(url, new File(workingDir.getCanonicalPath() + File.separator + Utils.filesystemSafe(new URI(name).getPath()) + ext));
		} catch (IOException | URISyntaxException ex) {
			Logger.getLogger(PahealRipper.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private String getTerm(URL url) throws MalformedURLException {
		if (gidPattern == null)
			gidPattern = Pattern.compile("^https?://(www\\.)?rule34\\.paheal\\.net/post/list/([a-zA-Z0-9$_.+!*'(),%-]+)(/.*)?(#.*)?$");

		Matcher m = gidPattern.matcher(url.toExternalForm());
		if (m.matches())
			return m.group(2);

		throw new MalformedURLException("Expected paheal.net URL format: rule34.paheal.net/post/list/searchterm - got " + url + " instead");
	}

	@Override
	public String getGID(URL url) throws MalformedURLException {
		try {
			return Utils.filesystemSafe(new URI(getTerm(url)).getPath());
		} catch (URISyntaxException ex) {
			Logger.getLogger(PahealRipper.class.getName()).log(Level.SEVERE, null, ex);
		}

		throw new MalformedURLException("Expected paheal.net URL format: rule34.paheal.net/post/list/searchterm - got " + url + " instead");
	}

}