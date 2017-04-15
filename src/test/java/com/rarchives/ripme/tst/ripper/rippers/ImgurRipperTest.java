package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.rippers.ImgurRipper;
import com.rarchives.ripme.ripper.rippers.ImgurRipper.ImgurAlbum;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImgurRipperTest extends RippersTest {

    @Test(expected = MalformedURLException.class)
    public void unsupportedImgurUrlFormatWithoutSlashTest() throws Exception {
        imgurRipError("http://imgur.com");
    }

    @Test(expected = MalformedURLException.class)
    public void unsupportedImgurUrlFormatWithSlashTest() throws Exception {
        imgurRipError("http://imgur.com/");
    }

    @Test(expected = MalformedURLException.class)
    public void unsupportedIImgurUrlFormatWithoutSlashTest() throws Exception {
        imgurRipError("http://i.imgur.com");
    }

    @Test(expected = MalformedURLException.class)
    public void unsupportedIImgurUrlFormatWithSlashTest() throws Exception {
        imgurRipError("http://i.imgur.com/");
    }

    @Test(expected = MalformedURLException.class)
    public void imgurImageDoesntContainCommasWithExtensionTest() throws Exception {
        imgurRipError("http://imgur.com/image");
    }

    @Test(expected = MalformedURLException.class)
    public void imgurImageDoesntContainCommasWithoutExtensionTest() throws Exception {
        imgurRipError("http://imgur.com/image.jpg");
    }

    @Test(expected = MalformedURLException.class)
    public void iImgurImageDoesntContainCommasWithoutExtensionTest() throws Exception {
        imgurRipError("http://i.imgur.com/image.jpg");
    }

    @Ignore
    public void imgurAlbumWithTitlesOrDescriptionsTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/bXQpH");
    }

    @Test
    public void imgurHorizontalLayoutTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/dS9OQ#0");
    }

    @Test
    public void imgurGridLayoutTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/YpsW9#0");
    }

    @Test
    public void imgurVerticalParamLayoutTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/WxG6f/layout/vertical#0");
    }

    @Test
    public void imgurHorizontalParamLayoutTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/WxG6f/layout/horizontal#0");
    }

    @Test
    public void imgurGridParamLayoutTest() throws Exception {
        imgurRipSuccess("http://imgur.com/a/WxG6f/layout/grid#0");
    }

    @Test
    public void imgurGalleryTest() throws Exception {
        imgurRipSuccess("http://imgur.com/gallery/FmP2o");
    }

    @Test
    public void imgurAlbumWithMoreThanTwentyPicturesTest() throws Exception {
        ImgurAlbum album = ImgurRipper.getImgurAlbum(new URL("http://imgur.com/a/HUMsq"));
        assertTrue("Failed to find 20 files from " + album.url.toExternalForm() + ", only got " + album.images.size(), album.images.size() >= 20);
    }

    @Test
    public void imgurAlbumWithMoreThanHundredPicturesTest() throws IOException {
        ImgurAlbum album = ImgurRipper.getImgurAlbum(new URL("http://imgur.com/a/zXZBU"));
        assertTrue("Failed to find 100 files from " + album.url.toExternalForm() + ", only got " + album.images.size(), album.images.size() >= 100);
    }

    @Test
    public void imgurAlbumWithMoreThanThousandPicturesTest() throws IOException {
        ImgurAlbum album = ImgurRipper.getImgurAlbum(new URL("http://imgur.com/a/vsuh5"));
        assertTrue("Failed to find 1000 files from " + album.url.toExternalForm() + ", only got " + album.images.size(), album.images.size() >= 1000);
    }

    private void imgurRipError(String urlText) throws Exception {
        URL url = new URL(urlText);

        try {
            new ImgurRipper(url);
            fail("Instantiated ripper for URL that should not work: " + url);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unable to rip url"));
            throw e;
        }
    }

    private void imgurRipSuccess(String urlText) throws Exception {
        URL url = new URL(urlText);

        ImgurRipper ripper = new ImgurRipper(url);
        testRipper(ripper);
    }

}