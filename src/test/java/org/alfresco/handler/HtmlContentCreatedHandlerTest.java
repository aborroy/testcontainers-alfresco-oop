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

import java.io.IOException;

/**
 * Integration test for HtmlContentCreatedHandler using Spring Boot and Testcontainers.
 */
@SpringBootTest
public class HtmlContentCreatedHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlContentCreatedHandlerTest.class);

    // Declare containers as static fields for reusability across tests
    private static AlfrescoContainer<?> alfrescoContainer;
    private static ActiveMQContainer activemqContainer;

    // In-memory log appender for capturing logs during tests
    private ListAppender<ILoggingEvent> listAppender;

    @Autowired
    private AlfrescoClient restClient;

    @Autowired
    private HtmlContentCreatedHandler handler;

    /**
     * Set up dynamic properties for ActiveMQ broker URL.
     */
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.brokerUrl", () ->
                "tcp://" + activemqContainer.getHost() + ":" + activemqContainer.getMappedPort(61616));
    }

    /**
     * Initialize logging appender before each test.
     */
    @BeforeEach
    void setUpEach() {
        restClient.setAlfrescoUrl("http", alfrescoContainer.getHost(), alfrescoContainer.getMappedPort(8080), "alfresco");

        // Configure logging appender for capturing logs
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        listAppender = new ListAppender<>();
        listAppender.setContext(loggerContext);
        listAppender.start();
        Logger logger = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);
        ((ch.qos.logback.classic.Logger) logger).addAppender(listAppender);
    }

    /**
     * Clean up logging appender after each test.
     */
    @AfterEach
    void tearDown() {
        Logger logger = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);
        ((ch.qos.logback.classic.Logger) logger).detachAppender(listAppender);
    }

    /**
     * Set up containers and handler before all tests. (Singleton pattern for containers)
     */
    @BeforeAll
    static void setUp() {
        alfrescoContainer = new AlfrescoContainer<>("23.2.1").withMessagingEnabled();
        alfrescoContainer.start();
        activemqContainer = alfrescoContainer.getActivemqContainer();
    }

    /**
     * Test to handle the creation of an HTML file in Alfresco.
     */
    @Test
    void testHandleEventForCreatedHtmlFile() throws IOException, InterruptedException {
        try {
            String nodeName = restClient.createHtmlFileInAlfresco();
            Assertions.assertTrue(nodeName.contains("test.html"), "Node name should contain 'test.html'");
        } catch (IOException ioException) {
            LOGGER.error("Alfresco container logs:\n{}", alfrescoContainer.getLogs());
            throw ioException;
        }

        // Allow some time for the log message to be processed by the handler
        Thread.sleep(1000);

        // Assert on the captured log message
        ILoggingEvent logEvent = listAppender.list.get(0);
        Assertions.assertEquals("An HTML content named test.html has been created!", logEvent.getFormattedMessage(),
                "Log message should indicate the creation of 'test.html'");
    }

    /**
     * Test to ignore the creation of a TXT file in Alfresco.
     */
    @Test
    void testIgnoreEventForCreatedTxtFile() throws IOException, InterruptedException {

        try {
            String nodeName = restClient.createTxtFileInAlfresco();
            Assertions.assertTrue(nodeName.contains("test.txt"), "Node name should contain 'test.txt'");
        } catch (IOException ioException) {
            LOGGER.error("Alfresco container logs:\n{}", alfrescoContainer.getLogs());
            throw ioException;
        }

        // Allow some time for the log message to be processed
        Thread.sleep(1000);

        Assertions.assertEquals(listAppender.list.size(), 0, "Log message must be empty");

    }

}
