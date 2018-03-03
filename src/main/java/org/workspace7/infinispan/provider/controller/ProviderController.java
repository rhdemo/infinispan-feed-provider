package org.workspace7.infinispan.provider.controller;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;
import org.workspace7.infinispan.provider.service.TriggerData;
import org.workspace7.infinispan.provider.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
@Slf4j
public class ProviderController {

  private final OpenWhiskAPIService openWhiskAPIService;
  RemoteCacheManager remoteCacheManager;

  //TODO neater and better logic via some persistent DB
  public static final Hashtable<String, TriggerData> triggerMap = new Hashtable<>();

  @Autowired
  public ProviderController(RemoteCacheManager remoteCacheManager, OpenWhiskAPIService openWhiskAPIService) {
    this.remoteCacheManager = remoteCacheManager;
    this.openWhiskAPIService = openWhiskAPIService;
  }

  @RequestMapping(value = "/listener", method = RequestMethod.POST, produces = "application/json")
  public ResponseEntity<String> addFeedToTrigger(@RequestBody Map<String, String> data) {

    log.info("Input Data: {}", data);

    String cacheName = "default";

    //TODO validations
    if (!data.isEmpty()) {
      if (data.containsKey("cache_name")) {
        cacheName = data.get("cache_name");
      }
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
        triggerMap.put(data.get("triggerName"), triggerData);
      }
    }

    final JsonObject response = new JsonObject();
    response.addProperty("status", String.valueOf(HttpStatus.OK));
    response.addProperty("message", String.format("Successfully enabled Listener for %s", cacheName));

    return ResponseEntity.ok(response.toString());
  }

  private TriggerData buildTriggerData(Map<String, String> data) {
    return TriggerData.builder().authKey(
            data.get("authKey"))
            .triggerShortName(Utils.shortTriggerID(data.get("triggerName")))
            .hotrodServerHost(data.get("hotrod_server_host"))
            .hotrodServerPort("hotrod_port").build();
  }

  @RequestMapping(value = "/listener/{triggerName}", method = RequestMethod.DELETE)
  public ResponseEntity removeFeedToTrigger(@PathVariable("triggerName") String triggerName) {
    try {
      String triggerID = new String(Base64.getDecoder().decode(triggerName.getBytes("US-ASCII")));
      log.info("Disassociating Trigger {}", triggerID);
      if (triggerMap.containsKey(triggerID)) {
        TriggerData triggerData = triggerMap.get(triggerID);
        FeedCacheListener feedCacheListener = triggerData.getFeedCacheListener();
        RemoteCache remoteCache = remoteCacheManager.getCache(triggerData.getCacheName());
        if (remoteCache != null) {
          if (remoteCache.getListeners().contains(feedCacheListener)) {
            remoteCache.removeClientListener(feedCacheListener);
          }
        }
      } else {
        log.info("Trigger {} does not exist in store", triggerID);
      }
    } catch (UnsupportedEncodingException e) {
      //nothing to do
      return ResponseEntity.badRequest().build();
    }
    //TODO
    return ResponseEntity.noContent().build();
  }

}
