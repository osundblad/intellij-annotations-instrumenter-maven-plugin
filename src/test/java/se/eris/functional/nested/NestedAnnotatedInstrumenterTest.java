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
package se.eris.functional.nested;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import se.eris.asm.AsmUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedAnnotatedInstrumenterTest {

    private static final File SRC_DIR = new File("src/test/data");
    private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

    private static final Map<String, TestCompiler> compilers = new HashMap<>();
    private static final TestClass testClass = new TestClass("se.eris.nested.TestNestedAnnotated");

    @BeforeAll
    static void beforeClass() {
        final Configuration configuration = new Configuration(false,
                new AnnotationConfiguration(),
                new ExcludeConfiguration(Collections.emptySet()));
        compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, configuration, testClass.getJavaFile(SRC_DIR)));
    }

    @TestSupportedJavaVersions
    void syntheticMethod_dispatchesToSpecializedMethod(final String javaVersion) throws Exception {
        final Class<?> superargClass = compilers.get(javaVersion).getCompiledClass(testClass.nested("Superarg").getName());
        final TestClass sub = testClass.nested("Sub");
        final Class<?> subClass = compilers.get(javaVersion).getCompiledClass(sub.getName());
        final Method generalMethod = subClass.getMethod("overload", superargClass);

        assertTrue(generalMethod.isSynthetic());
        assertTrue(generalMethod.isBridge());
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(subClass.getDeclaredConstructor().newInstance(), generalMethod, new Object[]{null}));
        assertEquals("NotNull annotated argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") + " of " + sub.getAsmName() + ".overload must not be null", exception.getMessage());
    }

    @TestSupportedJavaVersions
    void onlySpecificMethod_isInstrumented(final String javaVersion) throws Exception {
        // Check that only the specific method has a string annotation indicating instrumentation
        final TestClass sub = testClass.nested("Sub");
        final ClassReader classReader = sub.getClassReader(DESTINATION_BASEDIR.resolve(javaVersion).toFile());
        final List<String> strings = getStringConstants(classReader, "overload");
        final String onlyExpectedString = "(L" + testClass.nested("Subarg").getAsmName() + ";)V:" +
                "NotNull annotated argument 0" + VersionCompiler.maybeName(compilers.get(javaVersion), "s") + " of " +
                sub.getAsmName() + ".overload must not be null";
        assertEquals(Collections.singletonList(onlyExpectedString), strings);
    }

    @TestSupportedJavaVersions
    void nestedClassesSegmentIsPreserved(final String javaVersion) throws Exception {
        // Check that only the specific method has a string annotation indicating instrumentation
        final TestClass preserved = testClass.nested("NestedClassesSegmentIsPreserved");
        final ClassReader classReader = preserved.getClassReader(DESTINATION_BASEDIR.resolve(javaVersion).toFile());
        final List<AsmInnerClass> asmInnerClasses = getAsmInnerClasses(classReader);
        assertEquals(2, asmInnerClasses.size());
        //self-entry
        assertEquals(preserved.getAsmName(), asmInnerClasses.get(0).name);
        //inner entry
        final AsmInnerClass expected = new AsmInnerClass(preserved.nested("ASub").getAsmName(),
                preserved.getAsmName(), "ASub", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        assertEquals(expected, asmInnerClasses.get(1));
    }

    private List<AsmInnerClass> getAsmInnerClasses(final ClassReader cr) {
        final List<AsmInnerClass> asmInnerClasses = new ArrayList<>();
        cr.accept(new ClassVisitor(AsmUtils.ASM_OPCODES_VERSION) {
            @Override
            public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
                asmInnerClasses.add(new AsmInnerClass(name, outerName, innerName, access));
            }
        }, 0);
        return asmInnerClasses;
    }

    @NotNull
    private List<String> getStringConstants(final ClassReader cr, final String methodName) {
        final List<String> strings = new ArrayList<>();
        cr.accept(new ClassVisitor(AsmUtils.ASM_OPCODES_VERSION) {
            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                             final String[] exceptions) {
                if (name.equals(methodName)) {
                    return new MethodVisitor(AsmUtils.ASM_OPCODES_VERSION) {
                        @Override
                        public void visitLdcInsn(final Object cst) {
                            if (cst instanceof String) {
                                strings.add(desc + ":" + cst);
                            }
                        }
                    };
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        }, 0);
        return strings;
    }

}