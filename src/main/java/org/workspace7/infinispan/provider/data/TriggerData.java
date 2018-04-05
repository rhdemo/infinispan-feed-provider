package org.workspace7.infinispan.provider.data;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;

public class TriggerData {

  @Expose
  private String authKey;
  @Expose
  private String cacheName;
  @Expose
  private String triggerName;
  @Expose
  private String triggerShortName;
  @Expose
  private String hotrodServerHost;
  @Expose
  private String hotrodServerPort;
  @Expose
  @SerializedName("_rev")
  private String revision;
  //TODO wondering its this not need, check and remove
  private transient FeedCacheListener feedCacheListener;

  public TriggerData() {
  }

  public TriggerData(String authKey, String cacheName, String triggerName,
                     String triggerShortName, String hotrodServerHost, String hotrodServerPort, String revision) {
    this.authKey = authKey;
    this.cacheName = cacheName;
    this.triggerName = triggerName;
    this.triggerShortName = triggerShortName;
    this.hotrodServerHost = hotrodServerHost;
    this.hotrodServerPort = hotrodServerPort;
    this.revision = revision;
  }

  public String getAuthKey() {
    return authKey;
  }

  public void setAuthKey(String authKey) {
    this.authKey = authKey;
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public String getTriggerName() {
    return triggerName;
  }

  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  public String getTriggerShortName() {
    return triggerShortName;
  }

  public void setTriggerShortName(String triggerShortName) {
    this.triggerShortName = triggerShortName;
  }

  public String getHotrodServerHost() {
    return hotrodServerHost;
  }

  public void setHotrodServerHost(String hotrodServerHost) {
    this.hotrodServerHost = hotrodServerHost;
  }

  public String getHotrodServerPort() {
    return hotrodServerPort;
  }

  public void setHotrodServerPort(String hotrodServerPort) {
    this.hotrodServerPort = hotrodServerPort;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public FeedCacheListener getFeedCacheListener() {
    return feedCacheListener;
  }

  public void setFeedCacheListener(FeedCacheListener feedCacheListener) {
    this.feedCacheListener = feedCacheListener;
  }
}
