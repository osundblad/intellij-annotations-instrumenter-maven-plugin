/*
 * Copyright 2013-2016 Eris IT AB
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
package se.eris.notnull;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Configuration {

    private final boolean implicit;
    private final boolean warnInsteadOfThrow;
    @NotNull
    private final AnnotationConfiguration annotationConfiguration;
    @NotNull
    private final ExcludeConfiguration excludeConfiguration;

    @SuppressWarnings("BooleanParameter")
    public Configuration(
            final boolean implicit,
            final boolean warnInsteadOfThrow,
            @NotNull final AnnotationConfiguration annotationConfiguration,
            @NotNull final ExcludeConfiguration excludeConfiguration) {
        this.implicit = implicit;
        this.warnInsteadOfThrow = warnInsteadOfThrow;
        if (annotationConfiguration.isAnnotationsConfigured()) {
            this.annotationConfiguration = annotationConfiguration;
        } else {
            this.annotationConfiguration = new AnnotationConfiguration(getDefaultNotNull(), getDefaultNullable());
        }
        this.excludeConfiguration = excludeConfiguration;
    }

    @NotNull
    private Set<String> getDefaultNotNull() {
        return new HashSet<>(Arrays.asList(
                org.jetbrains.annotations.NotNull.class.getName(),
                se.eris.notnull.NotNull.class.getName())
        );
    }

    @NotNull
    private Set<String> getDefaultNullable() {
        return new HashSet<>(Arrays.asList(
                org.jetbrains.annotations.Nullable.class.getName(),
                se.eris.notnull.Nullable.class.getName())
        );
    }

    public boolean isImplicit() {
        return implicit;
    }

    public boolean isWarnInsteadOfThrow() {
        return warnInsteadOfThrow;
    }

    @NotNull
    public Set<String> getNotNullAnnotations() {
        return annotationConfiguration.getNotNull();
    }

    @NotNull
    public Set<String> getNullableAnnotations() {
        return annotationConfiguration.getNullable();
    }

    public boolean isImplicitInstrumentation(final String className) {
        return implicit && excludeConfiguration.isClassImplicitInstrumentation(className);
    }
}
