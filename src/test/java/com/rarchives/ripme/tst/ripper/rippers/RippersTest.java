package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.utils.Utils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contains helper methods for testing rippers.
 */
public class RippersTest {

    private final Logger logger = Logger.getLogger(RippersTest.class);

    /**
     * File extensions that are safe to delete.
     */
    private static final String[] SAFE_EXTENSIONS = {
            "png", "jpg", "jpeg", "gif",
            "mp4", "webm", "mov", "mpg",
            "mpeg", "txt", "log", "php"
    };

    void testRipper(AbstractRipper ripper) {
        try {
            // Turn on Debug logging
            ((ConsoleAppender) Logger.getRootLogger().getAppender("stdout")).setThreshold(Level.DEBUG);

            // Decrease timeout
            Utils.setConfigInteger("page.timeout", 20_000);

            ripper.setup();
            ripper.markAsTest();
            ripper.rip();
            assertTrue("Failed to download a single file from " + ripper.getURL(), ripper.getWorkingDir().listFiles().length >= 1);
        } catch (IOException e) {

            if (e.getMessage().contains("Ripping interrupted")) {
                // We expect some rips to get interrupted
                Assert.assertThat(e.getMessage(), CoreMatchers.containsString("Ripping interrupted"));
            } else {
                e.printStackTrace();
                fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
        } finally {
            deleteDir(ripper.getWorkingDir());
        }
    }

    /**
     * Recursively deletes a directory
     */
    void deleteDir(File dir) {
        if (!dir.getName().contains("_")) {
            // All ripped albums contain an underscore
            // Don't delete an album if it doesn't have an underscore
            return;
        }

        for (File f : dir.listFiles()) {
            boolean safe = false;

            for (String ext : SAFE_EXTENSIONS)
                safe |= f.getAbsolutePath().toLowerCase().endsWith("." + ext);

            // Found a file we shouldn't delete! Stop deleting immediately.
            if (!safe)
                return;

            if (f.isDirectory())
                deleteDir(f);

            f.delete();
        }
        dir.delete();
    }

    void deleteSubdirs(File workingDir) {
        for (File f : workingDir.listFiles()) {
            if (f.isDirectory()) {
                for (File sf : f.listFiles()) {
                    logger.debug("Deleting " + sf);
                    sf.delete();
                }

                logger.debug("Deleting " + f);
                f.delete();
            }
        }
    }

}