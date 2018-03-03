package org.workspace7.infinispan.provider.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/data")
public class DataController {

  RemoteCacheManager remoteCacheManager;

  RemoteCache<Object, Object> remoteCache;

  @Autowired
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
