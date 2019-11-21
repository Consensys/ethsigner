package tech.pegasys.ethsigner.signer.multiplatform;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;

public class MetadataFileFixture {

  public static final String CONFIG_FILE_EXTENSION = ".config";
  public static String NO_PREFIX_LOWERCASE_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";
  public static String KEY_FILE = "/mydirectory/keyFile";
  public static String PASSWORD_FILE = "/mydirectory/passwordFile";

  private static final Path metadataTomlConfigsDirectory = Path.of("src/test/resources/metadata-toml-configs");


  static FileBasedSigningMetadataFile load(final String metadataFilename, final String keyFilename, final String passwordFilename) {
    final Path metadataPath = metadataTomlConfigsDirectory.resolve(metadataFilename + CONFIG_FILE_EXTENSION);
    if (!metadataPath.toFile().exists()) {
      fail("Missing metadata TOML file " + metadataPath.getFileName().toString());
      return null;
    }

    return new FileBasedSigningMetadataFile(metadataPath, new File(keyFilename).toPath(), new File(passwordFilename).toPath());
  }
}
