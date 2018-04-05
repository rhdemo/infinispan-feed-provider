package org.workspace7.infinispan.provider.listener;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.commons.util.KeyValueWithPrevious;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.workspace7.infinispan.provider.data.EventPayload;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;

@Component
@ClientListener(converterFactoryName = "key-value-with-previous-converter-factory")
//TODO async http call
public class FeedCacheListener {

  private static final Logger log = LoggerFactory.getLogger(FeedCacheListener.class);

  OpenWhiskAPIService openWhiskAPIService;

  public FeedCacheListener(OpenWhiskAPIService openWhiskAPIService) {
    this.openWhiskAPIService = openWhiskAPIService;
  }

  @ClientCacheEntryCreated
  public void handleCreatedEvent(ClientCacheEntryCustomEvent<KeyValueWithPrevious<String, String>> e) {
    KeyValueWithPrevious<String, String> pair = e.getEventData();
    EventPayload eventData = toPayload("CREATE", pair.getKey(), pair.getValue());
    log.info("Create Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers("need-to-update", eventData);
  }


  @ClientCacheEntryModified
  public void handleModifiedEvent(ClientCacheEntryCustomEvent<KeyValueWithPrevious<String, String>> e) {
    KeyValueWithPrevious<String, String> pair = e.getEventData();
    EventPayload eventData = toPayload("UPDATE", pair.getKey(), pair.getValue());
    log.info("Modified Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers("need-to-update", eventData);
  }

  @ClientCacheEntryRemoved
  public void handleRemovedEvent(ClientCacheEntryCustomEvent<KeyValueWithPrevious<String, String>> e) {
    KeyValueWithPrevious<String, String> pair = e.getEventData();
    EventPayload eventData = toPayload("REMOVE", pair.getKey(), pair.getPrev());
    log.info("Removed Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers("need-to-update", eventData);
  }

  private EventPayload toPayload(String eventType, String key, String value) {
    return new EventPayload(eventType, key, value, System.currentTimeMillis());
  }

}
