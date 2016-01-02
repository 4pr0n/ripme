package com.rarchives.ripme.storage;

import com.rarchives.ripme.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthenticationException;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;

/**
 * Created by lucas on 30/12/2015.
 */
public class FilesystemStorage extends AbstractStorage {
    public String downloadPath;

    protected static final Logger logger = Logger.getLogger(FilesystemStorage.class);


    public FilesystemStorage() throws AuthenticationException, IOException {
        super();
        downloadPath = Utils.getWorkingDirectory().getCanonicalPath();
    }

    @Override
    public void configure() throws AuthenticationException {
    }

    @Override
    public void addFile(String path, InputStream inputStream, Long length, String contentType) throws IOException, FileAlreadyExistsException {
        String fullPath = downloadPath + File.separator + path;
        new File(fullPath).getParentFile().mkdirs();
        File outFile = new File(fullPath);
        FileOutputStream fos = new FileOutputStream(outFile);
        IOUtils.copy(inputStream, fos);
        try {
            fos.close();
        } catch (Exception e) {}
        try {
            inputStream.close();
        } catch (Exception e) {}
    }

    @Override
    public boolean fileExists(String path) {
        return new File(downloadPath + File.separator + path).exists();
    }
}
