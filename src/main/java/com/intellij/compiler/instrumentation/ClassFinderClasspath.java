/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.eris.notnull.instrumentation.Resource;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ClassFinderClasspath {
    private static final String FILE_PROTOCOL = "file";

    private final Stack<URL> myUrls = new Stack<>();
    private final List<Loader> myLoaders = new ArrayList<>();
    private final Map<URL, Loader> myLoadersMap = new HashMap<>();

    ClassFinderClasspath(final URL[] urls) {
        for (int i = urls.length - 1; i >= 0; i--) {
            myUrls.push(urls[i]);
        }
    }

    private static String unescapePercentSequences(final String s) {
        if (s.indexOf('%') == -1) {
            return s;
        }
        final int len = s.length();
        final StringBuilder decoded = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            final char c = s.charAt(i);
            if (c == '%') {
                final List<Integer> bytes = new ArrayList<>();
                while ((s.charAt(i) == '%') && ((i + 2) < len)) {
                    final int d1 = decode(s.charAt(i + 1));
                    final int d2 = decode(s.charAt(i + 2));
                    if ((d1 != -1) && (d2 != -1)) {
                        bytes.add((d1 << 4) | d2);
                        i += 3;
                    } else {
                        break;
                    }
                }
                if (!bytes.isEmpty()) {
                    final byte[] bytesArray = new byte[bytes.size()];
                    for (int j = 0; j < bytes.size(); j++) {
                        bytesArray[j] = (byte) bytes.get(j).intValue();
                    }
                    try {
                        decoded.append(new String(bytesArray, "UTF-8"));
                        continue;
                    } catch (final UnsupportedEncodingException ignored) {
                    }
                }
            }

            decoded.append(c);
            i++;
        }
        return decoded.toString();
    }

    /**
     * @param c a hexadecimal character
     * @return the hexadecimal character as an int
     */
    @Contract(pure = true)
    private static int decode(final char c) {
        if ((c >= '0') && (c <= '9')) {
            return c - '0';
        }
        if ((c >= 'a') && (c <= 'f')) {
            return (c - 'a') + 10;
        }
        if ((c >= 'A') && (c <= 'F')) {
            return (c - 'A') + 10;
        }
        return -1;
    }

    @Nullable
    Resource getResource(final String s) {
        int i = 0;
        for (Loader loader; (loader = getLoader(i)) != null; i++) {
            final Resource resource = loader.getResource(s);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    @Nullable
    private synchronized Loader getLoader(final int i) {
        while (myLoaders.size() < (i + 1)) {
            final URL url;
            synchronized (myUrls) {
                if (myUrls.empty()) {
                    return null;
                }
                url = myUrls.pop();
            }

            if (myLoadersMap.containsKey(url)) {
                continue;
            }

            final Loader loader;
            try {
                loader = getLoader(url);
                if (loader == null) {
                    continue;
                }
            } catch (final IOException ioexception) {
                continue;
            }

            myLoaders.add(loader);
            myLoadersMap.put(url, loader);
        }

        return myLoaders.get(i);
    }

    @Nullable
    private Loader getLoader(final URL url) throws IOException {
        String s;
        try {
            s = url.toURI().getSchemeSpecificPart();
        } catch (final URISyntaxException thisShouldNotHappen) {
            thisShouldNotHappen.printStackTrace();
            s = url.getFile();
        }

        Loader loader = null;
        if ((s != null) && new File(s).isDirectory()) {
            if (FILE_PROTOCOL.equals(url.getProtocol())) {
                loader = new FileLoader(url);
            }
        } else {
            loader = new JarLoader(url);
        }

        return loader;
    }


    private abstract static class Loader {
        static final String JAR_PROTOCOL = "jar";
        static final String FILE_PROTOCOL = "file";

        private final URL myURL;

        Loader(final URL url) {
            myURL = url;
        }

        URL getBaseURL() {
            return myURL;
        }

        public abstract Resource getResource(final String name);

    }

    private static class FileLoader extends Loader {
        private final File rootDir;

        FileLoader(final URL url) {
            super(url);
            if (!FILE_PROTOCOL.equals(url.getProtocol())) {
                throw new IllegalArgumentException(this.getClass().getSimpleName() + " requires the " + FILE_PROTOCOL + " protocol. (url: " + url + ")");
            } else {
                final String s = unescapePercentSequences(url.getFile().replace('/', File.separatorChar));
                rootDir = new File(s);
            }
        }

        @Nullable
        public Resource getResource(final String name) {

            try {
                final URL url = new URL(getBaseURL(), name);
                if (!url.getFile().startsWith(getBaseURL().getFile())) {
                    return null;
                }
            } catch (final Exception ignored) {
            }

            // check means we load or process resource so we check its existence via old way
            final File file = new File(rootDir, name.replace('/', File.separatorChar));
            if (file.exists()) {
                return new FileResource(file);
            }
            return null;
        }

        private static class FileResource extends Resource {
            private final File file;

            FileResource(final File file) {
                this.file = file;
            }

            @NotNull
            public InputStream getInputStream() throws IOException {
                return new BufferedInputStream(new FileInputStream(file));
            }

            public String toString() {
                return file.getAbsolutePath();
            }
        }

        public String toString() {
            return "FileLoader [" + rootDir + "]";
        }
    }

    private class JarLoader extends Loader {
        private final URL myURL;
        private ZipFile myZipFile;

        JarLoader(final URL url) throws IOException {
            super(new URL(JAR_PROTOCOL, "", -1, url + "!/"));
            myURL = url;
        }

        @Nullable
        private ZipFile acquireZipFile() throws IOException {
            ZipFile zipFile = myZipFile;
            if (zipFile == null) {
                zipFile = doGetZipFile();
                myZipFile = zipFile;
            }
            return zipFile;
        }

        @Nullable
        private ZipFile doGetZipFile() throws IOException {
            if (FILE_PROTOCOL.equals(myURL.getProtocol())) {
                final String s = unescapePercentSequences(myURL.getFile().replace('/', File.separatorChar));
                if (!new File(s).exists()) {
                    throw new FileNotFoundException(s);
                } else {
                    return new ZipFile(s);
                }
            }

            return null;
        }

        @Nullable
        public Resource getResource(final String name) {
            try {
                final ZipFile file = acquireZipFile();
                if (file != null) {
                    final ZipEntry entry = file.getEntry(name);
                    if (entry != null) {
                        return new JarLoader.JarResource(entry);
                    }
                }
            } catch (final Exception e) {
                return null;
            }
            return null;
        }

        private class JarResource extends Resource {
            private final ZipEntry zipEntry;

            JarResource(final ZipEntry zipEntry) {
                this.zipEntry = zipEntry;
            }

            @Nullable
            public InputStream getInputStream() {
                try {
                    final ZipFile file = acquireZipFile();
                    if (file == null) {
                        return null;
                    }

                    final InputStream inputStream = file.getInputStream(zipEntry);
                    if (inputStream == null) {
                        return null; // if entry was not found
                    }
                    return new FilterInputStream(inputStream) {
                    };
                } catch (final IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

        }

        public String toString() {
            return "JarLoader [" + myURL + "]";
        }
    }
}
