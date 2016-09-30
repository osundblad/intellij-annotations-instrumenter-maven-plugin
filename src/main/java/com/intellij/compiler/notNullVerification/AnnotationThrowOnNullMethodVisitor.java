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
import org.objectweb.asm.*;
import se.eris.asm.AsmUtils;

import java.util.Set;

class AnnotationThrowOnNullMethodVisitor extends ThrowOnNullMethodVisitor {

    private final Set<String> notNullAnnotations;

    AnnotationThrowOnNullMethodVisitor(@Nullable final MethodVisitor methodVisitor, @NotNull final Type[] argumentTypes, @NotNull final Type returnType, final int access, @NotNull final String methodName, @NotNull final String className, @NotNull final Set<String> notNullAnnotations) {
        super(Opcodes.ASM5, methodVisitor, argumentTypes, returnType, access, methodName, className, false);
        this.notNullAnnotations = notNullAnnotations;
    }

    /**
     * Visits an annotation of a parameter this method.
     * <p>
     * {@inheritDoc}
     */
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String annotation, final boolean visible) {
        final AnnotationVisitor av = mv.visitParameterAnnotation(parameter, annotation, visible);
        if (AsmUtils.isReferenceType(argumentTypes[parameter])) {
            if (isNotNullAnnotation(annotation)) {
                notNullParams.add(parameter);
            }
        } else if (annotation.equals(LJAVA_LANG_SYNTHETIC_ANNO)) {
            // See asm r1278 for what we do this,
            // http://forge.objectweb.org/tracker/index.php?func=detail&aid=307392&group_id=23&atid=100023
            syntheticCount++;
        }
        return av;
    }

    /**
     * Visits an annotation of this method.
     * <p>
     * {@inheritDoc}
     */
    public AnnotationVisitor visitAnnotation(final String annotation, final boolean visible) {
        final AnnotationVisitor av = mv.visitAnnotation(annotation, visible);
        if (AsmUtils.isReferenceType(returnType) && isNotNullAnnotation(annotation)) {
            returnIsNotNull = true;
        }

        return av;
    }

    /**
     * Starts the visit of the method's code, if any (ie non abstract method).
     */
    public void visitCode() {
        if (!notNullParams.isEmpty()) {
            startGeneratedCodeLabel = new Label();
            mv.visitLabel(startGeneratedCodeLabel);
        }
        for (final Integer notNullParam : notNullParams) {
            int var = ((access & Opcodes.ACC_STATIC) == 0) ? 1 : 0;
            for (int i = 0; i < notNullParam; ++i) {
                var += argumentTypes[i].getSize();
            }
            mv.visitVarInsn(Opcodes.ALOAD, var);

            final Label end = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, end);

            generateThrow(IAE_CLASS_NAME, "Argument " + (notNullParam - syntheticCount) + " for @NotNull parameter of " + className + "." + methodName + " must not be null", end);
        }
    }

    /**
     * Visits a local variable declaration.
     * <p>
     * {@inheritDoc}
     */
    public void visitLocalVariable(final String name, final String description, final String signature, final Label start, final Label end, final int index) {
        final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        final boolean isParameter = isStatic ? index < argumentTypes.length : index <= argumentTypes.length;
        mv.visitLocalVariable(name, description, signature, (isParameter && startGeneratedCodeLabel != null) ? startGeneratedCodeLabel : start, end, index);
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        try {
            super.visitMaxs(maxStack, maxLocals);
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("visitMaxs processing failed for method " + methodName + ": " + e.getMessage());
        }
    }

    private boolean isNotNullAnnotation(@NotNull final String annotation) {
        return notNullAnnotations.contains(annotation);
    }

}