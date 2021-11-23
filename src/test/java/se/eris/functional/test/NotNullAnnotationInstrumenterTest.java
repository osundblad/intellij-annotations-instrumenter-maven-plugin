/*
 * Copyright 2013-2015 Eris IT AB
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
package se.eris.functional.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import se.eris.notnull.AnnotationConfiguration;
import se.eris.notnull.Configuration;
import se.eris.notnull.ExcludeConfiguration;
import se.eris.util.ReflectionUtil;
import se.eris.util.TestClass;
import se.eris.util.TestCompiler;
import se.eris.util.TestSupportedJavaVersions;
import se.eris.util.version.VersionCompiler;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotNullAnnotationInstrumenterTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.notnull.TestNotNull");

    @BeforeAll
    static void beforeClass() {
        final Configuration configuration = new Configuration(false, new AnnotationConfiguration(notNull(), Collections.emptySet()), new ExcludeConfiguration(Collections.emptySet()));
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, configuration, testClass.getJavaFile(SRC_DIR)));
    }

    @NotNull
    private static Set<String> notNull() {
        final Set<String> annotations = new HashSet<>();
        annotations.add("org.jetbrains.annotations.NotNull");
        annotations.add("java.lang.Deprecated"); // a random Annotation for testing (not realistic use)
        return annotations;
    }

    @TestSupportedJavaVersions
    void annotatedParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullParameterMethod = c.getMethod("notNullParameter", String.class);
        ReflectionUtil.simulateMethodCall(notNullParameterMethod, "should work");

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(notNullParameterMethod, new Object[]{null}));
        assertEquals("NotNull annotated argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") +
                " of " + testClass.getAsmName() + ".notNullParameter must not be null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void notnullReturn_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullReturnMethod = c.getMethod("notNullReturn", String.class);
        ReflectionUtil.simulateMethodCall(notNullReturnMethod, "should work");

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ReflectionUtil.simulateMethodCall(notNullReturnMethod, new Object[]{null}));
        assertEquals("NotNull method " + testClass.getAsmName() + ".notNullReturn must not return null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void annotatedReturn_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullReturnMethod = c.getMethod("annotatedReturn", String.class);
        ReflectionUtil.simulateMethodCall(notNullReturnMethod, "should work");

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ReflectionUtil.simulateMethodCall(notNullReturnMethod, new Object[]{null}));
        assertEquals("NotNull method " + testClass.getAsmName() + ".annotatedReturn must not return null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void overridingMethod_isInstrumented(final String javaVersion) throws Exception {
        final Class<?> subargClass = compilers.get(javaVersion).getCompiledClass(testClass.getName() + "$Subarg");
        final Class<?> subClass = compilers.get(javaVersion).getCompiledClass(testClass.getName() + "$Sub");
        final Method specializedMethod = subClass.getMethod("overload", subargClass);
        assertFalse(specializedMethod.isSynthetic());
        assertFalse(specializedMethod.isBridge());
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(subClass.newInstance(), specializedMethod, new Object[]{null}));
        assertEquals("NotNull annotated argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") +
                " of " + testClass.getAsmName() + "$Sub.overload must not be null", exception.getMessage());
    }

}