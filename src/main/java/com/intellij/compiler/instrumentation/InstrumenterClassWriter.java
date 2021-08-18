/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.compiler.instrumentation;

import org.objectweb.asm.ClassWriter;

/**
 * @author Eugene Zhuravlev
 *         Date: 3/27/12
 */
public class InstrumenterClassWriter extends ClassWriter {
    private final InstrumentationClassFinder myFinder;

    public InstrumenterClassWriter(final int flags, final InstrumentationClassFinder finder) {
        super(flags);
        myFinder = finder;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        try {
            final PseudoClass cls1 = myFinder.loadClass(type1);
            final PseudoClass cls2 = myFinder.loadClass(type2);
            if (cls1.isAssignableFrom(cls2)) {
                return cls1.getName();
            }
            if (cls2.isAssignableFrom(cls1)) {
                return cls2.getName();
            }
            if (cls1.isInterface() || cls2.isInterface()) {
                return PseudoClass.JAVA_LANG_OBJECT;
            } else {
                PseudoClass pseudoClass = cls1;
                do {
                    pseudoClass = pseudoClass.getSuperClass();
                } while (!pseudoClass.isAssignableFrom(cls2));
                return pseudoClass.getName();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
