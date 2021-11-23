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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class AsmUtils {

    public static final int ASM_OPCODES_VERSION = Opcodes.ASM9;

    public static final int JAVA_VERSION_5 = 5;
    public static final int JAVA_VERSION_6 = 6;

    private AsmUtils() {
    }

    public static boolean isReferenceType(@NotNull final Type type) {
        final int sort = type.getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY;
    }

    public static boolean javaVersionSupportsAnnotations(final int opcodeVersion) {
        return asmOpcodeToJavaVersion(opcodeVersion) >= JAVA_VERSION_5;
    }

    /**
     * @return the Java version as an integer (ie. 1, 2, 3, ... 8)
     * @param versionOpcode .
     */
    public static int asmOpcodeToJavaVersion(final int versionOpcode) {
        return versionOpcode % (3 << 16) - 44;
    }
}
