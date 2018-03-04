package org.workspace7.infinispan.provider.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
}
