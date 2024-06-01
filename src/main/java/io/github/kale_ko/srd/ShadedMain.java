package io.github.kale_ko.srd;

import java.io.BufferedInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;

public class ShadedMain {
    public static void main(String[] args) throws Exception {
        System.out.println("Loading libraries...");

        ShadedClassLoader classLoader;
        Method mainMethod;

        {
            Path jarPath = new File(ShadedMain.class.getProtectionDomain().getCodeSource().getLocation().getFile()).toPath();

            try (JarInputStream inputStream = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarPath)))) {
                classLoader = new ShadedClassLoader(jarPath);
                classLoader.catalogAll();

                String mainClazzString = inputStream.getManifest().getMainAttributes().getValue("Shaded-Main-Class");
                Class<?> mainClazz = classLoader.loadClass(mainClazzString);

                mainMethod = mainClazz.getDeclaredMethod("main", String[].class);
            }
        }

        mainMethod.invoke(null, new Object[] { args });
    }
}