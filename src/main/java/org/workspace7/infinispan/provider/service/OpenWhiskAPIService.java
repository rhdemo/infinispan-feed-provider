package org.workspace7.infinispan.provider.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;

import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.workspace7.infinispan.provider.config.OpenWhiskProperties;
import org.workspace7.infinispan.provider.data.EventPayload;
import org.workspace7.infinispan.provider.data.TriggerData;
import org.workspace7.infinispan.provider.data.TriggerRequest;
import org.workspace7.infinispan.provider.service.functions.PostTrigger;
import org.workspace7.infinispan.provider.util.Utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class OpenWhiskAPIService {

  @Autowired
  private OpenWhiskProperties openWhiskProperties;

  @Autowired
  TriggerDataService triggerDataService;

  @Autowired
  private PostTrigger postTrigger;

  Flux<TriggerData> fluxOfTriggers = Flux.empty();

  @PostConstruct
  protected void init() {
    try {
      fluxOfTriggers = triggerDataService.findAll();
    } catch (Exception e) {
      log.error("Unable to find triggers from DB", e);
    }
  }

  /**
   * @param payload
   * @return
   */
  public Flux<JsonObject> invokeTriggers(EventPayload payload) {
    log.info("Invoking Triggers with Payload {} ", payload);
    Flux<JsonObject> triggerResponses;
    triggerResponses = fluxOfTriggers.map(triggerData -> {
      TriggerRequest.TriggerRequestBuilder requestBuilder = TriggerRequest.builder();
      try {
        return requestBuilder.triggerName(triggerData.getTriggerName()).eventPayload(payload)
            .auth(triggerData.getAuthKey())
            .uri(new URI(openWhiskProperties.getApiHost() + "/" + Utils.shortTriggerID(triggerData.getTriggerName())))
            .build();
      } catch (URISyntaxException e) {
        return null;
      }
    }).filter(triggerRequest -> triggerRequest != null).map(postTrigger);
    return triggerResponses;
  }

}
