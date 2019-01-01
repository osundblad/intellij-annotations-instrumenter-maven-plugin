/*
 * Copyright 2013-2017 Eris IT AB
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
package se.eris.exclude;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestExclude {

    public static void notNullParameter(@NotNull final String s) {
    }

    public static void unAnnotatedParameter(final String s) {
    }

    @NotNull
    public static String notNullReturn(@Nullable final String s) {
        return s;
    }

    public static String unAnnotatedReturn(@Nullable final String s) {
        return s;
    }

}
