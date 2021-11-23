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
import se.eris.util.ReflectionUtil;
import se.eris.util.TestClass;
import se.eris.util.TestCompiler;
import se.eris.util.TestSupportedJavaVersions;
import se.eris.util.version.VersionCompiler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImplicitClassAnnotationInstrumenterTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.implicit.TestImplicitClassAnnotation");

    @BeforeAll
    static void beforeClass() {
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void notNullAnnotatedParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method implicitReturn = c.getMethod("implicitReturn", String.class);
        ReflectionUtil.simulateMethodCall(implicitReturn, "should work");

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ReflectionUtil.simulateMethodCall(implicitReturn, new Object[]{null}));
        assertEquals(exception.getMessage(), String.format("NotNull method %s.implicitReturn must not return null", testClass.getAsmName()));
    }

    @TestSupportedJavaVersions
    void implicitParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method implicitParameterMethod = c.getMethod("implicitParameter", String.class);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(implicitParameterMethod, new Object[]{null}));
        final String expected = String.format("Implicit NotNull argument 0%s of %s.implicitParameter must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "s"), testClass.getAsmName());
        assertEquals(expected, exception.getMessage());
    }

    @TestSupportedJavaVersions
    void voidReturn_shouldNotBeInstrumented(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method voidReferenceReturnMethod = c.getMethod("voidReferenceReturn");
        ReflectionUtil.simulateMethodCall(voidReferenceReturnMethod);
    }

    @TestSupportedJavaVersions
    void implicitConstructorParameter_shouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> implicitParameterConstructor = c.getConstructor(String.class);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateConstructorCall(implicitParameterConstructor, new Object[]{null}));
        assertEquals(String.format("Implicit NotNull argument 0%s of %s.<init> must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "s"), testClass.getAsmName()), exception.getMessage());
    }

    @TestSupportedJavaVersions
    void anonymousClassConstructor_shouldNotBeInstrumented(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass.getName());
        final Method anonymousClassNullable = c.getMethod("anonymousClassNullable");
        ReflectionUtil.simulateMethodCall(anonymousClassNullable);

        final Method anonymousClassNotNull = c.getMethod("anonymousClassNotNull");
        ReflectionUtil.simulateMethodCall(anonymousClassNotNull);
    }
}
