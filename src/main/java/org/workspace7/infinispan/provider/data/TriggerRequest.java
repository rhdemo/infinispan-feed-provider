package org.workspace7.infinispan.provider.data;

import java.net.URI;

public class TriggerRequest {
  private String triggerName;
  private String auth;
  private URI uri;
  private EventPayload eventPayload;

  public TriggerRequest() {
  }

  public TriggerRequest(String triggerName, String auth, URI uri, EventPayload eventPayload) {
    this.triggerName = triggerName;
    this.auth = auth;
    this.uri = uri;
    this.eventPayload = eventPayload;
  }

  public String getTriggerName() {
    return triggerName;
  }

  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public EventPayload getEventPayload() {
    return eventPayload;
  }

  public void setEventPayload(EventPayload eventPayload) {
    this.eventPayload = eventPayload;
  }
}
