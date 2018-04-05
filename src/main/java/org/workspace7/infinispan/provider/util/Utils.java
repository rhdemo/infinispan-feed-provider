package org.workspace7.infinispan.provider.util;

import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Base64;

@Component
public class Utils {

  public static String shortTriggerID(String triggerName) {

    if (triggerName != null && triggerName.contains("/")) {
      String[] triggerNameArray = triggerName.split("/");
      return triggerNameArray[triggerNameArray.length - 1];
    }

    return triggerName;
  }

  public static String base64Encoded(String text) {
    return Base64.getEncoder().encodeToString(text.getBytes(Charset.forName("US-ASCII")));
  }

  public static String base64Decode(String text) {
    byte[] decodedText = Base64.getDecoder().decode(text);
    return new String(decodedText);
  }
}
