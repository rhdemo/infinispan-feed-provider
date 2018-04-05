package org.workspace7.infinispan.provider.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.workspace7.infinispan.provider.data.CouchDBClient;
import org.workspace7.infinispan.provider.data.DBAlreadyExistsException;
import org.workspace7.infinispan.provider.data.DocumentNotFoundException;
import org.workspace7.infinispan.provider.data.TriggerData;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.util.Utils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TriggerDataService {

  private static final Logger log = LoggerFactory.getLogger(TriggerDataService.class);

  private static final String[] SELECTOR_FIELDS = {"authKey", "triggerName", "cacheName", "triggerShortName"};


  private final Gson gson;
  private final RemoteCacheManager remoteCacheManager;
  private final OpenWhiskAPIService openWhiskAPIService;
  private CouchDBClient couchDBClient;

  @Value("${openwhisk.dbName}")
  private String dbName;

  //in memory cache
  Hashtable<String, TriggerData> localTriggerDatas = new Hashtable<>();

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
      JsonObject response = couchDBClient.createDB(dbName);
      Objects.equals(response.get("ok").getAsBoolean(), true);
    } catch (DBAlreadyExistsException e) {
      log.info("DB {} already exists, skipping creation", dbName);
      findAll().forEach(triggerData -> localTriggerDatas.put(Utils.base64Encoded(triggerData.getTriggerName()), triggerData));
    }
    reconstructListenersFromDB();
  }

  private void reconstructListenersFromDB() {
    log.info("Reconstructing all Listeners");
    findAll()
      .stream()
      .map(triggerData -> remoteCacheManager.getCache(triggerData.getCacheName()))
      .filter(remoteCache -> remoteCache != null)
      .map(remoteCache -> {
        log.info("Reconstructing Listener to Cache : {}", remoteCache);
        FeedCacheListener feedCacheListener = new FeedCacheListener(openWhiskAPIService);
        remoteCache.addClientListener(feedCacheListener);
        return feedCacheListener;
      });

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
      doc = couchDBClient.saveDoc(dbName, docId, triggerData);
      log.info("Updated Document {}", doc);
    } else {
      doc = couchDBClient.saveDoc(dbName, docId, triggerData);
      log.info("Saved Document {}", doc);
    }

    //Update local cache
    if (localTriggerDatas.containsKey(docId)) {
      localTriggerDatas.remove(docId);
    }
    localTriggerDatas.put(docId, triggerData);

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
      JsonObject response = couchDBClient.deleteDoc(dbName, docId, revision);
      return response != null ? response.get("ok").getAsBoolean() : false;
    }
    log.warn("Document with ID {} not found in DB {}", docId, dbName);
    return false;
  }

  /**
   * @param docId
   * @return
   */
  public Optional<TriggerData> getDocumentById(String docId) {
    log.info("Getting Document with ID {} " + docId);
    TriggerData triggerData = null;
    try {
      JsonObject doc = couchDBClient.getDocumentById(dbName, docId);
      log.info("Document Retrieved {}", doc);
      if (doc != null) {
        triggerData = gson.fromJson(doc, TriggerData.class);
      }
    } catch (DocumentNotFoundException e) {
      log.warn("Document with ID {} not found in DB {}", docId, dbName);
    }
    return Optional.ofNullable(triggerData);
  }

  /**
   * TODO - need to optimize for getting reactively with backpressure
   *
   * @return
   */
  public List<TriggerData> findAll() {
    log.info("Finding All Documents");
    return this.findAllByCache(null); //default get all available documents irrespective of cache
  }

  /**
   * @param cacheName
   * @return
   */
  public List<TriggerData> findAllByCache(String cacheName) {

    if (localTriggerDatas == null || localTriggerDatas.isEmpty()) {
      log.info("Finding All triggers for Cache {} ", cacheName);
      JsonObject request = requestSelector(SELECTOR_FIELDS[2], cacheName);
      log.info("find query {}" + request);
      JsonObject response = couchDBClient.allDocs(dbName, request);
      List<TriggerData> docs = extractDocs(response, "Got {} documents for query by cache");
      docs.forEach(triggerData -> localTriggerDatas.put(Utils.base64Encoded(triggerData.getTriggerName()), triggerData));
    }

    //Filter from local cache
    List<TriggerData> cacheTriggers = localTriggerDatas
      .values()
      .stream()
      .filter(triggerData -> cacheName.equalsIgnoreCase(triggerData.getCacheName()))
      .collect(Collectors.toList());

    return cacheTriggers;
  }

  /**
   * @param fieldName
   * @param regEx
   * @return
   */
  private JsonObject requestSelector(String fieldName, String regEx) {
    JsonObject request = new JsonObject();

    JsonObject triggerRegEx = new JsonObject();
    triggerRegEx.addProperty("$regex", (regEx == null || regEx.trim().length() == 0) ? "^(.*)" : regEx);

    JsonObject triggerNameSelector = new JsonObject();
    triggerNameSelector.add(fieldName, triggerRegEx);
    request.add((fieldName == null || fieldName.trim().length() == 0) ? "selector" : fieldName, triggerNameSelector);

    JsonArray fields = new JsonArray();
    for (String field : SELECTOR_FIELDS) {
      fields.add(field);
    }

    return request;
  }

  /**
   * @param response
   * @param s
   * @return
   */
  private List<TriggerData> extractDocs(JsonObject response, String s) {
    log.info("Extracing Docs from JSON: {}", response);
    List<TriggerData> docs = new ArrayList<>();
    if (response.has("docs")) {
      JsonArray jsonElements = response.get("docs").getAsJsonArray();
      log.debug(s, jsonElements.size());
      jsonElements.forEach(e -> {
        docs.add(gson.fromJson(e, TriggerData.class));
      });
    }
    return docs;
  }
}
