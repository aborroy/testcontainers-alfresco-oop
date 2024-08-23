package org.alfresco.event.sdk.integration.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.enterprise.repo.event.v1.model.EnterpriseEventData;
import org.alfresco.event.sdk.handling.EventHandlingException;
import org.alfresco.repo.event.databind.ObjectMapperFactory;
import org.alfresco.repo.event.v1.model.DataAttributes;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericTransformer;

/**
 * A transformer that converts a JSON string representation of an event into a {@link RepoEvent} object.
 * This transformer handles both standard Alfresco events and Alfresco Enterprise events.
 * <p>
 * If the JSON contains the {@link #ONLY_ENTERPRISE_PROPERTY}, it is treated as an Enterprise event.
 * Otherwise, it is treated as a standard event.
 * </p>
 *
 * This class was created as a patch for handling events in a specific Alfresco issue (MNT-24580).
 */
public class EventGenericTransformer implements GenericTransformer<String, RepoEvent<DataAttributes<Resource>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventGenericTransformer.class);

    /**
     * A unique property that distinguishes an Alfresco Enterprise event from a standard event.
     */
    public static final String ONLY_ENTERPRISE_PROPERTY = "resourceReaderAuthorities";

    /**
     * The ObjectMapper used for deserializing JSON into event objects.
     * The ObjectMapper is configured via the {@link ObjectMapperFactory}.
     */
    private final ObjectMapper objectMapper = new ObjectMapperFactory().createObjectMapper();

    /**
     * Transforms a JSON string into a {@link RepoEvent} object. The transformation logic distinguishes
     * between standard and Enterprise events based on the presence of the {@link #ONLY_ENTERPRISE_PROPERTY}.
     *
     * @param eventJSON the JSON string representing the event.
     * @return the deserialized {@link RepoEvent} object.
     * @throws EventHandlingException if an error occurs during JSON processing.
     */
    @Override
    public RepoEvent<DataAttributes<Resource>> transform(final String eventJSON) {
        LOGGER.debug("Transforming JSON event: {}", eventJSON);
        try {
            // Determine if the event is an Enterprise event by checking for a specific property
            boolean enterpriseEvent = eventJSON.contains(ONLY_ENTERPRISE_PROPERTY);

            // Deserialize JSON to the appropriate event type based on whether it's an Enterprise event
            if (enterpriseEvent) {
                return (RepoEvent<DataAttributes<Resource>>) (RepoEvent<?>)
                        objectMapper.readValue(eventJSON, new TypeReference<RepoEvent<EnterpriseEventData<Resource>>>() {
                        });
            } else {
                return (RepoEvent<DataAttributes<Resource>>) (RepoEvent<?>)
                        objectMapper.readValue(eventJSON, new TypeReference<RepoEvent<EventData<Resource>>>() {
                        });
            }
        } catch (final JsonProcessingException excp) {
            LOGGER.error("An error occurred while transforming the JSON event: {}", eventJSON, excp);
            throw new EventHandlingException("An error occurred while transforming the JSON event", excp);
        }
    }
}
