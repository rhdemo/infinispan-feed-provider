package org.workspace7.infinispan.provider.controller;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.workspace7.infinispan.provider.data.TriggerData;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;
import org.workspace7.infinispan.provider.service.TriggerDataService;
import org.workspace7.infinispan.provider.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/feed")
@Slf4j
public class ProviderController {

  private final OpenWhiskAPIService openWhiskAPIService;
  private final RemoteCacheManager remoteCacheManager;
  private final TriggerDataService triggerDataService;

  @Autowired
  public ProviderController(RemoteCacheManager remoteCacheManager,
                            OpenWhiskAPIService openWhiskAPIService,
                            TriggerDataService triggerDataService) {
    this.remoteCacheManager = remoteCacheManager;
    this.openWhiskAPIService = openWhiskAPIService;
    this.triggerDataService = triggerDataService;
  }

  /**
   * @param data
   * @return
   */
  @RequestMapping(value = "/listener", method = RequestMethod.POST, produces = "application/json")
  public ResponseEntity<String> addFeedToTrigger(@RequestBody Map<String, String> data) {

    log.info("Input Data: {}", data);

    String cacheName = "default";

    //TODO validations
    if (!data.isEmpty()) {
      if (data.containsKey("cache_name")) {
        cacheName = data.get("cache_name");
      }

      TriggerData triggerData = buildTriggerData(data);

      RemoteCache remoteCache = remoteCacheManager.getCache(cacheName);
      if (remoteCache != null) {
        FeedCacheListener feedCacheListener = new FeedCacheListener(openWhiskAPIService);
        if (!remoteCache.getListeners().contains(feedCacheListener)) {
          log.info("Adding Cache Listener to cache {}", cacheName);
          remoteCache.addClientListener(feedCacheListener);
          triggerData.setCacheName(cacheName);
          triggerData.setFeedCacheListener(feedCacheListener);
          triggerDataService.saveOrUpdate(triggerData);
        }
      }

      final JsonObject response = new JsonObject();
      response.addProperty("status", String.valueOf(HttpStatus.OK));
      response.addProperty("message", String.format("Successfully enabled Listener for %s", cacheName));
      return ResponseEntity.ok(response.toString());
    } else {
      return ResponseEntity.badRequest().body("Request data is not valid or empty");
    }
  }

  private TriggerData buildTriggerData(Map<String, String> data) {
    return TriggerData.builder().authKey(
      data.get("authKey"))
      .triggerName(data.get("triggerName"))
      .triggerShortName(Utils.shortTriggerID(data.get("triggerName")))
      .hotrodServerHost(data.get("hotrod_server_host"))
      .hotrodServerPort("hotrod_port").build();
  }

  /**
   * @param triggerName
   * @return
   */
  @RequestMapping(value = "/listener/{triggerName}", method = RequestMethod.DELETE)
  public ResponseEntity removeFeedToTrigger(@PathVariable("triggerName") String triggerName) {
    try {
      String triggerID = new String(Base64.getDecoder().decode(triggerName.getBytes("US-ASCII")));
      log.info("Disassociating Trigger {}", triggerID);
      //TODO WIP Logic to remove
      Optional<TriggerData> triggerData = triggerDataService.getDocumentById(triggerID);
      if (triggerData.isPresent()) {
        final FeedCacheListener feedCacheListener = new FeedCacheListener(openWhiskAPIService);
        RemoteCache remoteCache = remoteCacheManager.getCache(triggerData.get().getCacheName());
        if (remoteCache != null) {
          if (remoteCache.getListeners().contains(feedCacheListener)) {
            remoteCache.removeClientListener(feedCacheListener);
          }
        }
        triggerDataService.deleleteDoc(triggerID);
      }
    } catch (UnsupportedEncodingException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
    return ResponseEntity.noContent().build();
  }

}
