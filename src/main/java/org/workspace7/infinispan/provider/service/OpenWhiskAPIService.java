package org.workspace7.infinispan.provider.service;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.workspace7.infinispan.provider.config.OpenWhiskProperties;
import org.workspace7.infinispan.provider.util.Utils;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
public class OpenWhiskAPIService {

  OpenWhiskProperties openWhiskProperties;

  RestTemplate restTemplate;

  public OpenWhiskAPIService(OpenWhiskProperties openWhiskProperties, RestTemplate restTemplate) {
    this.openWhiskProperties = openWhiskProperties;
    this.restTemplate = restTemplate;
  }

  public JsonObject invokeTriggers(EventPayload payload, Map<String, TriggerData> triggerDataMap) {
    log.info("Invoking Triggers with Payload {} ", payload);
    JsonObject jsonObject = new JsonObject();

    triggerDataMap.forEach((s, triggerData) -> {
      try {

        final URI triggerURI = new URI(
          openWhiskProperties.getApiHost() + "/" + Utils.shortTriggerID(s));

        log.info("Trigger URI {}", triggerURI.toString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.set("Authorization", "Basic " + Utils.base64Encoded(triggerData.getAuthKey()));

        HttpEntity<EventPayload> requestEntity = new HttpEntity<>(payload, httpHeaders);

        ResponseEntity<String> response = restTemplate.exchange(triggerURI, HttpMethod.POST, requestEntity, String.class);

        log.info("Status: {}  Response body:{}", response.getStatusCode().value(), response.getBody());

        jsonObject.addProperty("done", true);
        jsonObject.addProperty("status", response.getStatusCode().toString());
        jsonObject.addProperty("response", response.getBody());


      } catch (Exception e1) {
        log.error("Error with trigger " + s, e1);
        jsonObject.addProperty("done", false);
        jsonObject.addProperty("error", e1.getMessage());
      }
    });

    return jsonObject;
  }


}
