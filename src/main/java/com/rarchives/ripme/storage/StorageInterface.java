package com.rarchives.ripme.storage;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * Created by lucas on 30/12/2015.
 */
public interface StorageInterface {
    void addFile(String path, InputStream inputStream, Long length, String contentType) throws IOException, FileAlreadyExistsException;
    void configure() throws AuthenticationException;
    boolean fileExists(String path);
}
