package com.rarchives.ripme.ui;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class RipStatusComplete {
    File dir = null;
    int count = 0;

    protected static final Logger LOGGER = Logger.getLogger(RipStatusComplete.class);

    public RipStatusComplete(File dir) {
        this.dir = dir;
        this.count = 1;
    }

    public RipStatusComplete(File dir, int count) {
        this.dir = dir;
        this.count = count;
    }

    public String getDir() {
        String result;

        try {
            result = this.dir.getCanonicalPath();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            result = this.dir.toString();
        }

        return result;
    }

}