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
package se.eris.functional.exclude;

import org.junit.jupiter.api.BeforeAll;
import se.eris.notnull.AnnotationConfiguration;
import se.eris.notnull.Configuration;
import se.eris.notnull.ExcludeConfiguration;
import se.eris.notnull.instrumentation.ClassMatcher;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests to verify that package exclusion works.
 */
class ExcludeImplicitClassesTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.exclude.TestExclude");

    @BeforeAll
    static void beforeClass() {
        final Configuration configuration = new Configuration(true,
                new AnnotationConfiguration(),
                new ExcludeConfiguration(Collections.singleton(ClassMatcher.namePattern("se.eris.exclude.*"))));
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, configuration, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void annotatedParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> clazz = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullParameterMethod = clazz.getMethod("notNullParameter", String.class);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(notNullParameterMethod, (Object) null));
        assertEquals("NotNull annotated argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") +
                " of " + testClass.getAsmName() + ".notNullParameter must not be null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void annotatedReturn_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullReturnMethod = c.getMethod("notNullReturn", String.class);

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ReflectionUtil.simulateMethodCall(notNullReturnMethod, new Object[]{null}));
        assertEquals("NotNull method " + testClass.getAsmName() + ".notNullReturn must not return null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void notAnnotatedParameter_shouldNotValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullParameterMethod = c.getMethod("unAnnotatedParameter", String.class);

        ReflectionUtil.simulateMethodCall(notNullParameterMethod, (Object) null);
    }

    @TestSupportedJavaVersions
    void notAnnotatedReturn_shouldNotValidate(final String javaVersion) throws Exception {
        compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullParameterMethod = c.getMethod("unAnnotatedReturn", String.class);

        ReflectionUtil.simulateMethodCall(notNullParameterMethod, (Object) null);
    }

}