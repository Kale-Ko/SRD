package io.github.kale_ko.srd;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;

public class ShadedMain {
    public static void main(String[] args) {
        System.out.println("Loading libraries...");

        ShadedClassLoader classLoader;
        Method mainMethod;

        {
            Path jarPath = new File(ShadedMain.class.getProtectionDomain().getCodeSource().getLocation().getFile()).toPath();

            try (JarInputStream inputStream = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarPath)))) {
                classLoader = new ShadedClassLoader(jarPath);
                classLoader.catalogAll();

                try {
                    String mainClazzString = inputStream.getManifest().getMainAttributes().getValue("Shaded-Main-Class");
                    Class<?> mainClazz = classLoader.loadClass(mainClazzString);

                    mainMethod = mainClazz.getDeclaredMethod("main", String[].class);
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    e.printStackTrace();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            mainMethod.invoke(null, new Object[] { args });
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}