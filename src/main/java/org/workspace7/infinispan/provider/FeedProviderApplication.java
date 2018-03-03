package org.workspace7.infinispan.provider;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.spring.starter.remote.InfinispanRemoteCacheCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;

@SpringBootApplication
public class FeedProviderApplication {
  public static void main(String[] args) {
    SpringApplication.run(FeedProviderApplication.class, args);
  }

  @Bean
  public InfinispanRemoteCacheCustomizer customizer() {
    //TODO fix FOR somereason am not able use Version 27
    return b -> b.version(ProtocolVersion.PROTOCOL_VERSION_26);
  }
}
