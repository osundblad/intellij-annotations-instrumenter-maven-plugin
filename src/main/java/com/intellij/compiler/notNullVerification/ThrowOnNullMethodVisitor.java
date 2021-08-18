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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import se.eris.asm.AsmUtils;
import se.eris.asm.ClassInfo;
import se.eris.lang.LangUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class ThrowOnNullMethodVisitor extends MethodVisitor {

    static final String LJAVA_LANG_SYNTHETIC_ANNO = "Ljava/lang/Synthetic;";
    private static final String IAE_CLASS_NAME = "java/lang/IllegalArgumentException";
    private static final String ISE_CLASS_NAME = "java/lang/IllegalStateException";
    private static final String CONSTRUCTOR_NAME = "<init>";

    final Type[] argumentTypes;
    private final int methodAccess;
    private final Type returnType;
    final String methodName;
    private final ClassInfo classInfo;
    boolean isReturnNotNull;
    @Nullable
    private final Boolean isAnonymousClass;

    int syntheticCount;
    final List<Integer> notNullParams;
    private boolean instrumented;
    Label startGeneratedCodeLabel;
    private List<String> parameterNames = null;

    ThrowOnNullMethodVisitor(@Nullable final MethodVisitor mv, final Type[] argumentTypes, final Type returnType, final int methodAccess, final String methodName, final ClassInfo classInfo, final boolean isReturnNotNull, @Nullable final Boolean isAnonymousClass) {
        super(AsmUtils.ASM_OPCODES_VERSION, mv);
        this.argumentTypes = argumentTypes;
        this.methodAccess = methodAccess;
        this.returnType = returnType;
        this.methodName = methodName;
        this.classInfo = classInfo;
        this.isReturnNotNull = isReturnNotNull;
        this.isAnonymousClass = isAnonymousClass;

        if (isConstructor()) {
            syntheticCount += isAnonymousClass != null ? 1 : 0;
            syntheticCount += classInfo.isEnum() ? 2 : 0;
        }
        notNullParams = new ArrayList<>();
        instrumented = false;
    }

    private void setInstrumented() {
        instrumented = true;
    }

    /**
     * This will be invoked only when visiting bytecode produced by java 8+ compiler with '-parameters' option.
     */
    @Override
    public void visitParameter(final String name, final int access) {
        if (parameterNames == null) {
            parameterNames = new ArrayList<>(argumentTypes.length);
        }
        parameterNames.add(name);
        if (mv != null) {
            mv.visitParameter(name, access);
        }
    }

    /**
     * Visits a zero operand instruction (ie return).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void visitInsn(final int opcode) {
        if (shouldInclude() && opcode == Opcodes.ARETURN && isReturnNotNull && !isReturnVoidReferenceType()) {
            mv.visitInsn(Opcodes.DUP);
            final Label skipLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, skipLabel);
            generateThrow(ISE_CLASS_NAME, "NotNull method " + classInfo.getName() + "." + methodName + " must not return null", skipLabel);
        }
        mv.visitInsn(opcode);
    }

    boolean hasInstrumented() {
        return instrumented;
    }

    private boolean isStatic() {
        return (methodAccess & Opcodes.ACC_STATIC) != 0;
    }

    boolean isParameter(final int index) {
        return isStatic() ? index < argumentTypes.length : index <= argumentTypes.length;
    }

    /**
     * Starts the visit of the method's code, if any (ie non abstract method).
     */
    @Override
    public void visitCode() {
        if (shouldInclude()) {
            if (!notNullParams.isEmpty()) {
                startGeneratedCodeLabel = new Label();
                mv.visitLabel(startGeneratedCodeLabel);
            }
            for (final Integer notNullParam : notNullParams) {
                int var = ((methodAccess & Opcodes.ACC_STATIC) == 0) ? 1 : 0;
                for (int i = 0; i < notNullParam + syntheticCount; ++i) {
                    var += argumentTypes[i].getSize();
                }
                mv.visitVarInsn(Opcodes.ALOAD, var);

                final Label end = new Label();
                mv.visitJumpInsn(Opcodes.IFNONNULL, end);

                generateThrow(IAE_CLASS_NAME, getThrowMessage(notNullParam), end);
            }
        }
        mv.visitCode();
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        try {
            super.visitMaxs(maxStack, maxLocals);
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("visitMaxs processing failed for method " + methodName + ": " + e.getMessage());
        }
    }

    private boolean shouldInclude() {
        return !shouldSkip();
    }

    private boolean shouldSkip() {
        return isSynthetic() || isAnonymousClassConstructor() || isEqualsMethod();
    }

    private boolean isAnonymousClassConstructor() {
        return isAnonymousClass != null && isAnonymousClass && isConstructor();
    }

    private boolean isConstructor() {
        return CONSTRUCTOR_NAME.equals(this.methodName);
    }

    private boolean isSynthetic() {
        return (this.methodAccess & Opcodes.ACC_SYNTHETIC) != 0;
    }

    private boolean isEqualsMethod() {
        return "equals".equals(this.methodName) &&
                this.returnType.equals(Type.BOOLEAN_TYPE) &&
                this.argumentTypes.length == 1 &&
                this.argumentTypes[0].getSort() == Type.OBJECT;
    }

    private void generateThrow(@NotNull final String exceptionClass, @NotNull final String description, @NotNull final Label end) {
        final String exceptionParamClass = "(" + LangUtils.convertToJavaClassName(String.class.getName()) + ")V";
        mv.visitTypeInsn(Opcodes.NEW, exceptionClass);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(description);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, exceptionClass, CONSTRUCTOR_NAME, exceptionParamClass, false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(end);

        setInstrumented();
    }

    boolean isReturnReferenceType() {
        return AsmUtils.isReferenceType(this.returnType);
    }

    boolean isReturnVoidReferenceType() {
        return returnType.getClassName().equals(Void.class.getName());
    }

    boolean isParameterReferenceType(final int parameter) {
        return AsmUtils.isReferenceType(getArgumentType(parameter));
    }

    private Type getArgumentType(final int parameter) {
        final int argumentNumber = parameter + syntheticCount;
        return argumentTypes[argumentNumber];
    }

    @NotNull
    private String getThrowMessage(final int parameterNumber) {
        final String pname = parameterNames == null || parameterNames.size() <= (parameterNumber + syntheticCount) ? "" : String.format(" (parameter '%s')", parameterNames.get(parameterNumber + syntheticCount));
        return String.format("%s argument %d%s of %s.%s must not be null", notNullCause(), parameterNumber, pname, classInfo.getName(), methodName);
    }

    /**
     * Returns the reason for the parameter to be instrumented as non-null one.
     * 
     * @return the reason that the parameter was instrumented as non-null.
     */
    @NotNull
    protected abstract String notNullCause();

    void increaseSyntheticCount() {
        syntheticCount++;
    }

}
