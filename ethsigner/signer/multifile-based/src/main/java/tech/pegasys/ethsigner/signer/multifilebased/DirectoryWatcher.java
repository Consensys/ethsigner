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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("rawtypes")
class DirectoryWatcher {

  private static final Logger LOG = LogManager.getLogger();

  private final Path directory;
  private final ExecutorService executorService = buildExecutorService();
  private final WatchService watchService;

  private Optional<Consumer<Path>> fileCreatedEventCallback = Optional.empty();
  private Optional<Consumer<Path>> fileDeletedEventCallback = Optional.empty();

  DirectoryWatcher(final Path directory) {
    this.directory = directory;

    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      directory.register(
          watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static ExecutorService buildExecutorService() {
    return Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("directory-monitoring").build());
  }

  void onFileCreatedEvent(final Consumer<Path> callback) {
    this.fileCreatedEventCallback = Optional.ofNullable(callback);
  }

  void onFileDeletedEvent(final Consumer<Path> callback) {
    this.fileDeletedEventCallback = Optional.ofNullable(callback);
  }

  void run() {
    LOG.info("Starting directory watcher ({})", directory);
    executorService.execute(watchForFileEvents());
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private Runnable watchForFileEvents() {
    return () -> {
      try {
        WatchKey key;
        while ((key = watchService.take()) != null) {
          for (WatchEvent<?> event : key.pollEvents()) {
            LOG.trace("Event kind: {}, File affected: {}", event.kind(), event.context());
            processWatchEvent(event);
          }
          key.reset();
        }
      } catch (InterruptedException e) {
        LOG.error("Directory watcher thread has been interrupted", e);
        throw new RuntimeException(e);
      }
    };
  }

  private void processWatchEvent(final WatchEvent<?> event) {
    if (event.context() instanceof Path) {
      final Path file = (Path) event.context();
      switch (event.kind().name()) {
        case "ENTRY_CREATE":
          {
            tryCallbackExecution(fileCreatedEventCallback, file);
            break;
          }
        case "ENTRY_DELETE":
          {
            tryCallbackExecution(fileDeletedEventCallback, file);
            break;
          }
      }
    }
  }

  private void tryCallbackExecution(final Optional<Consumer<Path>> callback, final Path file) {
    try {
      callback.ifPresent(l -> l.accept(directory.resolve(file)));
    } catch (Exception e) {
      LOG.warn("Error executing callback", e);
      // do nothing if we miss an event. We don't any errors to stop our watcher thread.
    }
  }

  void shutdown() {
    LOG.info("Shutting down directory watcher");
    executorService.shutdownNow();
  }
}
