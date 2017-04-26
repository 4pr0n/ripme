package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.ripper.rippers.DeviantartRipper;
import com.rarchives.ripme.ripper.rippers.EightmusesRipper;
import com.rarchives.ripme.ripper.rippers.FineboxRipper;
import com.rarchives.ripme.ripper.rippers.FivehundredpxRipper;
import com.rarchives.ripme.ripper.rippers.FuraffinityRipper;
import com.rarchives.ripme.ripper.rippers.FuskatorRipper;
import com.rarchives.ripme.ripper.rippers.GirlsOfDesireRipper;
import com.rarchives.ripme.ripper.rippers.HentaifoundryRipper;
import com.rarchives.ripme.ripper.rippers.ImagearnRipper;
import com.rarchives.ripme.ripper.rippers.ImagebamRipper;
import com.rarchives.ripme.ripper.rippers.ImagevenueRipper;
import com.rarchives.ripme.ripper.rippers.ImgboxRipper;
import com.rarchives.ripme.ripper.rippers.ModelmayhemRipper;
import com.rarchives.ripme.ripper.rippers.MotherlessRipper;
import com.rarchives.ripme.ripper.rippers.MyhentaicomicsRipper;
import com.rarchives.ripme.ripper.rippers.NfsfwRipper;
import com.rarchives.ripme.ripper.rippers.NhentaiRipper;
import com.rarchives.ripme.ripper.rippers.PhotobucketRipper;
import com.rarchives.ripme.ripper.rippers.PornhubRipper;
import com.rarchives.ripme.ripper.rippers.RedditRipper;
import com.rarchives.ripme.ripper.rippers.SankakuComplexRipper;
import com.rarchives.ripme.ripper.rippers.ShesFreakyRipper;
import com.rarchives.ripme.ripper.rippers.TapasticRipper;
import com.rarchives.ripme.ripper.rippers.TeenplanetRipper;
import com.rarchives.ripme.ripper.rippers.TwitterRipper;
import com.rarchives.ripme.ripper.rippers.TwodgalleriesRipper;
import com.rarchives.ripme.ripper.rippers.VidbleRipper;
import com.rarchives.ripme.ripper.rippers.VineRipper;
import com.rarchives.ripme.ripper.rippers.VkRipper;
import com.rarchives.ripme.ripper.rippers.XhamsterRipper;
import com.rarchives.ripme.ripper.rippers.ZizkiRipper;
import org.junit.Test;

import java.net.URL;

/**
 * Simple test cases for various rippers.
 * These tests only require a URL, no other special validation.
 */
public class BasicRippersTest extends RippersTest {

    @Test
    public void deviantartAlbumTest() throws Exception {
        DeviantartRipper ripper = new DeviantartRipper(new URL("http://airgee.deviantart.com/gallery/"));
        testRipper(ripper);
    }

    @Test
    public void deviantartNSFWAlbumTest() throws Exception {
        // NSFW gallery
        DeviantartRipper ripper = new DeviantartRipper(new URL("http://faterkcx.deviantart.com/gallery/"));
        testRipper(ripper);
    }

    @Test
    public void eightmusesAlbumTest() throws Exception {
        EightmusesRipper ripper = new EightmusesRipper(new URL("http://www.8muses.com/index/category/jab-hotassneighbor7"));
        testRipper(ripper);
        ripper = new EightmusesRipper(new URL("https://www.8muses.com/album/jab-comics/a-model-life"));
        testRipper(ripper);
    }

    @Test
    public void vineboxAlbumTest() throws Exception {
        FineboxRipper ripper = new FineboxRipper(new URL("http://vinebox.co/u/wi57hMjc2Ka"));
        testRipper(ripper);
    }

    @Test
    public void fineboxAlbumTest() throws Exception {
        FineboxRipper ripper = new FineboxRipper(new URL("http://finebox.co/u/wi57hMjc2Ka"));
        testRipper(ripper);
    }

    /*
    public void testRedditSubredditRip() throws Exception {
        RedditRipper ripper = new RedditRipper(new URL("http://www.reddit.com/r/nsfw_oc"));
        testRipper(ripper);
    }*/

    @Test
    public void redditSubredditTopRipTest() throws Exception {
        RedditRipper ripper = new RedditRipper(new URL("http://www.reddit.com/r/nsfw_oc/top?t=all"));
        testRipper(ripper);
    }

    @Test
    public void redditPostRipTest() throws Exception {
        RedditRipper ripper = new RedditRipper(new URL("http://www.reddit.com/r/UnrealGirls/comments/1ziuhl/in_class_veronique_popa/"));
        testRipper(ripper);
    }


    /*
    public void testTumblrFullRip() throws Exception {
        TumblrRipper ripper = new TumblrRipper(new URL("https://wrouinr.tumblr.com/archive"));
        testRipper(ripper);
    }

    public void testTumblrTagRip() throws Exception {
        TumblrRipper ripper = new TumblrRipper(new URL("http://girls-n-yogapants.tumblr.com/tagged/thin"));
        testRipper(ripper);
    }

    public void testTumblrPostRip() throws Exception {
        TumblrRipper ripper = new TumblrRipper(new URL("http://sadbaffoon.tumblr.com/post/132045920789/what-a-hoe"));
        testRipper(ripper);
    }
    */

    @Test
    public void twitterUserRipTest() throws Exception {
        TwitterRipper ripper = new TwitterRipper(new URL("https://twitter.com/danngamber01/media"));
        testRipper(ripper);
    }
    /*
    public void testTwitterSearchRip() throws Exception {
        TwitterRipper ripper = new TwitterRipper(new URL("https://twitter.com/search?q=from%3ADaisyfairymfc%20filter%3Aimages&src=typd"));
        testRipper(ripper);
    }
    */

    @Test
    public void test500pxAlbumTest() throws Exception {
        FivehundredpxRipper ripper = new FivehundredpxRipper(new URL("https://marketplace.500px.com/alexander_hurman"));
        testRipper(ripper);
    }

    /*
    public void testFlickrAlbum() throws Exception {
        FlickrRipper ripper = new FlickrRipper(new URL("https://www.flickr.com/photos/leavingallbehind/sets/72157621895942720/"));
        testRipper(ripper);
    }
    */

    @Test
    public void furaffinityAlbumTest() throws Exception {
        FuraffinityRipper ripper = new FuraffinityRipper(new URL("https://www.furaffinity.net/gallery/mustardgas/"));
        testRipper(ripper);
    }

    @Test
    public void fuskatorAlbumTest() throws Exception {
        FuskatorRipper ripper = new FuskatorRipper(new URL("http://fuskator.com/full/emJa1U6cqbi/index.html"));
        testRipper(ripper);
    }

    /*
    GIFYO IS DOWN
    public void testGifyoAlbum() throws Exception {
        GifyoRipper ripper = new GifyoRipper(new URL("http://gifyo.com/PieSecrets/"));
        testRipper(ripper);
    }
    */

    @Test
    public void girlsofdesireAlbumTest() throws Exception {
        GirlsOfDesireRipper ripper = new GirlsOfDesireRipper(new URL("http://www.girlsofdesire.org/galleries/krillia/"));
        testRipper(ripper);
    }

    @Test
    public void hentaifoundryRipTest() throws Exception {
        HentaifoundryRipper ripper = new HentaifoundryRipper(new URL("http://www.hentai-foundry.com/pictures/user/personalami"));
        testRipper(ripper);
    }

    @Test
    public void imagearnRipTest() throws Exception {
        AbstractRipper ripper = new ImagearnRipper(new URL("http://imagearn.com//gallery.php?id=578682"));
        testRipper(ripper);
    }

    @Test
    public void imagebamRipTest() throws Exception {
        AbstractRipper ripper = new ImagebamRipper(new URL("http://www.imagebam.com/gallery/488cc796sllyf7o5srds8kpaz1t4m78i"));
        testRipper(ripper);
    }

    /*
    public void testImagestashRip() throws Exception {
        AbstractRipper ripper = new ImagestashRipper(new URL("https://imagestash.org/tag/everydayuncensor"));
        testRipper(ripper);
    }
    */

    @Test
    public void imagevenueRipTest() throws Exception {
        AbstractRipper ripper = new ImagevenueRipper(new URL("http://img120.imagevenue.com/galshow.php?gal=gallery_1373818527696_191lo"));
        testRipper(ripper);
    }

    @Test
    public void imgboxRipTest() throws Exception {
        //Old url (http://imgbox.com/g/sEMHfsqx4w)
        AbstractRipper ripper = new ImgboxRipper(new URL("http://imgbox.com/g/RYH4VCZkIs"));
        testRipper(ripper);
    }

    /*
    public void testMinusUserRip() throws Exception {
        AbstractRipper ripper = new MinusRipper(new URL("http://vampyr3.minus.com/"));
        testRipper(ripper);
        deleteSubdirs(ripper.getWorkingDir());
        deleteDir(ripper.getWorkingDir());
    }
    public void testMinusUserAlbumRip() throws Exception {
        AbstractRipper ripper = new MinusRipper(new URL("http://vampyr3.minus.com/mw7ztQ6xzP7ae"));
        testRipper(ripper);
    }
    public void testMinusUserUploadsRip() throws Exception {
        AbstractRipper ripper = new MinusRipper(new URL("http://vampyr3.minus.com/uploads"));
        testRipper(ripper);
    }
    public void testMinusAlbumRip() throws Exception {
        AbstractRipper ripper = new MinusRipper(new URL("http://minus.com/mw7ztQ6xzP7ae"));
        testRipper(ripper);
    }
    */

    @Test
    public void modelmayhemRipTest() throws Exception {
        AbstractRipper ripper = new ModelmayhemRipper(new URL("http://www.modelmayhem.com/portfolio/520206/viewall"));
        testRipper(ripper);
    }

    @Test
    public void motherlessAlbumRipTest() throws Exception {
        MotherlessRipper ripper = new MotherlessRipper(new URL("http://motherless.com/G4DAA18D"));
        testRipper(ripper);
    }

    @Test
    public void nfsfwRipTest() throws Exception {
        AbstractRipper ripper = new NfsfwRipper(new URL("http://nfsfw.com/gallery/v/Kitten/"));
        testRipper(ripper);
    }

    @Test
    public void nhentaiRipTest() throws Exception {
        NhentaiRipper ripper = new NhentaiRipper(new URL("https://nhentai.net/g/159174/"));
        testRipper(ripper);
    }

    @Test
    public void zizkiRipTest() throws Exception {
        ZizkiRipper ripper = new ZizkiRipper(new URL("http://zizki.com/marcus-gray/emilys-secret"));
        testRipper(ripper);
    }

    @Test
    public void myHentaiComicsRipTest() throws Exception {
        MyhentaicomicsRipper ripper = new MyhentaicomicsRipper(new URL("http://myhentaicomics.com/index.php/Furry-U"));
        testRipper(ripper);
    }

    @Test
    public void myHentaiComicsTagTest() throws Exception {
        MyhentaicomicsRipper ripper = new MyhentaicomicsRipper(new URL("http://myhentaicomics.com/index.php/tag/2443/"));
        testRipper(ripper);
        deleteSubdirs(ripper.getWorkingDir());
        deleteDir(ripper.getWorkingDir());
    }

    @Test
    public void photobucketRipTest() throws Exception {
        AbstractRipper ripper = new PhotobucketRipper(new URL("http://s844.photobucket.com/user/SpazzySpizzy/library/Album%20Covers?sort=3&page=1"));
        testRipper(ripper);
        deleteSubdirs(ripper.getWorkingDir());
        deleteDir(ripper.getWorkingDir());
    }

    @Test
    public void pornhubRipTest() throws Exception {
        AbstractRipper ripper = new PornhubRipper(new URL("http://www.pornhub.com/album/16871122"));
        testRipper(ripper);
    }

    /*
    public void testSankakuChanRip() throws Exception {
        AbstractRipper ripper = new SankakuComplexRipper(new URL("https://chan.sankakucomplex.com/?tags=cleavage"));
        testRipper(ripper);
    }
    */

    @Test
    public void sankakuIdolRipTest() throws Exception {
        AbstractRipper ripper = new SankakuComplexRipper(new URL("https://idol.sankakucomplex.com/?tags=meme_%28me%21me%21me%21%29_%28cosplay%29"));
        testRipper(ripper);
    }

    @Test
    public void shesFreakyRipTest() throws Exception {
        AbstractRipper ripper = new ShesFreakyRipper(new URL("http://www.shesfreaky.com/gallery/nicee-snow-bunny-579NbPjUcYa.html"));
        testRipper(ripper);
    }

    @Test
    public void tapasticRipTest() throws Exception {
        AbstractRipper ripper = new TapasticRipper(new URL("http://tapastic.com/episode/2139"));
        testRipper(ripper);
    }

    @Test
    public void teenplanetRipTest() throws Exception {
        AbstractRipper ripper = new TeenplanetRipper(new URL("http://teenplanet.org/galleries/the-perfect-side-of-me-6588.html"));
        testRipper(ripper);
    }

    @Test
    public void twodgalleriesRipTest() throws Exception {
        AbstractRipper ripper = new TwodgalleriesRipper(new URL("http://www.2dgalleries.com/artist/regis-loisel-6477"));
        testRipper(ripper);
    }

    @Test
    public void vidbleRipTest() throws Exception {
        AbstractRipper ripper = new VidbleRipper(new URL("http://www.vidble.com/album/y1oyh3zd"));
        testRipper(ripper);
    }

    @Test
    public void vineRipTest() throws Exception {
        AbstractRipper ripper = new VineRipper(new URL("https://vine.co/u/954440445776334848"));
        testRipper(ripper);
    }

    @Test
    public void vkSubalbumRipTest() throws Exception {
        VkRipper ripper = new VkRipper(new URL("http://vk.com/album45506334_0"));
        testRipper(ripper);
    }

    @Test
    public void vkRootAlbumRipTest() throws Exception {
        VkRipper ripper = new VkRipper(new URL("https://vk.com/album45506334_0"));
        testRipper(ripper);
    }

    @Test
    public void vkPhotosRipTest() throws Exception {
        VkRipper ripper = new VkRipper(new URL("https://vk.com/photos45506334"));
        testRipper(ripper);
    }

    @Test
    public void xhamsterAlbumsTest() throws Exception {
        XhamsterRipper ripper = new XhamsterRipper(new URL("http://xhamster.com/photos/gallery/1462237/alyssa_gadson.html"));
        testRipper(ripper);
    }

}