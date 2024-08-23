package org.alfresco.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A simple client for interacting with the Alfresco REST API.
 * <p>
 * This client provides methods for authenticating with Alfresco and creating files (HTML and TXT) within the Alfresco repository.
 * It uses OkHttp as the HTTP client and Jackson for JSON parsing.
 */
@Service
public class AlfrescoClient {

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String USER_ID = "admin";
    private static final String PASSWORD = "admin";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String AUTH_PATH = "/api/-default-/public/authentication/versions/1/tickets";
    private static final String FILE_CREATION_PATH = "/api/-default-/public/alfresco/versions/1/nodes/-shared-/children";

    private String alfrescoUrl;

    /**
     * Sets the base URL for the Alfresco instance.
     *
     * @param protocol the protocol to use (e.g., "http" or "https")
     * @param host     the host name or IP address of the Alfresco server
     * @param port     the port number on which Alfresco is running
     * @param context  the context path of the Alfresco web application
     */
    public void setAlfrescoUrl(String protocol, String host, int port, String context) {
        this.alfrescoUrl = String.format("%s://%s:%d/%s", protocol, host, port, context);
    }

    /**
     * Obtains an authentication ticket from Alfresco using the default admin credentials.
     *
     * @return the authentication ticket as a String
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
            JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body().string());
            return jsonNode.path("entry").path("id").asText();
        }
    }

    /**
     * Creates an HTML file in the Alfresco repository.
     *
     * @param filename the name of the HTML file to be created
     * @return the name of the created node in Alfresco
     * @throws IOException if there is an error during the HTTP request
     */
    public String createHtmlFileInAlfresco(String filename) throws IOException {
        String jsonBody = String.format("""
        {
          "name": "%s",
          "nodeType": "cm:content",
          "properties": {
            "cm:title": "Test HTML File"
          }
        }
        """, filename);
        return createFileInAlfresco(jsonBody);
    }

    /**
     * Creates a TXT file in the Alfresco repository.
     *
     * @param filename the name of the TXT file to be created
     * @return the name of the created node in Alfresco
     * @throws IOException if there is an error during the HTTP request
     */
    public String createTxtFileInAlfresco(String filename) throws IOException {
        String jsonBody = String.format("""
        {
          "name": "%s",
          "nodeType": "cm:content",
          "properties": {
            "cm:title": "Test TXT File"
          }
        }
        """, filename);
        return createFileInAlfresco(jsonBody);
    }

    /**
     * Creates a file in the Alfresco repository using the provided JSON body.
     *
     * @param jsonBody JSON string representing the file properties and metadata
     * @return the name of the created node in Alfresco
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
            JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body().string());
            return jsonNode.path("entry").path("name").asText();
        }
    }

    /**
     * Encodes the provided authentication ticket using Base64 encoding.
     *
     * @param authTicket the authentication ticket to encode
     * @return the Base64 encoded authentication ticket
     */
    private String encodeCredentials(String authTicket) {
        return Base64.getEncoder().encodeToString(authTicket.getBytes(StandardCharsets.UTF_8));
    }

}