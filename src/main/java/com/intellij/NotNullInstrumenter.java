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
package com.intellij;

import com.intellij.compiler.instrumentation.InstrumentationClassFinder;
import com.intellij.compiler.instrumentation.InstrumenterClassWriter;
import com.intellij.compiler.notNullVerification.NotNullInstrumenterClassVisitor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import se.eris.asm.AsmUtils;
import se.eris.maven.LogWrapper;
import se.eris.notnull.Configuration;
import se.eris.notnull.InstrumentExecutionException;
import se.eris.util.ClassFileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * @author Olle Sundblad
 */
public class NotNullInstrumenter {

    private static final int NO_FLAGS = 0;

    @NotNull
    private final LogWrapper logger;

    public NotNullInstrumenter(@NotNull final LogWrapper logWrapper) {
        logger = logWrapper;
    }

    public int instrument(final Path classesDirectory, final Configuration configuration, @NotNull final List<URL> urls) {
        final Set<File> classFiles = ClassFileUtils.getClassFiles(classesDirectory);
        return instrument(classFiles, configuration, urls);
    }

    public int instrument(final Set<File> classFiles, final Configuration configuration, @NotNull final List<URL> urls) {
        final InstrumentationClassFinder finder = new InstrumentationClassFinder(urls.toArray(new URL[0]));
        return instrumentDirectoryRecursive(finder, configuration, classFiles);
    }

    private int instrumentDirectoryRecursive(@NotNull final InstrumentationClassFinder finder, final Configuration configuration, final Set<File> classFiles) {
        int instrumentedCounter = 0;
        for (final File file : classFiles) {
            instrumentedCounter += instrumentFile(file, finder, configuration);
        }
        return instrumentedCounter;
    }

    private int instrumentFile(@NotNull final File file, @NotNull final InstrumentationClassFinder finder, final Configuration configuration) {
        logger.debug("Adding NotNull assertions to " + file.getPath());
        try {
            return instrumentClass(file, finder, configuration) ? 1 : 0;
        } catch (final RuntimeException | IOException e) {
            throw new InstrumentExecutionException("NotNull instrumentation failed for " + file.getPath(), e);
        }
    }

    private static boolean instrumentClass(@NotNull final File file, @NotNull final InstrumentationClassFinder finder, final Configuration configuration) throws IOException {
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            final ClassReader classReader = new ClassReader(inputStream);

            final int fileVersion = getClassFileVersion(classReader);

            if (AsmUtils.javaVersionSupportsAnnotations(fileVersion)) {
                final ClassWriter writer = new InstrumenterClassWriter(getAsmClassWriterFlags(fileVersion), finder);

                final NotNullInstrumenterClassVisitor instrumentingVisitor = new NotNullInstrumenterClassVisitor(writer, configuration);
                classReader.accept(instrumentingVisitor, NO_FLAGS);
                if (instrumentingVisitor.hasInstrumented()) {
                    try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        fileOutputStream.write(writer.toByteArray());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return the flags for class writer
     */
    private static int getAsmClassWriterFlags(final int version) {
        return (AsmUtils.asmOpcodeToJavaVersion(version) >= AsmUtils.JAVA_VERSION_6) ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
    }

    private static int getClassFileVersion(@NotNull final ClassReader reader) {
        final int[] classFileVersion = new int[1];
        reader.accept(new ClassVisitor(AsmUtils.ASM_OPCODES_VERSION) {
            @Override
            public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
                classFileVersion[0] = version;
            }
        }, NO_FLAGS);
        return classFileVersion[0];
    }
}
