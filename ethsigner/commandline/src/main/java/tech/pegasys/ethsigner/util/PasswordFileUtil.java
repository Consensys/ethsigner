/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.io.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PasswordFileUtil {
  private static final Logger LOG = LogManager.getLogger();

  /**
   * Read password from the first line of specified file
   *
   * @param path The file to read the password from
   * @return Password
   * @throws IOException For file operations
   * @throws TransactionSignerInitializationException If password file is empty
   */
  public static String readPasswordFromFile(final Path path) throws IOException {
    final String password = Files.asCharSource(path.toFile(), UTF_8).readFirstLine();
    if (password == null || password.isEmpty()) {
      LOG.error("Cannot read password from empty file: " + path);
      throw new TransactionSignerInitializationException(
          "Cannot read password from empty file: " + path);
    }
    return password;
  }
}
