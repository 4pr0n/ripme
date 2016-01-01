package com.rarchives.ripme.storage;

import com.rarchives.ripme.utils.Utils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.auth.AuthenticationException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageService;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.GSCredentials;
import org.jets3t.service.security.ProviderCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * Created by lucas on 30/12/2015.
 */
public class Jets3tStorage extends AbstractStorage {
    // 0 for Amazon, 1 for Google.
    private String backend;
    private StorageService service;
    private ProviderCredentials credentials;
    private StorageBucket bucket;

    public Jets3tStorage() throws AuthenticationException {
        super();
    }

    @Override
    public void configure() throws AuthenticationException {
        backend = Utils.getConfigString("jets3t.backend", null);
        if (backend.equals("s3")) {
            credentials = new AWSCredentials(
                    Utils.getConfigString("jets3t.awsAccessKey", null), Utils.getConfigString("jets3t.awsSecretKey", null));
            service = new RestS3Service(credentials);
            try {
                bucket = service.getBucket(Utils.getConfigString("jets3t.awsBucket", null));
            } catch (ServiceException e) {
                throw new AuthenticationException();
            }
        } else if (backend.equals("gs")) {
            try {
                credentials = new GSCredentials(
                        Utils.getConfigString("jets3t.gsAccessKey", null), Utils.getConfigString("jets3t.gsSecretKey", null));
                service = new GoogleStorageService(credentials);
                bucket = service.getBucket(Utils.getConfigString("jets3t.gsBucket", null));
                if (bucket == null) {
                    throw new AuthenticationException();
                }
            } catch (ServiceException e) {
                throw new AuthenticationException();
            }
        } else {
            throw new AuthenticationException();
        }
    }

    @Override
    public void addFile(String path, InputStream inputStream, Long length, String contentType) throws IOException, FileAlreadyExistsException {
        boolean fileExists = false;
        try {
            service.getObjectDetails(bucket.getName(), path);
            fileExists = true;
        } catch (ServiceException e) {}
        if(fileExists){
            throw new FileAlreadyExistsException(path);
        }
        StorageObject newObject = new StorageObject(path);
        newObject.setDataInputStream(inputStream);
        newObject.setContentLength(length);
        newObject.setContentType(contentType);
        try {
            service.putObject(bucket.getName(), newObject);
        } catch (ServiceException e) {
            throw new IOException();
        }
    }

    @Override
    public boolean fileExists(String path) {
        try {
            service.getObjectDetails(bucket.getName(), path);
            return true;
        } catch (ServiceException e) {
            return false;
        }
    }
}
