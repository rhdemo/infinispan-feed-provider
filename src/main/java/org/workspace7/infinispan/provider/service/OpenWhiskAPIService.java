package org.workspace7.infinispan.provider.service;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.workspace7.infinispan.provider.config.OpenWhiskProperties;
import org.workspace7.infinispan.provider.data.EventPayload;
import org.workspace7.infinispan.provider.data.TriggerData;
import org.workspace7.infinispan.provider.data.TriggerRequest;
import org.workspace7.infinispan.provider.service.functions.PostTrigger;
import org.workspace7.infinispan.provider.util.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenWhiskAPIService {

  private static final Logger log = LoggerFactory.getLogger(OpenWhiskAPIService.class);

  @Autowired
  private OpenWhiskProperties openWhiskProperties;

  @Autowired
  TriggerDataService triggerDataService;

  @Autowired
  private PostTrigger postTrigger;

  /**
   * @param payload
   * @return
   */
  public List<JsonObject> invokeTriggers(String cacheName, EventPayload payload) {
    log.info("Invoking Triggers for Cache {} with Payload {} ", cacheName, payload);

    List<TriggerData> fluxOfTriggers = triggerDataService.findAllByCache(cacheName);

    return fluxOfTriggers.stream().map(triggerData -> {
      final TriggerRequest requestBuilder = new TriggerRequest();
      try {
        requestBuilder.setTriggerName(triggerData.getTriggerName());
        requestBuilder.setAuth(triggerData.getAuthKey());
        requestBuilder.setEventPayload(payload);
        requestBuilder.setUri(new URI(openWhiskProperties.getApiHost() + "/"
          + Utils.shortTriggerID(triggerData.getTriggerName())));
        return requestBuilder;
      } catch (URISyntaxException e) {
        return null;
      }
    }).filter(triggerRequest -> triggerRequest != null)
      .map(postTrigger)
      .collect(Collectors.toList());
  }

}
