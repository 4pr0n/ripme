package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadThreadPool;
import com.rarchives.ripme.utils.Base64;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuraffinityRipper extends AbstractHTMLRipper {

    static Map<String, String> cookies = null;
    private static final String URL_BASE = "https://www.furaffinity.net";

    // Thread pool for finding direct image links from "image" pages (html)
    private DownloadThreadPool furaffinityThreadPool = new DownloadThreadPool("furaffinity");

    public FuraffinityRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public DownloadThreadPool getThreadPool() {
        return furaffinityThreadPool;
    }

    @Override
    public String getDomain() {
        return "furaffinity.net";
    }

    @Override
    public String getHost() {
        return "furaffinity";
    }

    @Override
    public boolean hasDescriptionSupport() {
        return true;
    }

    @Override
    public Document getFirstPage() throws IOException {
        if (cookies == null || cookies.isEmpty())
            login();

        return Http.url(url).cookies(cookies).get();
    }

    private void login() throws IOException {
        String user = new String(Base64.decode("cmlwbWU="));
        String pass = new String(Base64.decode("cmlwbWVwYXNzd29yZA=="));

        Response loginPage = Http.url(URL_BASE + "/login/").referrer(URL_BASE).response();
        cookies = loginPage.cookies();

        Map<String, String> formData = new HashMap<>();
        formData.put("action", "login");
        formData.put("retard_protection", "1");
        formData.put("name", user);
        formData.put("pass", pass);
        formData.put("login", "Login toÂ FurAffinity");

        Response doLogin = Http.url(URL_BASE + "/login/?ref=" + url).referrer(URL_BASE + "/login/")
                .cookies(cookies).data(formData).method(Method.POST).response();
        cookies.putAll(doLogin.cookies());
    }

    @Override
    public Document getNextPage(Document doc) throws IOException {
        // Find next page
        Elements nextPageUrl = doc.select("td[align=right] form");
        if (nextPageUrl.isEmpty())
            throw new IOException("No more pages");

        String nextUrl = URL_BASE + nextPageUrl.first().attr("action");

        sleep(500);
        Document nextPage = Http.url(nextUrl).cookies(cookies).get();

        Elements hrefs = nextPage.select("div#no-images");
        if (hrefs.isEmpty())
            throw new IOException("No more pages");

        return nextPage;
    }

    @Override
    public List<String> getURLsFromPage(Document page) {
        List<String> urls = new ArrayList<>();
        Elements urlElements = page.select("b[id^=sid_]");

        for (Element e : urlElements)
            urls.add(URL_BASE + e.select("a").first().attr("href"));

        return urls;
    }

    @Override
    public List<String> getDescriptionsFromPage(Document page) {
        List<String> urls = new ArrayList<>();
        Elements urlElements = page.select("b[id^=sid_]");

        for (Element e : urlElements) {
            urls.add(URL_BASE + e.select("a").first().attr("href"));
            LOGGER.debug("Desc2 " + URL_BASE + e.select("a").first().attr("href"));
        }

        return urls;
    }

    @Override
    public int descSleepTime() {
        return 400;
    }

    @Override
    public String getDescription(String page) {
        try {
            // Fetch the image page
            Response resp = Http.url(page)
                    .referrer(this.url)
                    .cookies(cookies)
                    .response();
            cookies.putAll(resp.cookies());

            // Try to find the description
            Elements els = resp.parse().select("td[class=alt1][width=\"70%\"]");
            if (els.isEmpty()) {
                LOGGER.debug("No description at " + page);
                throw new IOException("No description found");
            }

            LOGGER.debug("Description found!");
            Document documentz = resp.parse();
            Element ele = documentz.select("td[class=alt1][width=\"70%\"]").get(0); // This is where the description is.
            // Would break completely if FurAffinity changed site layout.
            documentz.outputSettings(new Document.OutputSettings().prettyPrint(false));
            ele.select("br").append("\\n");
            ele.select("p").prepend("\\n\\n");
            LOGGER.debug("Returning description at " + page);
            String tempPage = Jsoup.clean(ele.html().replaceAll("\\\\n", System.getProperty("line.separator")), "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
            Elements titles = documentz.select("td[class=\"cat\"][valign=\"top\"] > b");

            if (titles.isEmpty())
                throw new IOException("No title found");

            Element title = titles.get(0);
            String tempText = title.text();
            return tempText + "\n" + tempPage; // Overridden saveText takes first line and makes it the file name.
        } catch (IOException ioe) {
            LOGGER.info("Failed to get description " + page + " : '" + ioe.getMessage() + "'", ioe);
            return null;
        }
    }

    @Override
    public boolean saveText(URL url, String subdirectory, String text, int index) {
        //TODO Make this better please?
        try {
            stopCheck();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }

        String newText = "";
        File saveFileAs;
        String saveAs = text.split("\n")[0];

        for (int i = 1; i < text.split("\n").length; i++)
            newText = newText.replace("\\", "").replace("/", "").replace("~", "") + "\n" + text.split("\n")[i];

        try {
            if (!subdirectory.trim().isEmpty())
                subdirectory = File.separator + subdirectory;

            int o = url.toString().lastIndexOf('/') - 1;
            String test = url.toString().substring(url.toString().lastIndexOf('/', o) + 1);
            test = test.replace("/", "");
            // This is probably not the best way to do this.
            test = test.replace("\\", "");
            // CLOSE ENOUGH!
            saveFileAs = new File(workingDir.getCanonicalPath() + subdirectory + File.separator + getPrefix(index)
                    + saveAs + " " + test + ".txt");
            // Write the file
            FileOutputStream out = new FileOutputStream(saveFileAs);
            out.write(text.getBytes());
            out.close();
        } catch (IOException e) {
            LOGGER.error("[!] Error creating save file path for description '" + url + "':", e);
            return false;
        }

        LOGGER.debug("Downloading " + url + "'s description to " + saveFileAs);
        if (!saveFileAs.getParentFile().exists()) {
            LOGGER.info("[+] Creating directory: " + Utils.removeCWD(saveFileAs.getParent()));
            saveFileAs.getParentFile().mkdirs();
        }
        return true;
    }

    @Override
    public void downloadURL(URL url, int index) {
        furaffinityThreadPool.addThread(new FuraffinityDocumentThread(url));
        sleep(250);
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://www\\.furaffinity\\.net/gallery/([-_.0-9a-zA-Z]+).*$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches())
            return m.group(1);

        throw new MalformedURLException(
                "Expected furaffinity.net URL format: www.furaffinity.net/gallery/username  - got " + url + " instead"
        );
    }

    private class FuraffinityDocumentThread extends Thread {
        private URL url;

        public FuraffinityDocumentThread(URL url) {
            super();
            this.url = url;
        }

        @Override
        public void run() {
            try {
                Document doc = Http.url(url).cookies(cookies).get();
                // Find image
                Elements donwloadLink = doc.select("div.alt1 b a[href^=//d.facdn.net/]");

                if (donwloadLink.isEmpty()) {
                    LOGGER.warn("Could not download " + this.url);
                    return;
                }

                String link = "http:" + donwloadLink.first().attr("href");
                LOGGER.info("Found URL " + link);
                addURLToDownload(new URL(link), "", "", url.toExternalForm(), cookies);
            } catch (IOException e) {
                LOGGER.error("[!] Exception while loading/parsing " + this.url, e);
            }
        }
    }

}