package tech.pegasys.ethsigner.signer.multifilebased;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.KEY_PASSWORD_1;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.KEY_PASSWORD_2;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.loadKeyPasswordFile;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KeyPasswordFileTest {

  @Test
  void matchingKeyAndPasswordWithoutPrefixShouldHaveExpectedName() {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(KEY_PASSWORD_1);

    assertThat(kpFile.getName()).matches(KEY_PASSWORD_1);
  }

  @Test
  void matchingKeyAndPasswordWithPrefixShouldHaveExpectedName() {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(KEY_PASSWORD_2);

    assertThat(kpFile.getName()).matches(KEY_PASSWORD_2);
  }

  @Test
  void nonMatchingKeyAndPasswordThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("foo.key");
    final Path passwordFile = Path.of("bar.password");

    final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Key and Password names must match");
  }

  @Test
  void keyWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("invalid_extensionn.txt");
    final Path passwordFile = Path.of("valid_extension.password");

    final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Invalid key/password filename extension");
  }

  @Test
  void passwordWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("valid_extensionn.key");
    final Path passwordFile = Path.of("invalid_extension.txt");

    final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Invalid key/password filename extension");
  }

}