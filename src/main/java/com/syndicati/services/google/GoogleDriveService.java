package com.syndicati.services.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class GoogleDriveService {
    private static final String APPLICATION_NAME = "Syndicati";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/syndicati-drive");
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Credential authorize() throws IOException {
        InputStream in = GoogleDriveService.class.getResourceAsStream("/client_secret_536985587673-1g3kqq579gpu0vnnlkts4u0h2095v1je.apps.googleusercontent.com.json");
        if (in == null) {
            throw new IOException("Resource not found: client_secret file");
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static String getOrCreateFolder(Drive service, String folderName, String parentId) throws IOException {
        String query = "name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
        if (parentId != null) {
            query += " and '" + parentId + "' in parents";
        }
        
        FileList result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .execute();
        
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            if (parentId != null) {
                fileMetadata.setParents(Collections.singletonList(parentId));
            }
            File file = service.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            return file.getId();
        } else {
            return files.get(0).getId();
        }
    }

    /**
     * Uploads a file to Google Drive. If a file with the same name exists in the user's folder, it updates it.
     * @param userEmail The email of the user (used for folder naming).
     * @param filename The name of the file on Drive.
     * @param localFile The local file to upload.
     */
    public static void uploadOrUpdateFileAsync(String userEmail, String filename, java.io.File localFile) {
        new Thread(() -> {
            try {
                uploadOrUpdateFile(userEmail, filename, localFile);
                System.out.println("[GoogleDrive] Successfully uploaded/updated: " + filename);
            } catch (IOException e) {
                System.err.println("[GoogleDrive] Failed to upload/update: " + filename);
                e.printStackTrace();
            }
        }).start();
    }

    public static void uploadOrUpdateFile(String userEmail, String filename, java.io.File localFile) throws IOException {
        if (localFile == null || !localFile.exists()) return;
        
        Drive service = getDriveService();
        
        // Root User Folder
        String userFolderId = getOrCreateFolder(service, userEmail, null);
        
        // Search for existing file with the same name in this folder
        String query = "name = '" + filename.replace("'", "\\'") + "' and '" + userFolderId + "' in parents and trashed = false";
        FileList result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();
        
        List<File> files = result.getFiles();
        
        // MIME type detection
        String mimeType = Files.probeContentType(localFile.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";
        FileContent mediaContent = new FileContent(mimeType, localFile);

        if (files != null && !files.isEmpty()) {
            // Update existing file content
            String fileId = files.get(0).getId();
            service.files().update(fileId, new File(), mediaContent).execute();
        } else {
            // Create new file
            File fileMetadata = new File();
            fileMetadata.setName(filename);
            fileMetadata.setParents(Collections.singletonList(userFolderId));
            
            service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
        }
    }

    public static java.io.File resolveUploadFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty() || "-".equals(relativePath)) return null;
        String uploadsPath = System.getProperty("user.dir") + java.io.File.separator + "uploads";
        return new java.io.File(uploadsPath, relativePath);
    }
}
