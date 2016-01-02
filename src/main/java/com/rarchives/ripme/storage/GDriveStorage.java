package com.rarchives.ripme.storage;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.model.File;
import com.rarchives.ripme.utils.Utils;
import org.apache.http.auth.AuthenticationException;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by lucas on 1/01/2016.
 */
public class GDriveStorage extends AbstractStorage {
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global Drive API client. */
    private static Drive drive;
    private Map<String, String> pathCache;
    private Semaphore pathCheckSemaphore;
    private String drivePrefix;

    public GDriveStorage() throws AuthenticationException {
        super();
        pathCache = new HashMap<String, String>();
        pathCheckSemaphore = new Semaphore(1);
    }

    @Override
    public void configure() throws AuthenticationException {
        Credential cred;
        try {
            cred = authorize();
        } catch (IOException e) {
            throw new AuthenticationException();
        }
        drive = new Drive.Builder(new NetHttpTransport(), JSON_FACTORY, cred).build();
        drivePrefix = Utils.getConfigString("drive.prefix", null);
    }

    private static Credential authorize() throws IOException {
        Credential cred;

        String refreshToken = Utils.getConfigString("drive.refreshToken", null);
        String accessToken = Utils.getConfigString("drive.accessToken", null);

        String clientId = Utils.getConfigString("drive.clientId", null);
        String clientSecret = Utils.getConfigString("drive.clientSecret", null);

        if (clientId == null || clientSecret == null) {
            logger.error("Client ID or secret was missing.");
            return null;
        }

        if(refreshToken == null || accessToken == null) {
            List<String> scopes = new LinkedList<String>();
            scopes.add(DriveScopes.DRIVE);
            scopes.add(DriveScopes.DRIVE_FILE);
            scopes.add(DriveScopes.DRIVE_METADATA);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(), JSON_FACTORY, clientId, clientSecret, scopes).build();
            cred = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
            Utils.setConfigString("drive.refreshToken", cred.getRefreshToken());
            Utils.setConfigString("drive.accessToken", cred.getAccessToken());
            Utils.saveConfig();
        } else {
            GoogleCredential.Builder credBuilder = new GoogleCredential.Builder();
            credBuilder.setClientSecrets(clientId, clientSecret);
            credBuilder.setJsonFactory(JSON_FACTORY);
            credBuilder.setTransport(new NetHttpTransport());
            cred = credBuilder.build();
            cred.setAccessToken(accessToken);
            cred.setRefreshToken(refreshToken);
        }
        return cred;
    }

    private ParentReference getParentFromPath(String[] path, Boolean create) throws IOException {
        ParentReference parent = new ParentReference();
        parent.setId("root");

        if (drivePrefix != null) {
            String[] prefixSplit = drivePrefix.split(java.io.File.separator);
            String[] oldPath = path.clone();
            path = new String[prefixSplit.length + oldPath.length];
            Integer counter = 0;
            for(;counter < prefixSplit.length; counter++){
                path[counter] = prefixSplit[counter];
            }
            for(;counter - prefixSplit.length < oldPath.length; counter++){
                path[counter] = oldPath[counter - prefixSplit.length];
            }
        }

        String fullPath;
        if (path.length == 0) {
            return null;
        } else if (path.length == 1) {
            fullPath = path[0];
        } else {
            fullPath = path[0];
            for (Integer i = 0; i < path.length; i++) {
                fullPath += java.io.File.separator + path[i];
            }
        }

        if(pathCache.containsKey(fullPath)) {
            String id = pathCache.get(fullPath);
            if (id != null) {
                parent.setId(pathCache.get(fullPath));
                return parent;
            } else if (!create) {
                return null;
            }
        }

        pathCheckSemaphore.acquireUninterruptibly();
        try {
            for (Integer i = 0; i < path.length - 1; i++) {
                String dir = path[i];
                Drive.Files.List fListReq = drive.files().list();
                String query = "";
                String escapedDir = dir.replace("'", "\\'");
                query += "title = '" + escapedDir + "'";
                query += " and trashed = false";
                query += " and mimeType = 'application/vnd.google-apps.folder'";
                query += " and '" + parent.getId() + "' in parents";

                fListReq.setQ(query);
                List<File> fListRes = fListReq.execute().getItems();
                if (fListRes.size() == 0) {
                    if (create) {
                        File folderMetadata = new File();
                        folderMetadata.setTitle(dir);
                        folderMetadata.setMimeType("application/vnd.google-apps.folder");
                        folderMetadata.setParents(Collections.singletonList(parent));
                        File newFolder = drive.files().insert(folderMetadata).execute();
                        parent = new ParentReference();
                        parent.setId(newFolder.getId());
                    } else {
                        parent = null;
                        break;
                    }
                } else {
                    parent = new ParentReference();
                    parent.setId(fListRes.get(0).getId());
                }
            }
        } catch (IOException e) {
            pathCheckSemaphore.release();
            throw e;
        }
        pathCache.put(fullPath, parent == null ? null : parent.getId());
        pathCheckSemaphore.release();
        return parent;
    }

    @Override
    public void addFile(String path, InputStream inputStream, Long length, String contentType) throws IOException, FileAlreadyExistsException {
        InputStreamContent mediaContent = new InputStreamContent(contentType, inputStream);
        mediaContent.setLength(length);

        String[] splitPath = path.split(java.io.File.separator);
        String fileName = splitPath[splitPath.length - 1];

        ParentReference parent = getParentFromPath(splitPath, true);

        File fileMetadata = new File();
        fileMetadata.setTitle(fileName);
        fileMetadata.setMimeType(contentType);
        fileMetadata.setParents(Collections.singletonList(parent));

        Drive.Files.Insert insert = drive.files().insert(fileMetadata, mediaContent);
        insert.execute();
    }

    @Override
    public boolean fileExists(String path) {
        try {
            String[] splitPath = path.split(java.io.File.separator);
            ParentReference parent = getParentFromPath(splitPath, false);
            if (parent == null) {
                return false;
            }
            Drive.Files.List listReq = drive.files().list();
            String fileName = splitPath[splitPath.length - 1];
            String query = "title = '" + fileName.replace("'", "\\'") + "' and '"
                    + parent.getId() + "' in parents and trashed=false";
            listReq.setQ(query);
            List<File> listRes = listReq.execute().getItems();
            return listRes.size() > 0;
        } catch (IOException e) {
            return false;
        }

    }
}
