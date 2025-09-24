package se.eris.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestCompilerOptions {

    private final Path destination;
    private final String source;
    private final String target;

    public static TestCompilerOptions from(final Path destination, final String sourceAndTargetVersion) {
        return new TestCompilerOptions(destination, sourceAndTargetVersion, sourceAndTargetVersion);
    }

    private TestCompilerOptions(final Path destination, final String source, final String target) {
        this.destination = destination;
        this.source = validateJavaVersion(source, "source");
        this.target = validateJavaVersion(target, "target");
    }

    public Path getDestination() {
        return destination;
    }

    public URL[] getClasspathURLs() {
        try {
            return new URL[]{destination.toUri().toURL()};
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getCompilerOptions() {
        return new ArrayList<String>() {{
            add("-source");
            add(source);
            add("-target");
            add(target);
            add("-d");
            add(destination.toString());
        }};

    }

    public boolean targetHasParametersSupport() {
        return !target.matches("^1\\.[2-7]$");
    }

    private String validateJavaVersion(final String javaVersion, final String versionName) {
        if (!javaVersion.matches("^(1\\.[2-9]|1[0-9]|2[0-1])$")) {
            throw new IllegalArgumentException("Unknown " + versionName + " version " + javaVersion);
        }
        return javaVersion;
    }

}
