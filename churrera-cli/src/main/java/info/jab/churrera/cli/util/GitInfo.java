package info.jab.churrera.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class GitInfo {

    private static final Logger logger = LoggerFactory.getLogger(GitInfo.class);

    private final Supplier<InputStream> gitPropertiesStreamSupplier;

    public GitInfo() {
        this(() -> GitInfo.class.getClassLoader().getResourceAsStream("git.properties"));
    }

    GitInfo(Supplier<InputStream> gitPropertiesStreamSupplier) {
        this.gitPropertiesStreamSupplier = gitPropertiesStreamSupplier;
    }

    public void print() {
        try (InputStream input = gitPropertiesStreamSupplier.get()) {
            //Preconditions
            if (Objects.isNull(input)) {
                logger.warn("git.properties not found");
                return;
            }

            //Load properties
            Properties prop = new Properties();
            prop.load(input);

            //Print info
            String version = prop.getProperty("git.build.version");
            String commit = prop.getProperty("git.commit.id.abbrev");
            if (version != null) {
                System.out.println("Version: " + version);
            }
            if (commit != null) {
                System.out.println("Commit: " + commit);
            }
        } catch (IOException ex) {
            logger.error("Error printing git info: {}", ex.getMessage(), ex);
        }
    }
}
