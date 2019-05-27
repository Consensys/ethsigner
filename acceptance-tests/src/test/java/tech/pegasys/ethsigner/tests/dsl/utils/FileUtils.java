package tech.pegasys.ethsigner.tests.dsl.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;

public class FileUtils {

  public static String readResource(final String resourceName) {
    final URL resource = Resources.getResource(resourceName);
    try {
      return new String(Resources.toString(resource, UTF_8).getBytes(UTF_8));
    } catch (IOException e) {
      throw new RuntimeException("Unable to load resource " + resourceName);
    }
  }

}
