package org.workspace7.infinispan.provider.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPayload {
  String eventType;
  String key;
  String verison;
  @Builder.Default
  String value = "N.A";
}
