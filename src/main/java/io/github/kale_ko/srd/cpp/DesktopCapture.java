package io.github.kale_ko.srd.cpp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class DesktopCapture {
    static {
        try {
            String libraryName = System.getProperty("os.name").toLowerCase().startsWith("win") ? "libDesktopCapture-windows.dll" : "libDesktopCapture-linux.so";

            Path jarFile = new File(DesktopCapture.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toPath();
            Path libraryFile = Files.createTempFile("srd-", System.getProperty("os.name").toLowerCase().startsWith("win") ? ".dll" : ".so"); // Windows requires .dll file extension, might as well add linux .so

            if (!Files.isDirectory(jarFile)) {
                try (JarInputStream inputStream = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarFile)))) {
                    try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(libraryFile))) {
                        JarEntry entry;
                        while ((entry = inputStream.getNextJarEntry()) != null) {
                            if (libraryName.equalsIgnoreCase(entry.getName())) {
                                int read;
                                byte[] buf = new byte[2048];
                                while ((read = inputStream.read(buf)) != -1) {
                                    outputStream.write(buf, 0, read);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Program must be run as a standalone jar!");
            }

            System.load(libraryFile.toRealPath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    protected static int refCount = 0;

    public native long connectDisplay();

    public native void disconnectDisplay(long displayHandle);

    public native long[] getScreens(long displayHandle);

    public native byte[] captureScreen(long displayHandle);

    public native byte[] captureScreen(long displayHandle, long screenHandle);
}