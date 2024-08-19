package org.alfresco.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple Alfresco REST API Client.
 */
@Service
public class AlfrescoClient {

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static final String USER_ID = "admin";
    private static final String PASSWORD = "admin";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String AUTH_PATH = "/api/-default-/public/authentication/versions/1/tickets";
    private static final String FILE_CREATION_PATH = "/api/-default-/public/alfresco/versions/1/nodes/-shared-/children";

    private String alfrescoUrl;

    public void setAlfrescoUrl(String protocol, String host, int port, String context) {
        this.alfrescoUrl = String.format("%s://%s:%d/%s", protocol, host, port, context);
    }

    /**
     * Obtain an Alfresco authentication ticket.
     *
     * @return the authentication ticket
     * @throws IOException if there is an error during the HTTP request
     */
    private String getAlfrescoAuthTicket() throws IOException {
        String jsonBody = String.format("""
        {
          "userId": "%s",
          "password": "%s"
        }
        """, USER_ID, PASSWORD);

        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(alfrescoUrl + AUTH_PATH)
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to obtain Alfresco auth ticket: " + response);
            }
            assert response.body() != null;
            JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
            return jsonNode.path("entry").path("id").asText();
        }
    }

    /**
     * Create an HTML file in Alfresco.
     *
     * @return the name of the created node
     * @throws IOException if there is an error during the HTTP request
     */
    public String createHtmlFileInAlfresco() throws IOException {
        String jsonBody = """
        {
          "name": "test.html",
          "nodeType": "cm:content",
          "properties": {
            "cm:title": "Test HTML File"
          }
        }
        """;
        return createFileInAlfresco(jsonBody);
    }

    /**
     * Create a TXT file in Alfresco.
     *
     * @return the name of the created node
     * @throws IOException if there is an error during the HTTP request
     */
    public String createTxtFileInAlfresco() throws IOException {
        String jsonBody = """
        {
          "name": "test.txt",
          "nodeType": "cm:content",
          "properties": {
            "cm:title": "Test TXT File"
          }
        }
        """;
        return createFileInAlfresco(jsonBody);
    }

    /**
     * Create a file in Alfresco.
     *
     * @param jsonBody JSON string representing the file properties
     * @return the name of the created node
     * @throws IOException if there is an error during the HTTP request
     */
    private String createFileInAlfresco(String jsonBody) throws IOException {
        String authTicket = getAlfrescoAuthTicket();
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(alfrescoUrl + FILE_CREATION_PATH)
                .header("Authorization", "Basic " + encodeCredentials(authTicket))
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create file in Alfresco: " + response);
            }
            assert response.body() != null;
            JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
            return jsonNode.path("entry").path("name").asText();
        }
    }

    /**
     * Encode the authentication ticket using Base64.
     *
     * @param authTicket the authentication ticket to encode
     * @return the encoded ticket as a Base64 string
     */
    private String encodeCredentials(String authTicket) {
        return Base64.getEncoder().encodeToString(authTicket.getBytes(StandardCharsets.UTF_8));
    }
}