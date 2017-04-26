package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.rippers.NatalieMuRipper;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NatalieMuRipperTest extends RippersTest {

    @Test(expected = MalformedURLException.class)
    public void natalieMuURLFailuresTest() throws Exception {
        List<URL> failURLs = new ArrayList<>();
        failURLs.add(new URL("http://cdn2.natalie.mu/"));

        // URLs that should not work
        for (URL url : failURLs) {
            try {
                new NatalieMuRipper(url);
                fail("Instantiated ripper for URL that should not work: " + url);
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Unable to rip"));
                throw e;
            }
        }
    }

    @Test
    public void natalieMuURLSuccessTest() throws Exception {
        List<URL> passURLs = new ArrayList<>();
        // URLs that should work
        passURLs.add(new URL("http://natalie.mu/music/news/140367"));
        passURLs.add(new URL("http://cdn2.natalie.mu/music/news/140411"));
        passURLs.add(new URL("http://cdn2.natalie.mu/music/gallery/show/news_id/140411/image_id/369655"));
        passURLs.add(new URL("http://natalie.mu/music/gallery/show/news_id/139146/image_id/365218"));

        for (URL url : passURLs) {
            NatalieMuRipper ripper = new NatalieMuRipper(url);
            ripper.setup();
            assertTrue(ripper.canRip(url));

            assertNotNull("Ripper for " + url + " did not have a valid working directory.", ripper.getWorkingDir());
            deleteDir(ripper.getWorkingDir());
        }
    }

    @Test
    public void natalieMuRipperTest() throws Exception {
        List<URL> contentURLs = new ArrayList<>();
        // URLs that should return more than 1 image
        contentURLs.add(new URL("http://natalie.mu/music/news/140367"));
        contentURLs.add(new URL("http://cdn2.natalie.mu/music/news/140411"));
        contentURLs.add(new URL("http://cdn2.natalie.mu/music/gallery/show/news_id/140411/image_id/369655"));
        contentURLs.add(new URL("http://natalie.mu/music/gallery/show/news_id/139146/image_id/365218"));

        // Most *chans have volatile threads & can't be trusted for integration testing.

        //contentURLs.add(new URL("http://boards.4chan.org/r/res/12225949"));
        //contentURLs.add(new URL("http://7chan.org/gif/res/23795.html"));
        //contentURLs.add(new URL("http://unichan2.org/b/res/518004.html"));

        // xchan has an HTTPS certificaiton error...
        //contentURLs.add(new URL("http://xchan.pw/porn/res/437.html"));
        for (URL url : contentURLs) {
            NatalieMuRipper ripper = new NatalieMuRipper(url);
            testRipper(ripper);
        }
    }

}