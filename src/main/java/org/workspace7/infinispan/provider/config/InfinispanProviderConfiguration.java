package org.workspace7.infinispan.provider.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.spring.starter.remote.InfinispanRemoteCacheCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.workspace7.infinispan.provider.data.CouchDBClient;
import org.workspace7.infinispan.provider.data.CouchDBErrorDecoder;
import org.workspace7.infinispan.provider.listener.FeedCacheListener;
import org.workspace7.infinispan.provider.service.OpenWhiskAPIService;

import static java.lang.reflect.Modifier.TRANSIENT;

@Configuration
@EnableConfigurationProperties(OpenWhiskProperties.class)
@Slf4j
public class InfinispanProviderConfiguration {

  @Value("${COUCHDB_USER}")
  private String couchdbUser;

  @Value("${COUCHDB_PASSWORD}")
  private String couchdbPassword;

  @Value("${TRIGGERSTORE_SERVICE_HOST}")
  private String couchdbHost = "triggerstore";

  @Value("${TRIGGERSTORE_SERVICE_PORT}")
  private String couchdbPort;

  @Bean
  public InfinispanRemoteCacheCustomizer customizer() {
    //TODO fix FOR somereason am not able use Version 27
    return b -> b.version(ProtocolVersion.PROTOCOL_VERSION_26);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public OpenWhiskAPIService openWhiskAPIService() {
    return new OpenWhiskAPIService();
  }

  @Bean
  public FeedCacheListener defaultCacheListener(OpenWhiskAPIService openWhiskAPIService) {
    return new FeedCacheListener(openWhiskAPIService);
  }

  @Bean
  public CouchDBClient couchDBClient() {
    String couchdbRestURI = String.format("http://%s:%s", couchdbHost, couchdbPort);
    log.info("Using Trigger Store {}", couchdbRestURI);
    return Feign.builder().decoder(new GsonDecoder())
      .encoder(new GsonEncoder())
      .logLevel(Logger.Level.BASIC)
      .logger(new Slf4jLogger())
      .errorDecoder(new CouchDBErrorDecoder())
      .requestInterceptor(new BasicAuthRequestInterceptor(couchdbUser, couchdbPassword))
      .target(CouchDBClient.class, couchdbRestURI);
  }

  @Bean
  public Gson gson() {
    return new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .excludeFieldsWithModifiers(TRANSIENT)
      .create();
  }
}
