package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.rippers.InstagramRipper;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InstagramRipperTest extends RippersTest {

    @Test
    public void instagramGIDTest() throws Exception {
        Map<URL, String> testURLs = new HashMap<>();
        testURLs.put(new URL("http://instagram.com/Test_User"), "Test_User");
        testURLs.put(new URL("http://instagram.com/_test_user_"), "_test_user_");
        testURLs.put(new URL("http://instagram.com/-test-user-"), "-test-user-");

        for (URL url : testURLs.keySet()) {
            InstagramRipper ripper = new InstagramRipper(url);
            ripper.setup();
            assertEquals(testURLs.get(url), ripper.getGID(ripper.getURL()));
            deleteDir(ripper.getWorkingDir());
        }
    }

    @Test
    public void instagramAlbumsTest() throws Exception {
        List<URL> contentURLs = new ArrayList<>();
        contentURLs.add(new URL("http://instagram.com/anacheri"));
        contentURLs.add(new URL("https://www.instagram.com/missmiakang/"));

        for (URL url : contentURLs) {
            InstagramRipper ripper = new InstagramRipper(url);
            testRipper(ripper);
        }
    }

}