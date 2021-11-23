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
package se.eris.types;

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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterTypesTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.types.TestTypes");

    @BeforeAll
    static void beforeClass() {
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void objectArgument(final String javaVersion) throws Exception {
        final Class<?> clazz = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> constructor = clazz.getConstructor();

        final Object instance = ReflectionUtil.simulateConstructorCall(constructor);

        final Method paramObject = instance.getClass().getDeclaredMethod("paramObject", String.class);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(instance, paramObject, (Object) null));
        assertEquals(String.format("NotNull annotated argument 0%s of %s.paramObject must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "string"), testClass.getAsmName()), exception.getMessage());
    }

    @TestSupportedJavaVersions
    void arrayArgument(final String javaVersion) throws Exception {
        final Class<?> clazz = compilers.get(javaVersion).getCompiledClass(testClass);
        final Constructor<?> constructor = clazz.getConstructor();

        final Object instance = ReflectionUtil.simulateConstructorCall(constructor);

        final Method paramObject = instance.getClass().getDeclaredMethod("paramArray", int[].class);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(instance, paramObject, (Object) null));
        assertEquals(String.format("NotNull annotated argument 0%s of %s.paramArray must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "array"), testClass.getAsmName()), exception.getMessage());
    }

    public String append(String a, String b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        String result = a + b;
        Objects.requireNonNull(result);
        return result;
    }


}