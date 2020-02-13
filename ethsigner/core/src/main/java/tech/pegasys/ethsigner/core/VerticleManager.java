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
package tech.pegasys.ethsigner.core;

import tech.pegasys.ethsigner.core.http.HttpServerService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import io.vertx.core.AsyncResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerticleManager {

  private static final Logger LOG = LogManager.getLogger();
  private final HttpServerService httpServerService;
  private final Context context;

  public VerticleManager(final Context context, final HttpServerService httpServerService) {
    this.context = context;
    this.httpServerService = httpServerService;
  }

  public void start() {
    context.getVertx().deployVerticle(httpServerService, this::httpServerServiceDeployment);
  }

  public void stop() {
    context.getVertx().close();
  }

  private void httpServerServiceDeployment(final AsyncResult<String> result) {
    if (result.succeeded()) {
      LOG.info("JsonRpcHttpService Vertx deployment id is: {}", result.result());

      if (context.getDataPath() != null) {
        final File portsFile = new File(context.getDataPath().toString(), "ethsigner.ports");
        portsFile.deleteOnExit();
        writePortsToFile(portsFile, httpServerService);
      }
    } else {
      deploymentFailed(result.cause());
    }
  }

  private void deploymentFailed(final Throwable cause) {
    LOG.error("Vertx deployment failed", cause);
    context.getVertx().close();
    System.exit(1);
  }

  private void writePortsToFile(final File portsFile, final HttpServerService httpService) {
    final Properties properties = new Properties();
    properties.setProperty("http-jsonrpc", String.valueOf(httpService.actualPort()));

    LOG.info(
        "Writing ethsigner.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Web3Provider. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }
}
