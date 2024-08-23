# Alfresco Community Out-of-Process Extension with TestContainers

This project provides a Spring Boot application that listens to the Alfresco Community ActiveMQ to detect the creation of HTML files. It also includes a testing strategy using [AlfrescoContainer](https://testcontainers.com/modules/alfresco/) for unit testing.

## Project Structure

The project is organized as follows:

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org
│   │   │       └── alfresco
│   │   │           ├── App.java
│   │   │           └── handler
│   │   │               └── HtmlContentCreatedHandler.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
│           └── org
│               └── alfresco
│                   ├── handler
│                   │   └── HtmlContentCreatedHandlerTest.java
│                   └── rest
│                       └── AlfrescoClient.java
```

### Files and Their Purpose

- **`App.java`**: The entry point for the Spring Boot application. Initializes and starts the application.
  
- **`HtmlContentCreatedHandler.java`**: Implements the Alfresco Out-of-Process event handler that detects the creation of HTML files. Logs a message whenever a new HTML file is detected.
  
- **`application.properties`**: Configuration file for the Spring Boot application, including properties for connecting to the ActiveMQ endpoint.

- **`HtmlContentCreatedHandlerTest.java`**: Contains unit tests for `HtmlContentCreatedHandler`, using `AlfrescoContainer` for test isolation and integration. Note that the dependency is imported from Maven Central as `org.alfresco.alfresco-testcontainers:0.8.0`.

- **`AlfrescoClient.java`**: A simple REST client for interacting with the Alfresco API.


## Additional Notes

For further details on configuring and customizing the application, please refer to the `application.properties` and relevant Java classes. If you encounter issues or have questions, consult the [TestContainers documentation](https://testcontainers.com/modules/alfresco/) or the Alfresco Community forums.