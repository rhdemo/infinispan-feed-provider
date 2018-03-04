package org.workspace7.infinispan.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.workspace7.infinispan.provider.config.OpenWhiskProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles(profiles = {"test"})
@RunWith(SpringRunner.class)
public class FeedProviderApplicationTest {

  @Autowired
  OpenWhiskProperties openWhiskProperties;

  @Test
  public void isPropertiesAvailable() {
    assertThat(openWhiskProperties).isNotNull();
    assertThat(openWhiskProperties.getApiHost()).isNotNull();
    assertThat(openWhiskProperties.getApiHost()).isEqualTo("http://nginx.openwhisk.svc.cluster.local/api/v1/namespaces/_/triggers");
  }
}
