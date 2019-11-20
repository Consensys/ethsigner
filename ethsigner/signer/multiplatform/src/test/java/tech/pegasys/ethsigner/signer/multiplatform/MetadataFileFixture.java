package tech.pegasys.ethsigner.signer.multiplatform;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

public class MetadataFileFixture {

  public static final String CONFIG_FILE_EXTENSION = ".config";
  public static String NO_PREFIX_LOWERCASE_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";

  private static final Path metadataTomlConfigsDirectory = Path.of("src/test/resources/metadata-toml-configs");


  static MetadataFile load(final String metadataFilename) {
    final Path metadataPath = metadataTomlConfigsDirectory.resolve(metadataFilename + CONFIG_FILE_EXTENSION);
    if (!metadataPath.toFile().exists()) {
      fail("Missing metadata TOML file " + metadataPath.getFileName().toString());
      return null;
    }

    return new MetadataFile(metadataPath);
  }
}
