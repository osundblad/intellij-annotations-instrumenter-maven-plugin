package se.eris.util.version;

import com.intellij.NotNullInstrumenter;
import org.jetbrains.annotations.NotNull;
import se.eris.maven.NopLogWrapper;
import se.eris.notnull.AnnotationConfiguration;
import se.eris.notnull.Configuration;
import se.eris.notnull.ExcludeConfiguration;
import se.eris.util.TestCompiler;
import se.eris.util.TestCompilerOptions;
import se.eris.util.TestJava16AndLater;
import se.eris.util.TestSupportedJavaVersions;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionCompiler {

    private final String[] supportedVersions;

    public static VersionCompiler withJava16AndLater() {
        return new VersionCompiler(TestJava16AndLater.SupportedVersions.getSupportedVersions());
    }

    public static VersionCompiler withSupportedVersions() {
        return new VersionCompiler(TestSupportedJavaVersions.SupportedVersions.getSupportedVersions());
    }

    private VersionCompiler(final String[] supportedVersions) {
        this.supportedVersions = supportedVersions;
    }

    public Map<String, TestCompiler> compile(final Path destinationBasedir, final File... javaFiles) {
        final Configuration configuration = new Configuration(false,
                new AnnotationConfiguration(notnull(), nullable()),
                new ExcludeConfiguration(Collections.emptySet()));
        return compile(destinationBasedir, configuration, javaFiles);
    }

    @NotNull
    public Map<String, TestCompiler> compile(final Path destinationBasedir, final Configuration configuration, final File... javaFiles) {
        final Map<String, TestCompiler> compilers = new HashMap<>();
        for (final String version : supportedVersions) {
            final Path destination = destinationBasedir.resolve(version);
            final TestCompiler compiler = TestCompiler.create(TestCompilerOptions.from(destination, version));
            if (!compiler.compile(javaFiles)) {
                throw new RuntimeException("Compilation failed for version " + version);
            }
            compilers.put(version, compiler);

            final NotNullInstrumenter instrumenter = new NotNullInstrumenter(new NopLogWrapper());
            final int numberOfInstrumentedFiles = instrumenter.instrument(destination, configuration, Collections.emptyList());

            assertTrue(numberOfInstrumentedFiles > 0, "No files instrumented, check that test file(s) are \"marked\" for instrumentation");
        }
        return compilers;
    }

    @NotNull
    private static Set<String> nullable() {
        final Set<String> annotations = new HashSet<>();
        annotations.add("org.jetbrains.annotations.Nullable");
        return annotations;
    }

    @NotNull
    private static Set<String> notnull() {
        final Set<String> annotations = new HashSet<>();
        annotations.add("org.jetbrains.annotations.NotNull");
        return annotations;
    }

    /**
     * @return single-quoted parameter name if compilers supports `-parameters` option, empty string otherwise.
     */
    @NotNull
    public static String maybeName(final TestCompiler testCompiler, @NotNull final String parameterName) {
        return testCompiler.hasParametersSupport() ? String.format(" (parameter '%s')", parameterName) : "";
    }

}
