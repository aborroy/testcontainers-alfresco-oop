package org.alfresco.handler;

import org.alfresco.enterprise.repo.event.v1.model.EnterpriseEventData;
import org.alfresco.event.sdk.handling.filter.EventFilter;
import org.alfresco.event.sdk.handling.filter.IsFileFilter;
import org.alfresco.event.sdk.handling.filter.MimeTypeFilter;
import org.alfresco.event.sdk.handling.handler.OnNodeCreatedEventHandler;
import org.alfresco.repo.event.v1.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles events related to the creation of HTML content nodes in Alfresco.
 * It implements the {@link OnNodeCreatedEventHandler} interface to handle node creation events.
 * Specifically, it filters for events where the created node is a file with the MIME type "text/html".
 * <p/>
 * The handler logs a message when an HTML content node is created, including the name of the node.
 */
@Component
public class HtmlContentCreatedHandler implements OnNodeCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);

    /**
     * Handles the event when a new node is created in the Alfresco repository.
     * This method is triggered only for nodes that are files with the MIME type "text/html".
     *
     * @param event the {@link RepoEvent} containing the data about the created node.
     */
    @Override
    public void handleEvent(RepoEvent<DataAttributes<Resource>> event) {
        final NodeResource nodeResource = (NodeResource) event.getData().getResource();
        LOGGER.info("An HTML content named {} has been created!", nodeResource.getName());
        if (event.getData() instanceof EnterpriseEventData<?> enterpriseEventData) {
            LOGGER.info("Permissions - reader authorities: {}", enterpriseEventData.getResourceReaderAuthorities());
            LOGGER.info("Permissions - denied authorities: {}", enterpriseEventData.getResourceDeniedAuthorities());
            LOGGER.info("Permissions - reader security controls: {}", enterpriseEventData.getResourceReaderSecurityControls());
        }
    }

    /**
     * Specifies the filter criteria for the events that this handler should process.
     * The filter is configured to include only events where the created node is a file
     * and has the MIME type specified by {@code content.mime-type}.
     * <p/>
     * Out-of-the-Box filters can be used, available in the package org.alfresco.event.sdk.handling.filter, or a custom filter can be built.
     *
     * @return an {@link EventFilter} that filters for specific content creation events.
     */
    @Override
    public EventFilter getEventFilter() {
        return IsFileFilter.get()
                .and(MimeTypeFilter.of("text/html"));
    }

}
