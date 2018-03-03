package org.workspace7.infinispan.provider.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;

@Configuration
@EnableConfigurationProperties(OpenWhiskProperties.class)
public class InfinispanProviderConfiguration {

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public OpenWhiskAPIService openWhiskAPIService(OpenWhiskProperties openWhiskProperties, RestTemplate restTemplate) {
    return new OpenWhiskAPIService(openWhiskProperties, restTemplate);
  }

  @Bean
  public FeedCacheListener defaultCacheListener(OpenWhiskAPIService openWhiskAPIService) {
    return new FeedCacheListener(openWhiskAPIService);
  }
}
