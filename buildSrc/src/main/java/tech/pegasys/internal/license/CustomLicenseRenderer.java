/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.internal.license;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.github.jk1.license.License;
import com.github.jk1.license.LicenseReportExtension;
import com.github.jk1.license.ManifestData;
import com.github.jk1.license.ModuleData;
import com.github.jk1.license.PomData;
import com.github.jk1.license.ProjectData;
import com.github.jk1.license.render.ReportRenderer;
import com.github.jk1.license.util.Files;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

public class CustomLicenseRenderer implements ReportRenderer {
  private String fileName;
  private String name;
    private Project project;
    private LicenseReportExtension config;
    private File output;
    private int counter;
  private final Map<String, Map<String, String>> unknownOverridesLicenses;
  private final Map<String, String> multiLicenses;

  public CustomLicenseRenderer(
      final String fileName,
      final String name,
      final File unknownOverridesFile,
      final File multiLicensesFile) {
    this.fileName = fileName;
    this.name = name;
    if (unknownOverridesFile != null && unknownOverridesFile.exists()) {
      unknownOverridesLicenses = parseUnknownOverrides(unknownOverridesFile);
    } else {
      unknownOverridesLicenses = Collections.emptyMap();
    }

    if (multiLicensesFile != null && multiLicensesFile.exists()) {
      multiLicenses = parseMultiLicensesFile(multiLicensesFile);
    } else {
      System.err.println("Multi license file does not exist: " + multiLicensesFile);
      multiLicenses = Collections.emptyMap();
    }
  }

  @Input
  public String getFileNameCache() {
      return this.fileName;
  }

  @Override
  public void render(final ProjectData data) {
//    licenseGroup.forEach(
//        (license, moduleDataList) -> {
//          data.getProject().getLogger().warn("License: {}", license);
//          moduleDataList.forEach(
//              module -> {
//                data.getProject().getLogger().warn("  Module: {}", getModuleKey(module));
//              });
//          data.getProject().getLogger().warn("------");
//        });

      project = data.getProject();
      name = name == null ? project.getName() : name;
      fileName = fileName == null ? "index.html" : fileName;
      config = project.getExtensions().findByType(LicenseReportExtension.class);
      output = new File(config.outputDir, fileName);

      final Map<ModuleData, Set<String>> moduleLicenses = buildModuleLicenses(data);
      final Map<String, List<ModuleData>> licenseGroup = buildLicenseGroup(moduleLicenses);

      writeReport(licenseGroup, moduleLicenses);
  }

  private Map<ModuleData, Set<String>> buildModuleLicenses(final ProjectData data) {
    Map<ModuleData, Set<String>> moduleLicences = new TreeMap<>();
    // go through all dependencies
    data.getAllDependencies()
        .forEach(
            moduleData -> {
              // check manifest and/or pom
              for (ManifestData manifestData : moduleData.getManifests()) {
                if (manifestData.getLicense() != null
                    && Files.maybeLicenseUrl(manifestData.getLicenseUrl())) {
                  final String licenseName = getLicenseName(manifestData.getLicense(), moduleData);
                  moduleLicences.computeIfAbsent(moduleData, k -> new HashSet<>()).add(licenseName);
                }
              }

              for (PomData pomData : moduleData.getPoms()) {
                // pom can contain multiple licenses
                for (License license : pomData.getLicenses()) {
                  final String licenseName = getLicenseName(license.getName(), moduleData);
                  moduleLicences.computeIfAbsent(moduleData, k -> new HashSet<>()).add(licenseName);
                }
              }

              if (!moduleLicences.containsKey(moduleData)) {
                final String licenseName =
                    getLicenseName(
                        moduleData.getLicenseFiles().isEmpty() ? "Unknown" : "Embedded",
                        moduleData);
                  moduleLicences.computeIfAbsent(moduleData, k -> new HashSet<>()).add(licenseName);
              }
            });
    // select which license to use - if supplied
    moduleLicences.forEach(
        (module, licenses) -> {
          if (licenses.size() > 1) {
            final String licenseToUse = multiLicenses.get(module.getGroup());
            if (licenseToUse != null) {
              licenses.removeIf(license -> !Objects.equals(license, licenseToUse));
            }
          }

          if (licenses.size() > 1) {
            data.getProject()
                .getLogger()
                .warn("Module {} has multiple licenses: {}", getModuleKey(module), licenses);
          }
        });
    return moduleLicences;
  }

  protected Map<String, List<ModuleData>> buildLicenseGroup(
      final Map<ModuleData, Set<String>> moduleLicenses) {
    Map<String, List<ModuleData>> licenseGroup = new TreeMap<>();
    moduleLicenses.forEach(
        (module, licenses) ->
            licenses.forEach(
                license ->
                    licenseGroup.computeIfAbsent(license, k -> new ArrayList<>()).add(module)));
    return licenseGroup;
  }

  private String getLicenseName(final String licenseName, final ModuleData moduleData) {
    final String moduleKey = getModuleKey(moduleData);
    if ("Unknown".equalsIgnoreCase(licenseName)
        && unknownOverridesLicenses.containsKey(moduleKey)) {
      return unknownOverridesLicenses.get(moduleKey).getOrDefault("license", "Not Available");
    }
    return licenseName;
  }

  private String getModuleKey(final ModuleData moduleData) {
    return String.format(
        "%s:%s:%s", moduleData.getGroup(), moduleData.getName(), moduleData.getVersion());
  }

  private Map<String, Map<String, String>> parseUnknownOverrides(
      final File unknownOverridesFileName) {
    final Map<String, Map<String, String>> unknownOverridesLicenses = new TreeMap<>();
    try {
      final List<String> lines =
          java.nio.file.Files.readAllLines(
              unknownOverridesFileName.toPath(), StandardCharsets.UTF_8);
      lines.forEach(
          line -> {
            String[] columns = line.split("\\|");
            if (columns.length > 0) {
              final String groupName = columns[0];
              final Map<String, String> overrideMap = new HashMap<>();
              overrideMap.put("projectUrl", columns.length > 1 ? columns[1] : null);
              overrideMap.put("license", columns.length > 2 ? columns[2] : null);
              overrideMap.put("licenseUrl", columns.length > 3 ? columns[3] : null);

              unknownOverridesLicenses.put(groupName, overrideMap);
            }
          });
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return unknownOverridesLicenses;
  }

  private Map<String, String> parseMultiLicensesFile(final File file) {
    final Map<String, String> multipleLicenseMap = new TreeMap<>();
    try {
      final List<String> lines =
          java.nio.file.Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
      lines.forEach(
          line -> {
            String[] columns = line.split("\\|");
            if (columns.length > 0) {
              final String groupName = columns[0];
              final String licenseName = columns.length > 1 ? columns[1] : null;
              multipleLicenseMap.put(groupName, licenseName);
            }
          });
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return multipleLicenseMap;
  }

  private void writeReport(final Map<String, List<ModuleData>> licenseGroup, Map<ModuleData, Set<String>> moduleLicenses) {
      final StringBuilder html = new StringBuilder();
      html.append("<html>\n" +
              "<head>\n" +
              "    <title>\n" +
              "        Dependency License Report for " + name + "\n" +
              "    </title>\n" +
              "<head>\n" +
              "<body>\n" +
              "    <h1>\n" +
              "        Dependency License Report for " + name + "\n" +
              "    </h1>");

      html.append("<table style=\"border-collapse: collapse; width: 100%;\" border=\"0\">\n" +
              "<tbody>\n");

      licenseGroup.forEach((license, moduleList) -> {
          html.append("<tr>\n" +
                  "<th style=\"width: 98.0679%; background-color: #ced4d9;\">" + license + "</th>\n" +
                  "</tr>\n");
          // TODO: Generate detailed information
          moduleList.forEach(module -> {
              html.append("<tr>\n" +
                      "<td style=\"width: 98.0679%;\">" + getModuleKey(module) + "</td>\n" +
                      "</tr>\n");
          });
      });

      html.append("</tbody>\n" +
              "</table>\n");

      html.append("<hr />\n" +
              "        <p id=\"timestamp\">\n" +
              "            This report was generated at\n" +
              "            <em>" + Instant.now() + "</em>\n" +
              "        </p>\n" +
              "</body>\n" +
              "</html>");


      try {
          java.nio.file.Files.writeString(output.toPath(), html, StandardCharsets.UTF_8);
      } catch (IOException e) {
          throw new UncheckedIOException(e);
      }
  }
}
