package io.github.kale_ko.srd.cpp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class DesktopCapture implements AutoCloseable {
    static {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");

            String libraryName = isWindows ? "libDesktopCapture-windows.dll" : "libDesktopCapture-linux.so";

            Path jarFile = new File(DesktopCapture.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toPath();
            Path libraryFile = Files.createTempFile("srd-", isWindows ? ".dll" : ".so"); // Windows requires .dll file extension, might as well add linux .so

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
            throw new RuntimeException("Failed to load native library.", e);
        }
    }

    public static record Capture(int width, int height, byte[] data) {
        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{width=" + width + ", height=" + height + ", data=" + data + "}";
        }
    }

    public class Screen {
        protected final @NotNull DesktopCapture desktop = DesktopCapture.this;

        private final long handle;

        protected final @NotNull String name;

        protected final int x;
        protected final int y;

        protected final int width;
        protected final int height;

        protected final boolean primary;

        private Screen(long handle, @NotNull String name, int x, int y, int width, int height, boolean primary) {
            this.handle = handle;

            this.name = name;

            this.x = x;
            this.y = y;

            this.width = width;
            this.height = height;

            this.primary = primary;
        }

        public @NotNull String getName() {
            return this.name;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public boolean isPrimary() {
            return this.primary;
        }

        public boolean isClosed() {
            return desktop.closed;
        }

        public @NotNull Capture capture() {
            if (desktop.closed) {
                throw new RuntimeException(desktop.getClass().getSimpleName() + " is closed.");
            }

            return this.n_capture();
        }

        private native Capture n_capture();

        private native void n_close();

        @Override
        public @NotNull String toString() {
            return this.getClass().getSimpleName() + "{handle=" + this.handle + ", name='" + this.name + "', x=" + this.x + ", y=" + this.y + ", width=" + this.width + ", height=" + this.height + ", primary=" + this.primary + ", closed=" + desktop.closed + "}";
        }
    }

    private final long handle;

    protected final @NotNull String name;

    protected boolean closed = false;
    private List<Screen> screens = null;

    private DesktopCapture(long handle, @NotNull String name) {
        this.handle = handle;

        this.name = name;
    }

    public @NotNull String getName() {
        return this.name;
    }

    public @NotNull Screen getDefaultScreen() {
        if (this.closed) {
            throw new RuntimeException(this.getClass().getSimpleName() + " is closed.");
        }

        return this.getScreens().get(0);
    }

    public @NotNull @Unmodifiable List<Screen> getScreens() {
        if (this.closed) {
            throw new RuntimeException(this.getClass().getSimpleName() + " is closed.");
        }

        if (this.screens == null) {
            Screen[] n_screens = this.n_getScreens();

            this.screens = new ArrayList<>(List.of(n_screens));
            this.screens.sort((a, b) -> (a.isPrimary() ? -1 : 1) + (b.isPrimary() ? 1 : -1));
        }

        return Collections.unmodifiableList(this.screens);
    }

    private native Screen[] n_getScreens();

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.closed) {
            throw new RuntimeException(this.getClass().getSimpleName() + " is already closed.");
        }
        this.closed = true;

        if (this.screens != null) {
            for (Screen screen : this.screens) {
                screen.n_capture();
            }
        }

        this.n_close();
    }

    private native void n_close();

    @Override
    public @NotNull String toString() {
        return this.getClass().getSimpleName() + "{handle=" + this.handle + ", name='" + this.name + "', closed=" + this.closed + "}";
    }

    public static @NotNull DesktopCapture create() {
        return DesktopCapture.n_create();
    }

    private static native DesktopCapture n_create();
}