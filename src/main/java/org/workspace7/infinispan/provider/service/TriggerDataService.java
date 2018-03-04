package org.workspace7.infinispan.provider.service;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.springframework.stereotype.Service;
import org.workspace7.infinispan.provider.data.CouchDBClient;
import org.workspace7.infinispan.provider.data.DBAlreadyExistsException;
import org.workspace7.infinispan.provider.data.DocumentNotFoundException;
import org.workspace7.infinispan.provider.data.TriggerData;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.util.Utils;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class TriggerDataService {

  private final Gson gson;
  private final RemoteCacheManager remoteCacheManager;
  private final OpenWhiskAPIService openWhiskAPIService;
  private CouchDBClient couchDBClient;

  private static final String DB_NAME = "infinispanfeedtriggers";

  private static final String[] SELECTOR_FIELDS = {"authKey", "triggerName", "cacheName", "triggerShortName"};

  public TriggerDataService(CouchDBClient couchDBClient, Gson gson,
                            RemoteCacheManager remoteCacheManager,
                            OpenWhiskAPIService openWhiskAPIService) {
    this.couchDBClient = couchDBClient;
    this.gson = gson;
    this.remoteCacheManager = remoteCacheManager;
    this.openWhiskAPIService = openWhiskAPIService;
  }

  @PostConstruct
  protected void init() {
    try {
      JsonObject response = couchDBClient.createDB(DB_NAME);
      Objects.equals(response.get("ok").getAsBoolean(), true);
    } catch (DBAlreadyExistsException e) {
      log.info("DB {} already exists, skipping creation", DB_NAME);
    }
    reconstructListenersFromDB();
  }

  private void reconstructListenersFromDB() {
    log.info("Reconstructing all Listeners");
    findAll()
      .map(triggerData -> remoteCacheManager.getCache(triggerData.getCacheName()))
      .filter(remoteCache -> remoteCache != null)
      .map(remoteCache -> {
        log.info("Reconstructing Listener to Cache : {}", remoteCache);
        FeedCacheListener feedCacheListener = new FeedCacheListener(openWhiskAPIService);
        remoteCache.addClientListener(feedCacheListener);
        return feedCacheListener;
      }).subscribe(); //not bothered about return values

  }

  /**
   * @param triggerData
   * @return
   */
  public boolean saveOrUpdate(TriggerData triggerData) {
    String docId = Utils.base64Encoded(triggerData.getTriggerName());
    JsonObject doc = null;
    log.info("Saving Document {} with ID {}", triggerData, docId);
    Optional<TriggerData> optionalTriggerData = getDocumentById(docId);
    if (optionalTriggerData.isPresent()) {
      String revision = optionalTriggerData.get().getRevision();
      triggerData.setRevision(revision);
      doc = couchDBClient.saveDoc(DB_NAME, docId, triggerData);
      log.info("Updated Document {}", doc);
    } else {
      doc = couchDBClient.saveDoc(DB_NAME, docId, triggerData);
      log.info("Saved Document {}", doc);
    }
    return doc != null ? doc.get("ok").getAsBoolean() : false;
  }

  /**
   * @param docId
   * @return
   */
  public boolean deleleteDoc(String docId) {
    Optional<TriggerData> optionalTriggerData = getDocumentById(docId);
    if (optionalTriggerData.isPresent()) {
      String revision = optionalTriggerData.get().getRevision();
      JsonObject response = couchDBClient.deleteDoc(DB_NAME, docId, revision);
      return response != null ? response.get("ok").getAsBoolean() : false;
    } else {
      log.warn("Document with ID {} not found in DB {}", docId, DB_NAME);
      return false;
    }
  }

  /**
   * @param docId
   * @return
   */
  public Optional<TriggerData> getDocumentById(String docId) {
    log.info("Getting Document with ID {} " + docId);
    TriggerData triggerData = null;
    try {
      JsonObject doc = couchDBClient.getDocumentById(DB_NAME, docId);
      log.info("Document Retrieved {}", doc);
      if (doc != null && doc.get("ok").getAsBoolean()) {
        triggerData = gson.fromJson(doc, TriggerData.class);
      }
    } catch (DocumentNotFoundException e) {
      log.warn("Document with ID {} not found in DB {}", docId, DB_NAME);
    }
    return Optional.ofNullable(triggerData);
  }

  /**
   * TODO - need to optimize for getting reactively with backpressure
   *
   * @return
   */
  public Flux<TriggerData> findAll() {
    log.info("Finding All Documents");
    JsonObject request = requestSelector();
    JsonObject response = couchDBClient.allDocs(DB_NAME, request);
    if (response.has("docs")) {
      JsonArray jsonElements = response.get("docs").getAsJsonArray();
      log.debug("Got {} documents", jsonElements.size());
      return Flux.fromIterable(jsonElements).map(e -> {
        log.debug("JSON Element {}", e);
        return gson.fromJson(e, TriggerData.class);
      });
    } else {
      return Flux.empty();
    }
  }

  /**
   * @return
   */
  private JsonObject requestSelector() {
    JsonObject request = new JsonObject();
    JsonObject triggerNameSelector = new JsonObject();
    JsonObject triggerRegEx = new JsonObject();
    triggerRegEx.addProperty("$regex", "^(.*)");
    triggerNameSelector.add("triggerName", triggerRegEx);
    request.add("selector", triggerNameSelector);
    JsonArray fields = new JsonArray();
    for (String field : SELECTOR_FIELDS) {
      fields.add(field);
    }
    return request;
  }


}
