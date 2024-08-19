package org.alfresco.handler;

import org.alfresco.event.sdk.handling.filter.EventFilter;
import org.alfresco.event.sdk.handling.filter.IsFileFilter;
import org.alfresco.event.sdk.handling.filter.MimeTypeFilter;
import org.alfresco.event.sdk.handling.handler.OnNodeCreatedEventHandler;
import org.alfresco.repo.event.v1.model.DataAttributes;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlContentCreatedHandler implements OnNodeCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlContentCreatedHandler.class);

    @Override
    public void handleEvent(RepoEvent<DataAttributes<Resource>> event) {
        final NodeResource nodeResource = (NodeResource) event.getData().getResource();
        LOGGER.info("An HTML content named {} has been created!", nodeResource.getName());
    }

    @Override
    public EventFilter getEventFilter() {
        return IsFileFilter.get()
                .and(MimeTypeFilter.of("text/html"));
    }

}
