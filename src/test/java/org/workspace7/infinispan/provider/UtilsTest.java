package org.workspace7.infinispan.provider;

import org.junit.Test;
import org.workspace7.infinispan.provider.util.Utils;

import java.nio.charset.Charset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;


public class UtilsTest {

  @Test
  public void testTriggerName() {
    String actual = Utils.shortTriggerID("/_/cacheEntryTrigger");
    assertThat(actual).isEqualToIgnoringCase("cacheEntryTrigger");
  }

  @Test
  public void testTriggerNameWithoutNS() {
    String actual = Utils.shortTriggerID("cacheEntryTrigger");
    assertThat(actual).isEqualToIgnoringCase("cacheEntryTrigger");
  }

  @Test
  public void testBase64Encoding() {
    String text = "user:changeme";
    String base64Text = Utils.base64Encoded(text);

    String decoded = new String(Base64.getDecoder().decode(base64Text.getBytes(Charset.forName("US-ASCII"))));
    assertThat(decoded).isEqualTo(text);
  }
}
