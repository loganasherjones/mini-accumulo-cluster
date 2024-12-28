package com.loganasherjones.mac;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.VFSClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultClasspathLoader implements ClasspathLoader {

    private static final Logger log = LoggerFactory.getLogger(DefaultClasspathLoader.class);

    private final List<String> staticClasspath;

    public DefaultClasspathLoader(List<String> staticClasspath) {
        this.staticClasspath = staticClasspath;
    }

    public String getClasspath() throws IOException {
        StringBuilder classpathBuilder = new StringBuilder();

        if (this.staticClasspath == null || this.staticClasspath.isEmpty()) {
            appendDefaultClasspath(classpathBuilder, getClassLoaders());
        } else {
            for (String s : staticClasspath) {
                classpathBuilder.append(File.pathSeparator).append(s);
            }
        }

        String classpath = classpathBuilder.toString();
        log.trace(classpath);
        return classpath;
    }

    private void appendDefaultClasspath(StringBuilder classpathBuilder, List<ClassLoader> classloaders) throws IOException {
        try {
            // assume 0 is the system classloader and skip it
            for (int i = 1; i < classloaders.size(); i++) {
                ClassLoader classLoader = classloaders.get(i);

                if (classLoader instanceof URLClassLoader) {

                    for (URL u : ((URLClassLoader) classLoader).getURLs()) {
                        append(classpathBuilder, u);
                    }

                } else if (classLoader instanceof VFSClassLoader) {

                    VFSClassLoader vcl = (VFSClassLoader) classLoader;
                    for (FileObject f : vcl.getFileObjects()) {
                        append(classpathBuilder, f.getURL());
                    }
                } else {
                    if (classLoader.getClass().getName()
                            .equals("jdk.internal.loader.ClassLoaders$AppClassLoader")) {
                        log.debug("Detected Java 11 classloader: {}", classLoader.getClass().getName());
                    } else {
                        log.debug("Detected unknown classloader: {}", classLoader.getClass().getName());
                    }
                    String javaClassPath = System.getProperty("java.class.path");
                    if (javaClassPath == null) {
                        throw new IllegalStateException("java.class.path is not set");
                    } else {
                        log.debug("Using classpath set by java.class.path system property: {}",
                                javaClassPath);
                    }
                    classpathBuilder.append(File.pathSeparator).append(javaClassPath);
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

    }

    private void append(StringBuilder classpathBuilder, URL url) throws URISyntaxException {
        File file = new File(url.toURI());
        // do not include dirs containing hadoop or accumulo site files
        if (!containsSiteFile(file))
            classpathBuilder.append(File.pathSeparator).append(file.getAbsolutePath());
    }

    private boolean containsSiteFile(File f) {
        if (!f.isDirectory()) {
            return false;
        } else {
            File[] files = f.listFiles(pathname -> pathname.getName().endsWith("site.xml"));
            return files != null && files.length > 0;
        }
    }

    private List<ClassLoader> getClassLoaders() {
        ArrayList<ClassLoader> classloaders = new ArrayList<>();

        ClassLoader cl = this.getClass().getClassLoader();

        while (cl != null) {
            classloaders.add(cl);
            cl = cl.getParent();
        }

        Collections.reverse(classloaders);
        return classloaders;
    }
}
