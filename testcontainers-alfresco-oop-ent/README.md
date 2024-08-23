# Alfresco Enterprise Out-of-Process Extension with TestContainers

This Spring Boot application is designed for the Alfresco Enterprise edition. It listens to the Alfresco Enterprise ActiveMQ to detect HTML file creation and verifies that Enterprise Events are properly captured. Additionally, this module includes a patch for Alfresco issue MNT-24580, addressing the lack of support for Enterprise Events in `alfresco-java-sdk:0.6.2`. The project also incorporates a testing strategy using [AlfrescoContainer](https://testcontainers.com/modules/alfresco/) for unit testing.

## Project Structure

The project is organized as follows:

```
.
├── README.md
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org
│   │   │       └── alfresco
│   │   │           ├── App.java
│   │   │           ├── event
│   │   │           │   └── sdk
│   │   │           │       └── integration
│   │   │           │           └── transformer
│   │   │           │               └── EventGenericTransformer.java
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

- **`HtmlContentCreatedHandler.java`**: An Alfresco Out-of-Process event handler that detects the creation of HTML files. Logs a message and processes information related to Enterprise Events.

- **`EventGenericTransformer.java`**: Contains a fix for Alfresco issue MNT-24580, addressing the Enterprise Events support issue in `alfresco-java-sdk:0.6.2`.

- **`application.properties`**: Configuration file for the Spring Boot application, including properties for connecting to the ActiveMQ endpoint.

- **`HtmlContentCreatedHandlerTest.java`**: Unit tests for `HtmlContentCreatedHandler`, utilizing `AlfrescoContainer` for isolated and comprehensive testing. The relevant dependency is `org.alfresco.alfresco-testcontainers:0.8.1`, imported from Maven Central.

- **`AlfrescoClient.java`**: A simple REST client for interacting with the Alfresco API.


## Additional Notes

Refer to the `application.properties` and the relevant Java classes for further details on configuration and implementation. For any issues or questions, consult the [TestContainers documentation](https://testcontainers.com/modules/alfresco/) or the Alfresco Community forums.