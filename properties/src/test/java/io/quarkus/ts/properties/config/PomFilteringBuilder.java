package io.quarkus.ts.properties.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder;

public class PomFilteringBuilder extends ProdQuarkusApplicationManagedResourceBuilder {
    static final String GENERATED_POM = "quarkus-app-pom.xml";
    static final String TEMPORARY_POM = "quarkus-app-pom.xml.tmp";

    private static final Logger LOG = Logger.getLogger(PomFilteringBuilder.class);
    private static final String FOR_REMOVAL = "<extensions>true</extensions>";

    @Override
    public void build() {
        Path target = this.getTargetFolderForLocalArtifacts();
        Path pom = target.resolve(GENERATED_POM).toAbsolutePath();
        Path temporaryPom = pom.resolveSibling(TEMPORARY_POM);
        LOG.info("Removing " + FOR_REMOVAL + " from " + pom + " using " + temporaryPom);
        try {
            try (Stream<String> lines = Files.lines(pom);
                    BufferedWriter writer = Files.newBufferedWriter(temporaryPom, StandardOpenOption.CREATE_NEW)) {
                lines
                        .filter(line -> !line.contains(FOR_REMOVAL))
                        .forEach(line -> {
                            try {
                                writer.write(line);
                                writer.newLine();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            Files.copy(temporaryPom, pom, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Failed to remove " + FOR_REMOVAL + " from " + pom, e);
        }
        super.build();
    }
}
