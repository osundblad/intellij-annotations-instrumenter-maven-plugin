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
package com.intellij.compiler.notNullVerification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import se.eris.asm.ClassInfo;

import java.util.Set;

class AnnotationThrowOnNullMethodVisitor extends ThrowOnNullMethodVisitor {

    private final Set<String> notNullAnnotations;

    AnnotationThrowOnNullMethodVisitor(@Nullable final MethodVisitor methodVisitor, @NotNull final Type[] argumentTypes, @NotNull final Type returnType, final int access, @NotNull final String methodName, @NotNull final ClassInfo classInfo, @NotNull final Set<String> notNullAnnotations, final Boolean isAnonymous) {
        super(methodVisitor, argumentTypes, returnType, access, methodName, classInfo, false, isAnonymous);
        this.notNullAnnotations = notNullAnnotations;
    }

    /**
     * Visits an annotation of a parameter this method.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String annotation, final boolean visible) {
        final AnnotationVisitor av = mv.visitParameterAnnotation(parameter, annotation, visible);
        if (isParameterReferenceType(parameter) && isNotNullAnnotation(annotation)) {
            notNullParams.add(parameter);
        } else if (annotation.equals(LJAVA_LANG_SYNTHETIC_ANNO)) {
            // See asm r1278 for what we do this,
            // http://forge.objectweb.org/tracker/index.php?func=detail&aid=307392&group_id=23&atid=100023
            increaseSyntheticCount();
        }
        return av;
    }

    /**
     * Visits an annotation of this method.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public AnnotationVisitor visitAnnotation(final String annotation, final boolean visible) {
        final AnnotationVisitor av = mv.visitAnnotation(annotation, visible);
        if (isReturnReferenceType() && isNotNullAnnotation(annotation)) {
            isReturnNotNull = true;
        }

        return av;
    }

    @Override
    @NotNull
    protected String notNullCause() {
        return "NotNull annotated";
    }

    /**
     * Visits a local variable declaration.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(final String name, final String description, final String signature, final Label start, final Label end, final int index) {
        mv.visitLocalVariable(name, description, signature, (isParameter(index) && startGeneratedCodeLabel != null) ? startGeneratedCodeLabel : start, end, index);
    }

    private boolean isNotNullAnnotation(@NotNull final String annotation) {
        return notNullAnnotations.contains(annotation);
    }

}
