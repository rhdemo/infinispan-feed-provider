package org.workspace7.infinispan.provider.controller;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data")
public class DataController {

  private static final Logger log = LoggerFactory.getLogger(DataController.class);

  RemoteCacheManager remoteCacheManager;

  RemoteCache<Object, Object> remoteCache;

  public DataController(RemoteCacheManager remoteCacheManager) {
    this.remoteCacheManager = remoteCacheManager;
    this.remoteCache = remoteCacheManager.getCache("default");
  }


  @RequestMapping(value = "/add/{key}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void putTestData(@PathVariable("key") String key, @RequestBody String data) {
    try {
      if (data == null) {
        data = String.valueOf(System.currentTimeMillis());
      }
      remoteCache.put(key, data);
    } catch (Exception e) {
      log.error("Error Adding data key :" + key, e);
    }
  }
}
