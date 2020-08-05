package tech.pegasys.ethsigner;

import picocli.CommandLine;

import java.io.File;
import java.util.List;

import static tech.pegasys.ethsigner.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;

// Allows to obtain config file by PicoCLI using two pass approach.
@CommandLine.Command(mixinStandardHelpOptions = true)
class ConfigFileCommand {
    @CommandLine.Option(names = CONFIG_FILE_OPTION_NAME, description = "...")
    File configPath = null;

    @SuppressWarnings("UnusedVariable")
    @CommandLine.Unmatched
    List<String> unmatched;
}
