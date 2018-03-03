package org.workspace7.infinispan.provider.service;

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
  String value = "N.A";
}
