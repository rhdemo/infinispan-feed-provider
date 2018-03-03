package org.workspace7.infinispan.provider.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerData {

  private String authKey;
  private String cacheName;
  private String triggerName;
  private String triggerShortName;
  private String hotrodServerHost;
  private String hotrodServerPort;
  private FeedCacheListener feedCacheListener;
}
