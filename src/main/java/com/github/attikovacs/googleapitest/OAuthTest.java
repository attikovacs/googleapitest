package com.github.attikovacs.googleapitest;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.common.collect.Lists;

public class OAuthTest {

	private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

	private static final String APPLICATION_NAME = "YouTube Playlists";
	
	public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	public static Credential authorize(List<String> scopes, String credentialDatastore) throws IOException {

        // Load client secrets.
        Reader clientSecretReader = new InputStreamReader(OAuthTest.class.getResourceAsStream("/client_secret.json"));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(new JacksonFactory(), clientSecretReader);

        // Checks that the defaults have been replaced (Default = "Enter X here").
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://console.developers.google.com/project/_/apiui/credential "
                            + "into src/main/resources/client_secrets.json");
            System.exit(1);
        }

        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(), clientSecrets, scopes)
        		.setCredentialDataStore(datastore)
//        		.setAccessType("offline")
                .build();

        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
    }

    public static void main(String[] args) throws IOException {

    	List<String> scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE_READONLY);
    	
        // Authorize the request.
        Credential credential = authorize(scopes, "getplaylists");

        // This object is used to make YouTube Data API requests.
        YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        		.setApplicationName(APPLICATION_NAME).build();

        try {
        	
        	HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet,contentDetails");
            parameters.put("mine", "true");
            parameters.put("maxResults", "25");
            parameters.put("onBehalfOfContentOwner", "");
            parameters.put("onBehalfOfContentOwnerChannel", "");

            YouTube.Playlists.List playlistsListMineRequest = youtube.playlists().list(parameters.get("part").toString());
            if (parameters.containsKey("mine") && parameters.get("mine") != "") {
                boolean mine = (parameters.get("mine") == "true") ? true : false;
                playlistsListMineRequest.setMine(mine);
            }

            if (parameters.containsKey("maxResults")) {
                playlistsListMineRequest.setMaxResults(Long.parseLong(parameters.get("maxResults").toString()));
            }

            if (parameters.containsKey("onBehalfOfContentOwner") && parameters.get("onBehalfOfContentOwner") != "") {
                playlistsListMineRequest.setOnBehalfOfContentOwner(parameters.get("onBehalfOfContentOwner").toString());
            }

            if (parameters.containsKey("onBehalfOfContentOwnerChannel") && parameters.get("onBehalfOfContentOwnerChannel") != "") {
                playlistsListMineRequest.setOnBehalfOfContentOwnerChannel(parameters.get("onBehalfOfContentOwnerChannel").toString());
            }

            PlaylistListResponse response = playlistsListMineRequest.execute();
            System.out.println(response);

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
