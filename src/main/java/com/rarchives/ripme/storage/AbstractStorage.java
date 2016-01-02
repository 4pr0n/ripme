package com.rarchives.ripme.storage;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.auth.AuthenticationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * Created by lucas on 30/12/2015.
 */
public abstract class AbstractStorage implements StorageInterface {
    protected static final Logger logger = Logger.getLogger(FilesystemStorage.class);
    public AbstractStorage() throws AuthenticationException {
        this.configure();
    }
    public abstract void configure() throws AuthenticationException;
    public abstract void addFile(String path, InputStream inputStream, Long length, String contentType) throws IOException, FileAlreadyExistsException;
    public abstract boolean fileExists(String path);
}
