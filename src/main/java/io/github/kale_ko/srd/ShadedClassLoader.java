package io.github.kale_ko.srd;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.jetbrains.annotations.NotNull;

public class ShadedClassLoader extends ClassLoader {
    protected final @NotNull Path jarPath;

    public ShadedClassLoader(@NotNull Path jarPath) {
        super(null);

        this.jarPath = jarPath;
    }

    protected boolean cataloged = false;
    protected Map<String, List<Path>> resourceCatalog = new HashMap<>();
    protected Map<String, List<Path>> extractedResourceCatalog = new HashMap<>();

    protected synchronized void catalogAll() throws IOException {
        if (this.cataloged) {
            return;
        }
        this.cataloged = true;

        Path tempDir = Files.createTempDirectory("srd-lib-");

        this.catalog(this.jarPath, tempDir);
    }

    private synchronized void catalog(Path parentJar, Path tempDir) throws IOException {
        List<Path> jars = new ArrayList<>();

        try (JarInputStream jarInputStream = new JarInputStream(new BufferedInputStream(Files.newInputStream(parentJar)))) {
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (!jarEntry.isDirectory()) {
                    Path file = tempDir.resolve(parentJar.getFileName()).resolve(jarEntry.getName());
                    if (!Files.exists(file.getParent())) {
                        Files.createDirectories(file.getParent());
                    }

                    if (jarEntry.getName().endsWith(".jar")) {
                        jars.add(file);
                    }

                    try (OutputStream fileOutputStream = Files.newOutputStream(file)) {
                        int read;
                        byte[] buf = new byte[4096];
                        while ((read = jarInputStream.read(buf)) != -1) {
                            fileOutputStream.write(buf, 0, read);
                        }
                    }

                    if (!this.resourceCatalog.containsKey(jarEntry.getName())) {
                        this.resourceCatalog.put(jarEntry.getName(), new ArrayList<>());
                    }
                    this.resourceCatalog.get(jarEntry.getName()).add(parentJar);

                    if (!this.extractedResourceCatalog.containsKey(jarEntry.getName())) {
                        this.extractedResourceCatalog.put(jarEntry.getName(), new ArrayList<>());
                    }
                    this.extractedResourceCatalog.get(jarEntry.getName()).add(file);
                }
            }
        }

        for (Path jar : jars) {
            this.catalog(jar, tempDir);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String fileName = name.replace(".", "/") + ".class";

        if (!(this.extractedResourceCatalog.containsKey(fileName) && !this.extractedResourceCatalog.get(fileName).isEmpty())) {
            throw new ClassNotFoundException(name);
        }

        try (InputStream jarInputStream = Files.newInputStream(this.extractedResourceCatalog.get(fileName).get(0))) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                int read;
                byte[] buf = new byte[4096];
                while ((read = jarInputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, read);
                }

                return defineClass(name, outputStream.toByteArray(), 0, outputStream.size());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected URL findResource(String name) {
        if (!(this.resourceCatalog.containsKey(name) && !this.resourceCatalog.get(name).isEmpty())) {
            return null;
        }

        try {
            Path path = this.resourceCatalog.get(name).get(0);

            return new URL("jar", "", -1, "file:" + path.toAbsolutePath() + "!/" + name);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Enumeration<URL> findResources(String name) {
        if (!(this.resourceCatalog.containsKey(name) && !this.resourceCatalog.get(name).isEmpty())) {
            return null;
        }

        try {
            List<URL> resources = new ArrayList<>();

            for (Path path : this.resourceCatalog.get(name)) {
                resources.add(new URL("jar", "", -1, "file:" + path.toAbsolutePath() + "!/" + name));
            }

            return Collections.enumeration(resources);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}