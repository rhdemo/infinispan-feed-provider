package org.workspace7.infinispan.provider.data;

public class EventPayload {
  String eventType;
  String key;
  String value;
  long timestamp;

  public EventPayload() {
  }

  public EventPayload(String eventType, String key, String value, long timestamp) {
    this.eventType = eventType;
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
