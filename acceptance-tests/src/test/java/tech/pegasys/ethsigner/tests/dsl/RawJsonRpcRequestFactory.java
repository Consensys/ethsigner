package tech.pegasys.ethsigner.tests.dsl;

import java.util.Collections;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

public class RawJsonRpcRequestFactory {

  public static class ArbitraryResponseType extends Response<Boolean> { }

  private final Web3jService web3jService;

  public RawJsonRpcRequestFactory(final Web3jService web3jService) {
    this.web3jService = web3jService;
  }

  public Request<?, ArbitraryResponseType> createRequest(final String method) {
    return new Request<>(
        method,
        Collections.emptyList(),
        web3jService,
        ArbitraryResponseType.class);
  }
}
