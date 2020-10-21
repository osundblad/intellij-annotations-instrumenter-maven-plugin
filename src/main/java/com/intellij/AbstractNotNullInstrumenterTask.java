/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import se.eris.maven.MavenLogWrapper;
import se.eris.notnull.AnnotationConfiguration;
import se.eris.notnull.Configuration;
import se.eris.notnull.ExcludeConfiguration;
import se.eris.notnull.instrumentation.ClassMatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Rassokhin
 * @author Olle Sundblad
 */
abstract class AbstractNotNullInstrumenterTask extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    MavenProject project;

    @Parameter
    private Set<String> notNull;

    @Parameter
    private Set<String> nullable;

    @Parameter
    private Set<String> excludes;

    @Parameter
    private boolean implicit;

    @Parameter
    private boolean warnInsteadOfThrow;

    @Parameter(property = "se.eris.notnull.instrument", defaultValue = "true")
    private boolean instrument;

    private final NotNullInstrumenter instrumenter;

    private final MavenLogWrapper logger = new MavenLogWrapper(getLog());

    public AbstractNotNullInstrumenterTask() {
        instrumenter = new NotNullInstrumenter(logger);
    }

    void instrument(final Path classesDirectory, @NotNull final Iterable<String> classpathElements) throws MojoExecutionException {
        if (!instrument) {
            return;
        }
        final Configuration configuration = getConfiguration();
        logAnnotations(configuration);
        final List<URL> classpathUrls = getClasspathUrls(classpathElements);
        final int instrumented = instrumenter.instrument(classesDirectory, configuration, classpathUrls);
        logger.info("Instrumented " + instrumented + " files with NotNull assertions");
    }

    @NotNull
    private List<URL> getClasspathUrls(@NotNull final Iterable<String> classpathElements) throws MojoExecutionException {
        final List<URL> urls = new ArrayList<>();
        try {
            for (final String cp : classpathElements) {
                urls.add(new File(cp).toURI().toURL());
            }
        }
        catch (final MalformedURLException e) {
            throw new MojoExecutionException("Cannot convert classpath element into URL", e);
        }
        catch (final RuntimeException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
        return urls;
    }

    private Configuration getConfiguration() {
        return new Configuration(implicit,
                warnInsteadOfThrow,
                getAnnotationConfiguration(nullToEmpty(notNull), nullToEmpty(nullable)),
                getExcludeConfiguration(nullToEmpty(excludes)));
    }

    private AnnotationConfiguration getAnnotationConfiguration(final Set<String> notNull, final Set<String> nullable) {
        return new AnnotationConfiguration(notNull, nullable);
    }

    private ExcludeConfiguration getExcludeConfiguration(final Set<String> excludes) {
        final Set<ClassMatcher> excludeMatchers = new HashSet<>();
        for (final String exclude : excludes) {
            excludeMatchers.add(ClassMatcher.namePattern(exclude));
        }
        return new ExcludeConfiguration(excludeMatchers);
    }

    private Set<String> nullToEmpty(final Set<String> set) {
        return (set != null) ? set : Collections.emptySet();
    }

    private void logAnnotations(@NotNull final Configuration configuration) {
        if (!configuration.getNotNullAnnotations().isEmpty()) {
            logger.info("Using the following NotNull annotations:");
            for (final String notNullAnnotation : configuration.getNotNullAnnotations()) {
                logger.info("  " + notNullAnnotation);
            }
        }
        if (!configuration.getNullableAnnotations().isEmpty()) {
            logger.info("Using the following Nullable annotations:");
            for (final String nullableAnnotation : configuration.getNullableAnnotations()) {
                logger.info("  " + nullableAnnotation);
            }
        }
    }

}
