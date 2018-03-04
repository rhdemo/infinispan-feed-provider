package org.workspace7.infinispan.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.workspace7.infinispan.provider.data.*;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.util.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CouchDBTest {

  final static String COUCHDB_HOST = "http://couchdb-rest-myproject.192.168.64.65.nip.io";
  final static String COUCHDB_USER = "admin";
  final static String COUCHDB_PASSWORD = "password";

  static CouchDBClient couchDBClient;

  static {
    couchDBClient = Feign.builder().decoder(new GsonDecoder())
      .encoder(new GsonEncoder())
      .logLevel(Logger.Level.FULL)
      .logger(new Slf4jLogger())
      .errorDecoder(new CouchDBErrorDecoder())
      .requestInterceptor(new BasicAuthRequestInterceptor(COUCHDB_USER, COUCHDB_PASSWORD))
      .target(CouchDBClient.class, COUCHDB_HOST);
  }

  @Test
  public void createDB() {
    try {
      JsonObject response = couchDBClient.createDB("testtriggers");
      assertThat(response).isNotNull();
      assertThat(response.get("ok").getAsBoolean()).isTrue();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test(expected = DBAlreadyExistsException.class)
  public void createSameDBAgain() throws Exception {
    couchDBClient.createDB("testtriggers");
  }

  @Test
  public void deleteDB() {
    try {
      JsonObject response = couchDBClient.deleteDB("testtriggers");
      assertThat(response).isNotNull();
      assertThat(response.get("ok").getAsBoolean()).isTrue();
    } catch (DBDoesNotExistsException e) {
      fail("Should not throw this exception as db testtriggers exists");
    }
  }

  @Test(expected = DBDoesNotExistsException.class)
  public void deleteNotExistentDB() throws Exception {
    couchDBClient.deleteDB("foo");
  }


  @Test
  public void saveDoc() {
    TriggerData triggerData = buildDesinDoc();
    String docId = Utils.base64Encoded(triggerData.getTriggerName());
    JsonObject doc = null;
    try {
      couchDBClient.getDocumentById("test", docId);
    } catch (DocumentNotFoundException e) {
      System.out.println("Document not Found creating it ");
      JsonObject response = couchDBClient.saveDoc("test", docId, buildDesinDoc());
      assertThat(response).isNotNull();
      assertThat(response.get("ok").getAsBoolean()).isTrue();
      assertThat(Utils.base64Decode(response.get("id").getAsString())).isEqualTo("/_/myTrigger");
    }
  }

  @Test
  public void updateDoc() throws Exception {
    TriggerData triggerData = buildDesinDoc();
    String docId = Utils.base64Encoded(triggerData.getTriggerName());
    JsonObject doc = null;
    doc = couchDBClient.getDocumentById("test", docId);
    assertThat(doc).isNotNull();
    String revision = doc.get("_rev").getAsString();
    triggerData.setRevision(revision);
    triggerData.setCacheName("AnotherCache");
    JsonObject response = couchDBClient.saveDoc("test", docId, triggerData);
    assertThat(response).isNotNull();
    assertThat(response.get("ok").getAsBoolean()).isTrue();
    assertThat(Utils.base64Decode(response.get("id").getAsString())).isEqualTo("/_/myTrigger");
    assertThat(response.get("rev")).isNotEqualTo(revision);

    //Get the document and check
    doc = couchDBClient.getDocumentById("test", docId);
    assertThat(doc.has("_id")).isTrue();
    docId = doc.get("_id").getAsString();
    assertThat(docId).isNotNull();
    assertThat(Utils.base64Decode(docId)).isEqualTo("/_/myTrigger");
    assertThat(doc.get("triggerName").getAsString()).isEqualTo("/_/myTrigger");
    assertThat(doc.get("cacheName").getAsString()).isEqualTo("AnotherCache");
  }

  @Test
  public void xGetDocumentById() throws Exception {
    String docId = Utils.base64Encoded("/_/myTrigger");
    JsonObject response = couchDBClient.getDocumentById("test", docId);
    assertThat(response).isNotNull();
    assertThat(Utils.base64Decode(response.get("_id").getAsString())).isEqualTo("/_/myTrigger");
    assertThat(response.get("triggerName").getAsString()).isEqualTo("/_/myTrigger");
    assertThat(response.get("authKey").getAsString()).isEqualTo("user:changeme");
  }

  @Test
  public void zDeleteDoc() {
    String docId = Utils.base64Encoded("/_/myTrigger");
    try {
      JsonObject doc = couchDBClient.getDocumentById("test", docId);
      JsonObject response = couchDBClient.deleteDoc("test", docId, doc.get("_rev").getAsString());
      assertThat(response).isNotNull();
      assertThat(response.get("ok").getAsBoolean()).isTrue();
    } catch (DocumentNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void zListDocs() {
    loadFewDocs();
    JsonObject request = new JsonObject();
    JsonObject triggerNameSelector = new JsonObject();
    JsonObject triggerRegEx = new JsonObject();
    triggerRegEx.addProperty("$regex", "^(.*)");
    triggerNameSelector.add("triggerName", triggerRegEx);
    request.add("selector", triggerNameSelector);
    JsonArray fields = new JsonArray();
    fields.add("authKey");
    fields.add("cacheName");
    fields.add("triggerName");
    fields.add("triggerShortName");
    request.add("fields", fields);
    JsonObject response = couchDBClient.allDocs("test", request);
    JsonArray jsonElements = response.get("docs").getAsJsonArray();
    assertThat(jsonElements).isNotNull();
    assertThat(jsonElements).isNotEmpty();
    for (int i = 0; i < 10; i++) {
      JsonObject doc = jsonElements.get(i).getAsJsonObject();
      assertThat(doc).isNotNull();
      fields.forEach(e -> {
        assertThat(e.getAsString()).isNotNull();
      });
    }
  }

  private void loadFewDocs() {
    for (int i = 0; i < 10; i++) {
      String docId = "doc" + i;
      TriggerData triggerData = buildDesinDoc();
      triggerData.setTriggerName(triggerData.getTriggerName() + "i");
      couchDBClient.saveDoc("test", docId, triggerData);
    }
  }

  public TriggerData buildDesinDoc() {
    TriggerData triggerData = TriggerData.builder()
      .authKey("user:changeme")
      .hotrodServerHost("localhost")
      .hotrodServerPort("11222")
      .cacheName("default")
      .feedCacheListener(new FeedCacheListener(null))
      .triggerName("/_/myTrigger")
      .triggerShortName("myTrigger")
      .build();
    return triggerData;
  }

  @BeforeClass
  public static void createTestDB() throws Exception {
    try {
      couchDBClient.deleteDB("test");
    } catch (Exception e) {
      //nothing
    }
    couchDBClient.createDB("test");
  }

  @AfterClass
  public static void deleteTestDB() throws DBDoesNotExistsException {
    couchDBClient.deleteDB("test");
  }
}
