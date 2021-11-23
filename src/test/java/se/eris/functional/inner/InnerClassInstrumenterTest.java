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
package se.eris.functional.inner;

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

class InnerClassInstrumenterTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.inner.TestInner");

    @BeforeAll
    static void beforeClass() {
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void innerClassConstructorShouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> constructor = c.getConstructor(String.class, Integer.class, Integer.class);

        ReflectionUtil.simulateConstructorCall(constructor, "A String", 17, 18);
        ReflectionUtil.simulateConstructorCall(constructor, null, 17, 18);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateConstructorCall(constructor, new Object[]{null, null, 18}));
        assertEquals(String.format("NotNull annotated argument 1%s of %s$InnerClass.<init> must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "notNull"), testClass.getAsmName()), exception.getMessage());
    }

    @TestSupportedJavaVersions
    void innerClassMethodShouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> constructor = c.getConstructor(String.class, Integer.class, Integer.class);

        final Object outer = ReflectionUtil.simulateConstructorCall(constructor, "A String", 17, 18);

        final Method getInner = outer.getClass().getDeclaredMethod("getInner");
        final Object innerClass = ReflectionUtil.simulateMethodCall(outer, getInner);

        final Method innerClassMethod = innerClass.getClass().getDeclaredMethod("innerMethod", String.class, Integer.class);
        ReflectionUtil.simulateMethodCall(innerClass, innerClassMethod, null, 12);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(innerClass, innerClassMethod, null, null));
        assertEquals(String.format("NotNull annotated argument 1%s of %s$InnerClass.innerMethod must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "innerNotNull"), testClass.getAsmName()), exception.getMessage());
    }

    @TestSupportedJavaVersions
    void nestedClassConstructorShouldValidate(final String javaVersion) throws Exception {
        final Class<?> c = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> constructor = c.getConstructor(String.class, Integer.class, Integer.class);

        ReflectionUtil.simulateConstructorCall(constructor, "A String", 17, 18);
        ReflectionUtil.simulateConstructorCall(constructor, null, 17, 18);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateConstructorCall(constructor, new Object[]{null, 17, null}));
        assertEquals(String.format("NotNull annotated argument 1%s of %s$NestedClass.<init> must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "notNull"), testClass.getAsmName()), exception.getMessage());
    }

}