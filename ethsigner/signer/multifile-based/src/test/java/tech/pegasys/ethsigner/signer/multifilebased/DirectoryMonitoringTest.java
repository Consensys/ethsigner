/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.ethsigner.signer.multifilebased;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class DirectoryMonitoringTest {

  private static final int TIMEOUT_IN_MILLIS = 15000;

  @Test
  void onFileCreatedTriggersForANewFile(@TempDir Path directory) throws IOException {
    final DirectoryWatcher watcher = new DirectoryWatcher(directory);
    final Path file = directory.resolve("foo");

    final Consumer<Path> callback = mock(Consumer.class);
    final ArgumentCaptor<Path> callbackArgCaptor = ArgumentCaptor.forClass(Path.class);

    watcher.onFileCreatedEvent(callback);
    watcher.run();

    assertThat(file.toFile().createNewFile()).isTrue();

    verify(callback, timeout(TIMEOUT_IN_MILLIS).only()).accept(callbackArgCaptor.capture());
    assertThat(callbackArgCaptor.getValue()).isEqualTo(file);
  }

  @Test
  void onFileDeletedTriggersForADeletedFile(@TempDir Path directory) throws IOException {
    final DirectoryWatcher watcher = new DirectoryWatcher(directory);
    final Path file = directory.resolve("foo");
    assertThat(file.toFile().createNewFile()).isTrue();

    final Consumer<Path> callback = mock(Consumer.class);
    final ArgumentCaptor<Path> callbackArgCaptor = ArgumentCaptor.forClass(Path.class);

    watcher.onFileDeletedEvent(callback);
    watcher.run();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      //do nothing
    }

    assertThat(file.toFile().delete()).isTrue();
    verify(callback, timeout(TIMEOUT_IN_MILLIS).only()).accept(callbackArgCaptor.capture());
    assertThat(callbackArgCaptor.getValue()).isEqualTo(file);
  }

}
