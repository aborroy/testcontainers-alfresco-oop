# Alfresco Out-of-Process Extension with TestContainers

Welcome to the guide for developing an [Alfresco Out-of-Process (OOP) extension](https://docs.alfresco.com/content-services/latest/develop/oop-ext-points/) integrated with [TestContainers](https://testcontainers.com) for testing. This project supports both the Alfresco Community and Enterprise editions.

## Project Structure

This project includes the following folders:

* [testcontainers-alfresco-oop](testcontainers-alfresco-oop): A Spring Boot application that listens to the Alfresco Community ActiveMQ to detect the creation of HTML files. It includes a testing strategy using [AlfrescoContainer](https://testcontainers.com/modules/alfresco/) for unit testing.
* [testcontainers-alfresco-oop-ent](testcontainers-alfresco-oop-ent): A Spring Boot application designed for the Alfresco Enterprise edition. It listens to the Alfresco Enterprise ActiveMQ for HTML file creation and verifies that Enterprise Events are captured. This module also provides a patch for Alfresco issue MNT-24580, addressing the lack of support for Enterprise Events in `alfresco-java-sdk:0.6.2`. It includes a testing strategy using [AlfrescoContainer](https://testcontainers.com/modules/alfresco/) for unit testing.

## Using TestContainers

To perform unit tests with TestContainers, follow these steps:

1. Start the `AlfrescoContainer` with ActiveMQ Enabled

   Ensure that the `AlfrescoContainer` is initialized and started before running the tests. This example shows how to configure it with ActiveMQ:

   ```java
   @BeforeAll
   static void setUp() {
       alfrescoContainer = new AlfrescoContainer<>("23.2.1").withMessagingEnabled();
       alfrescoContainer.start();
       activemqContainer = alfrescoContainer.getActivemqContainer();
   }
   ```

2. Configure ActiveMQ Container Properties

   Overwrite the `application.properties` values with the ActiveMQ container's host and port:

   ```java
   @DynamicPropertySource
   static void dynamicProperties(DynamicPropertyRegistry registry) {
       registry.add("spring.activemq.brokerUrl", () ->
               "tcp://" + activemqContainer.getHost() + ":" + activemqContainer.getMappedPort(61616));
   }
   ```

3. Set Alfresco Container Host and Port for the RestClient

   Configure the `RestClient` with the Alfresco container's host and port before each test:

   ```java
   @BeforeEach
   void setUpEach() {
       restClient.setAlfrescoUrl("http", alfrescoContainer.getHost(), alfrescoContainer.getMappedPort(8080), "alfresco");
   }
   ```

4. Shutdown the AlfrescoContainer After Tests

   Clean up resources by stopping the `AlfrescoContainer` after all tests have completed:

   ```java
   @AfterAll
   static void tearDownAll() {
       alfrescoContainer.stop();
   }
   ```

## Additional Notes

Please refer to the specific instructions within the Community and Enterprise folders for detailed implementation guidelines.