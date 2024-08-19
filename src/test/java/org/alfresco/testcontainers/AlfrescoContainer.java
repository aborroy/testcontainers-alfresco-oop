package org.alfresco.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * This class sets up an Alfresco Content Repository container using Testcontainers.
 * It supports connecting to a PostgreSQL database container and an optional ActiveMQ container.
 *
 * @param <SELF> The type of the container, allowing fluent chaining.
 */
public class AlfrescoContainer<SELF extends GenericContainer<SELF>> extends GenericContainer<SELF> {

    // Network shared between containers
    private final Network network;

    // Containers for PostgreSQL and ActiveMQ
    private PostgreSQLContainer<?> postgresContainer;
    private GenericContainer<?> activemqContainer;

    // Default Docker image for the Alfresco Repository
    private static final String DEFAULT_REPOSITORY_DOCKER_IMAGE = "alfresco/alfresco-content-repository-community:23.2.1";

    // Default Java tool options for Alfresco
    private static final String DEFAULT_JAVA_TOOL_OPTIONS =
            """
            -Dencryption.keystore.type=JCEKS
            -Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding
            -Dencryption.keyAlgorithm=DESede
            -Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore
            -Dmetadata-keystore.password=mp6yc0UD9e
            -Dmetadata-keystore.aliases=metadata
            -Dmetadata-keystore.metadata.password=oKIWzVdEdA
            -Dmetadata-keystore.metadata.algorithm=DESede
            """.replace("\n", " ").trim();

    // Default Java options for Alfresco
    private static final String DEFAULT_JAVA_OPTS =
            """
            -Ddb.driver=org.postgresql.Driver
            -Ddb.username=alfresco
            -Ddb.password=alfresco
            -Ddb.url=jdbc:postgresql://postgres:5432/alfresco
            -Dindex.subsystem.name=noindex
            -Dlocal.transform.service.enabled=false
            -Drepo.event2.enabled=false
            -Dmessaging.subsystem.autoStart=false
            -Dcsrf.filter.enabled=false
            """.replace("\n", " ").trim();

    /**
     * Default constructor using the default Alfresco Docker image.
     */
    public AlfrescoContainer() {
        this(DEFAULT_REPOSITORY_DOCKER_IMAGE);
    }

    /**
     * Constructor with a custom Docker image.
     *
     * @param dockerImageName The Docker image to use for the Alfresco Repository.
     */
    public AlfrescoContainer(String dockerImageName) {
        super(dockerImageName);
        this.network = Network.newNetwork();
    }

    /**
     * Constructs custom Java options when ActiveMQ is used, updating the messaging broker URL.
     *
     * @return A string containing the updated Java options.
     */
    private String getActivemqJavaOpts() {
        String activemqBrokerUrl = "-Dmessaging.broker.url=\"failover:(nio://"
                + activemqContainer.getNetworkAliases().get(0)
                + ":61616)?timeout=3000&jms.useCompression=true\"";
        return DEFAULT_JAVA_OPTS
                .replace("-Drepo.event2.enabled=false -Dmessaging.subsystem.autoStart=false ", "")
                + " " + activemqBrokerUrl;
    }

    /**
     * Configures the Alfresco container with the necessary environment variables, network, and wait strategy.
     * This method is automatically called during the container initialization.
     */
    @Override
    protected void configure() {

        if (postgresContainer == null) {
            throw new IllegalStateException("PostgreSQLContainer is required to run Alfresco Repository");
        }

        String javaOpts = activemqContainer != null ? getActivemqJavaOpts() : DEFAULT_JAVA_OPTS;

        withEnv("JAVA_TOOL_OPTIONS", DEFAULT_JAVA_TOOL_OPTIONS);
        withEnv("JAVA_OPTS", javaOpts);
        withNetwork(network);
        withNetworkAliases("alfresco");
        withExposedPorts(8080);

        waitingFor(new HttpWaitStrategy()
                .forPort(8080)
                .forPath("/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-")
                .forStatusCodeMatching(response -> response == 200)
                .withStartupTimeout(Duration.ofMinutes(3)));
    }

    /**
     * Gets the network shared by the containers.
     *
     * @return The shared network.
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Sets the PostgreSQL container to be used by the Alfresco container.
     *
     * @param postgreSQLContainer The PostgreSQL container.
     */
    public void setPostgreSQLContainer(PostgreSQLContainer<?> postgreSQLContainer) {
        this.postgresContainer = postgreSQLContainer;
    }

    /**
     * Gets the PostgreSQL container being used.
     *
     * @return The PostgreSQL container.
     */
    public PostgreSQLContainer<?> getPostgreSQLContainer() {
        return this.postgresContainer;
    }

    /**
     * Creates and configures a default PostgreSQL container for Alfresco.
     *
     * @return The configured PostgreSQL container.
     */
    public PostgreSQLContainer<?> createDefaultPostgreSQLContainer() {
        this.postgresContainer = new PostgreSQLContainer<>("postgres:14.4")
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withPassword("alfresco")
                .withUsername("alfresco")
                .withDatabaseName("alfresco")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
        return this.postgresContainer;
    }

    /**
     * Sets the ActiveMQ container to be used by the Alfresco container.
     *
     * @param activemqContainer The ActiveMQ container.
     */
    public void setActivemqContainer(GenericContainer<?> activemqContainer) {
        this.activemqContainer = activemqContainer;
    }

    /**
     * Gets the ActiveMQ container being used.
     *
     * @return The ActiveMQ container.
     */
    public GenericContainer<?> getActivemqContainer() {
        return this.activemqContainer;
    }

    /**
     * Creates and configures a default ActiveMQ container for Alfresco.
     *
     * @return The configured ActiveMQ container.
     */
    public GenericContainer<?> createDefaultActivemqContainer() {
        this.activemqContainer = new GenericContainer<>("alfresco/alfresco-activemq:5.18-jre17-rockylinux8")
                .withNetwork(network)
                .withNetworkAliases("activemq")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedPorts(61616, 8161);
        return this.activemqContainer;
    }

    /**
     * Starts the Alfresco container along with the required PostgreSQL container
     * and optionally, the ActiveMQ container.
     */
    public void startServices() {
        postgresContainer.start();
        if (activemqContainer != null) {
            activemqContainer.start();
        }
        this.start();
    }
}
