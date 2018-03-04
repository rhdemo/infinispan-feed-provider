package org.workspace7.infinispan.provider.listener;

import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.springframework.stereotype.Component;
import org.workspace7.infinispan.provider.data.EventPayload;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;

@Component
@ClientListener
@Slf4j
public class FeedCacheListener {

  OpenWhiskAPIService openWhiskAPIService;

  public FeedCacheListener(OpenWhiskAPIService openWhiskAPIService) {
    this.openWhiskAPIService = openWhiskAPIService;
  }

  @ClientCacheEntryCreated
  public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
    EventPayload eventData = toPayload("CREATE", e.getKey(), e.getVersion());
    log.info("Create Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers(eventData).subscribe();
  }


  @ClientCacheEntryModified
  public void handleModifiedEvent(ClientCacheEntryModifiedEvent e) {
    EventPayload eventData = toPayload("UPDATE", e.getKey(), e.getVersion());
    log.info("Modified Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers(eventData).subscribe();
  }

  @ClientCacheEntryRemoved
  public void handleRemovedEvent(ClientCacheEntryRemovedEvent e) {
    EventPayload eventData = toPayload("REMOVE", e.getKey(), -1);
    log.info("Removed Event {} and data {} ", e.getType(), eventData.toString());
    openWhiskAPIService.invokeTriggers(eventData).subscribe();
  }

  private EventPayload toPayload(String eventType, Object key, long version) {
    return EventPayload.builder()
      .eventType(eventType)
      .key(String.valueOf(key))
      .verison(String.valueOf(version))
      .build();
  }

}
