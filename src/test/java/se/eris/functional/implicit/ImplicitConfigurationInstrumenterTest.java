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
package se.eris.functional.implicit;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ImplicitConfigurationInstrumenterTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.implicit.TestImplicitConfiguration");

    @BeforeAll
    static void beforeClass() {
        final Configuration configuration = new Configuration(true, new AnnotationConfiguration(), new ExcludeConfiguration(Collections.emptySet()));
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, configuration, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void notNullAnnotatedParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullParameterMethod = c.getMethod("notNullParameter", String.class);
        ReflectionUtil.simulateMethodCall(notNullParameterMethod, "should work");

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(notNullParameterMethod, new Object[]{null}));
        final String expected = String.format("Implicit NotNull argument 0%s of %s.notNullParameter must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "s"), testClass.getAsmName());
        assertEquals(expected, exception.getMessage());
    }

    @TestSupportedJavaVersions
    void implicitParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method implicitParameterMethod = c.getMethod("implicitParameter", String.class);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(implicitParameterMethod, new Object[]{null}));
        assertEquals("Implicit NotNull argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") + " of " + testClass.getAsmName() + ".implicitParameter must not be null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void nullableAnnotatedParameter_shouldNotValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method implicitParameterMethod = c.getMethod("nullableParameter", String.class);
        ReflectionUtil.simulateMethodCall(implicitParameterMethod, new Object[]{null});
    }

    @TestSupportedJavaVersions
    void implicitReturn_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullReturnMethod = c.getMethod("implicitReturn", String.class);
        ReflectionUtil.simulateMethodCall(notNullReturnMethod, "should work");

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ReflectionUtil.simulateMethodCall(notNullReturnMethod, new Object[]{null}));
        assertEquals("NotNull method " + testClass.getAsmName() + ".implicitReturn must not return null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void annotatedReturn_shouldNotValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method notNullReturnMethod = c.getMethod("annotatedReturn", String.class);
        ReflectionUtil.simulateMethodCall(notNullReturnMethod, (String) null);
    }

    @TestSupportedJavaVersions
    void nestedClassWithoutConstructor_shouldWork(final String javaVersion) throws Exception {
        boolean noArgConstructorFound = false;
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName() + "$Nested");
        for (final Constructor<?> constructor : c.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) {
                noArgConstructorFound = true;
                constructor.setAccessible(true);
                constructor.newInstance();
            } else if (constructor.isSynthetic()) {
                constructor.setAccessible(true);
                constructor.newInstance(constructor.getGenericParameterTypes()[0].getClass().cast(null));
            } else {
                throw new RuntimeException("There should be no constructors with these properties (only no-arg snd synthetic)");
            }
        }
        assertTrue(noArgConstructorFound);
    }

}