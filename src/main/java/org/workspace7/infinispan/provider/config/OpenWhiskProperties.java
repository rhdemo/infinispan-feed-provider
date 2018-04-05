package org.workspace7.infinispan.provider.config;


import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;


@Component
@ConfigurationProperties(prefix = "openwhisk")
public class OpenWhiskProperties {
  @NotEmpty
  @URL
  private String apiHost;

  public OpenWhiskProperties() {
  }

  public OpenWhiskProperties(@NotEmpty @URL String apiHost) {
    this.apiHost = apiHost;
  }

  public String getApiHost() {
    return apiHost;
  }

  public void setApiHost(String apiHost) {
    this.apiHost = apiHost;
  }
}
