package org.alfresco.handler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.alfresco.rest.AlfrescoClient;
import org.alfresco.testcontainers.AlfrescoContainer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * Integration test for {@link HtmlContentCreatedHandler} using Spring Boot and Testcontainers.
 * <p>
 * This class verifies the behavior of the handler when creating HTML and non-HTML files in the Alfresco repository.
 * It uses Testcontainers to start an Alfresco instance and an ActiveMQ broker for testing the event handling.
 */
@SpringBootTest
public class HtmlContentCreatedHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlContentCreatedHandlerTest.class);

    // Containers are static to ensure they are shared across tests and only started once
    private static AlfrescoContainer<?> alfrescoContainer;
    private static ActiveMQContainer activemqContainer;

    // In-memory log appender to capture logs for assertion
    private ListAppender<ILoggingEvent> listAppender;

    @Autowired
    private AlfrescoClient restClient;

    @Autowired
    private HtmlContentCreatedHandler handler;

    /**
     * Registers dynamic properties such as the ActiveMQ broker URL.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.brokerUrl", () ->
                "tcp://" + activemqContainer.getHost() + ":" + activemqContainer.getMappedPort(61616));
    }

    /**
     * Initializes resources before all tests, including starting containers.
     */
    @BeforeAll
    static void setUp() {
        // Spawn Alfresco Enterprise Repository
        alfrescoContainer = new AlfrescoContainer<>(
                DockerImageName.parse("quay.io/alfresco/alfresco-content-repository:23.2.1"))
                .withMessagingEnabled();
        alfrescoContainer.start();
        activemqContainer = alfrescoContainer.getActivemqContainer();
    }

    /**
     * Sets up the environment before each test, configuring the REST client and initializing log capturing.
     */
    @BeforeEach
    void setUpEach() {
        restClient.setAlfrescoUrl("http", alfrescoContainer.getHost(), alfrescoContainer.getMappedPort(8080), "alfresco");

        // Configure in-memory log appender to capture logs for verification
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        listAppender = new ListAppender<>();
        listAppender.setContext(loggerContext);
        listAppender.start();

        Logger logger = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);
        ((ch.qos.logback.classic.Logger) logger).addAppender(listAppender);
    }

    /**
     * Cleans up the log appender after each test to prevent memory leaks.
     */
    @AfterEach
    void tearDown() {
        Logger logger = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);
        ((ch.qos.logback.classic.Logger) logger).detachAppender(listAppender);
    }

    /**
     * Test the handler's behavior when an HTML file is created in Alfresco.
     * <p>
     * It ensures the handler logs the correct message when processing an HTML file creation event.
     *
     * @throws IOException          if there is an issue creating the file in Alfresco
     * @throws InterruptedException if the thread is interrupted while waiting for log processing
     */
    @Test
    void testHandleEventForCreatedHtmlFile() throws IOException, InterruptedException {
        try {
            String filename = "test.html";
            String nodeName = restClient.createHtmlFileInAlfresco(filename);
            Assertions.assertTrue(nodeName.contains(filename), "Node name should contain 'test.html'");
        } catch (IOException ioException) {
            LOGGER.error("Alfresco container logs:\n{}", alfrescoContainer.getLogs());
            throw ioException;
        }

        // Allow some time for the log message to be processed by the handler
        Thread.sleep(1000);

        // Assert that the correct log message was captured
        Assertions.assertFalse(listAppender.list.isEmpty(), "Log message should not be empty");
        ILoggingEvent logEvent = listAppender.list.get(0);
        Assertions.assertEquals("An HTML content named test.html has been created!", logEvent.getFormattedMessage(),
                "Log message should indicate the creation of 'test.html'");
        // Assert the event is including Enterprise information
        logEvent = listAppender.list.get(1);
        Assertions.assertTrue(logEvent.getFormattedMessage().startsWith("Permissions - reader authorities:"),
                "Expected result to start with 'Permissions - reader authorities:', but it starts with " + logEvent.getFormattedMessage());

    }

    /**
     * Test the handler's behavior when a non-HTML file (TXT) is created in Alfresco.
     * <p>
     * It ensures the handler does not log a message when processing a non-HTML file creation event.
     *
     * @throws IOException          if there is an issue creating the file in Alfresco
     * @throws InterruptedException if the thread is interrupted while waiting for log processing
     */
    @Test
    void testIgnoreEventForCreatedTxtFile() throws IOException, InterruptedException {
        try {
            String filename = "test.txt";
            String nodeName = restClient.createTxtFileInAlfresco(filename);
            Assertions.assertTrue(nodeName.contains(filename), "Node name should contain 'test.txt'");
        } catch (IOException ioException) {
            LOGGER.error("Alfresco container logs:\n{}", alfrescoContainer.getLogs());
            throw ioException;
        }

        // Allow some time for the log message to be processed
        Thread.sleep(1000);

        // Assert that no log message was captured since it's not an HTML file
        Assertions.assertTrue(listAppender.list.isEmpty(), "Log message should be empty for non-HTML files");
    }

    /**
     * Stops the containers after all tests have run to clean up resources.
     */
    @AfterAll
    static void tearDownAll() {
        alfrescoContainer.stop();
    }
}
