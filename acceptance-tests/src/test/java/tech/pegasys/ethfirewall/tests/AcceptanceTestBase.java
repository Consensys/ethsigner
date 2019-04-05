package tech.pegasys.ethfirewall.tests;

import static org.junit.Assert.fail;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.junit.After;
import org.junit.Before;

public class AcceptanceTestBase {

  private DockerClient dockerClient;
  private String pantheonId;
  private EthFirewallProcessRunner ethFirewallRunner;

  @Before
  public void setUpBase() {
    final DefaultDockerClientConfig config = DefaultDockerClientConfig
        .createDefaultConfigBuilder().build();

    final DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
        .withReadTimeout(1000)
        .withConnectTimeout(1000)
        .withMaxTotalConnections(100)
        .withMaxPerRouteConnections(10);

    dockerClient = DockerClientBuilder.getInstance(config)
        .withDockerCmdExecFactory(dockerCmdExecFactory)
        .build();

    try {
      final CreateContainerResponse pantheon = dockerClient
          .createContainerCmd("pegasyseng/pantheon:latest").exec();
      pantheonId = pantheon.getId();
      dockerClient.startContainerCmd(pantheonId).exec();
    } catch (final NotFoundException e) {
      fail("Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'");
    }

    ethFirewallRunner = new EthFirewallProcessRunner();
    ethFirewallRunner.start("EthFirewall");
  }

  @After
  public void tearDownBase() {
    if (dockerClient != null && pantheonId != null) {
      dockerClient.stopContainerCmd(pantheonId).exec();
      dockerClient.waitContainerCmd(pantheonId)
          .exec((new WaitContainerResultCallback()));
      dockerClient.removeContainerCmd(pantheonId).exec();
    }

    ethFirewallRunner.shutdown();
  }

  //TODO ports! need the EthFirewall ports for request / response - or provide functions!
}
