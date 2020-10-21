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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationTest {

    private static final String ORG_JETBRAINS_ANNOTATIONS_NOT_NULL = org.jetbrains.annotations.NotNull.class.getName();
    private static final String ORG_JETBRAINS_ANNOTATIONS_NULLABLE = org.jetbrains.annotations.Nullable.class.getName();

    private static final String SE_ERIS_NOT_NULL = se.eris.notnull.NotNull.class.getName();
    private static final String SE_ERIS_NULLABLE = se.eris.notnull.Nullable.class.getName();

    private static final Set<String> EXPECTED_NOTNULL = new HashSet<String>() {{
        add(ORG_JETBRAINS_ANNOTATIONS_NOT_NULL);
        add(SE_ERIS_NOT_NULL);
    }};

    private static final Set<String> EXPECTED_NULLABLE = new HashSet<String>() {{
        add(ORG_JETBRAINS_ANNOTATIONS_NULLABLE);
        add(SE_ERIS_NULLABLE);
    }};


    @Test
    void getAnnotations_defaultNotNull() {
        final Configuration configuration = getDefaultNotNullConfiguration(false);
        assertEquals(EXPECTED_NOTNULL, configuration.getNotNullAnnotations());
        assertFalse(configuration.isImplicit());
    }

    @Test
    void getAnnotations_defaultNullable() {
        final Configuration configuration = getDefaultNotNullConfiguration(true);
        assertEquals(EXPECTED_NULLABLE, configuration.getNullableAnnotations());
        assertTrue(configuration.isImplicit());
    }

    @Test
    void excludes() {
        final Configuration configuration = getDefaultNotNullConfiguration(true);
        configuration.isImplicitInstrumentation("NoName.java");
    }

    private Configuration getDefaultNotNullConfiguration(final boolean implicit) {
        return new Configuration(implicit, false, new AnnotationConfiguration(Collections.emptySet(), Collections.emptySet()), new ExcludeConfiguration(Collections.emptySet()));
    }

}