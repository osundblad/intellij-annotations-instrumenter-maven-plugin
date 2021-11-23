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
package se.eris.asm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;

/**
* Collects class info. That is name, modifiers, super class, and interfaces.
*/
public class ClassInfoVisitor extends ClassVisitor {

    private ClassInfo classInfo;

    public ClassInfoVisitor() {
        super(AsmUtils.ASM_OPCODES_VERSION);
    }

    @Override
    public void visit(final int version, final int access, @NotNull final String name, final String signature, @Nullable final String superName, @Nullable final String[] interfaces) {
        classInfo = new ClassInfo(version, access, name, signature, superName, interfaces);
    }

    @NotNull
    public ClassInfo getClassInfo() {
        if (classInfo == null) {
            throw new RuntimeException(ClassInfo.class.getSimpleName() + " has not benn initialized (i.e. visit(...) has not yet been called)");
        }
        return classInfo;
    }
}
