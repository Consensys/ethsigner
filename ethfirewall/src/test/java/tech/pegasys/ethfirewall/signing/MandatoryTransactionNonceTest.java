package tech.pegasys.ethfirewall.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

public class MandatoryTransactionNonceTest {

  // Instance being tested
  private MandatoryTransactionNonce transactionNonce;

  @Before
  public void setUp() {
    transactionNonce = new MandatoryTransactionNonce();
  }

  @Test
  public void missingTransaction() {
    final JsonObject transaction = null;

    final Throwable thrown = catchThrowable(() -> transactionNonce.get(transaction));

    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing params from transaction");
  }


  @Test
  public void missingParametersEntry() {
    final JsonObject transaction = new JsonObject();

    final Throwable thrown = catchThrowable(() -> transactionNonce.get(transaction));

    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing params from transaction");
  }

  @Test
  public void emptyParametersEntry() {
    final JsonObject transaction = new JsonObject();
    transaction.put("params", (JsonArray) null);

    final Throwable thrown = catchThrowable(() -> transactionNonce.get(transaction));

    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing params from transaction");
  }

  @Test
  public void a(){
    final JsonObject transaction = new JsonObject();
    final JsonArray params = new JsonArray();
    final JsonObject nonce = new JsonObject();

    params.add(nonce);
    transaction.put("params", params);


  }
}
