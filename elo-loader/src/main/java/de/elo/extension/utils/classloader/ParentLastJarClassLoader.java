package de.elo.extension.utils.classloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by Lucian.Dragomir on 5/22/2015.
 */
public class ParentLastJarClassLoader extends ClassLoader {

    private List<URL> jarFiles;
    private Hashtable<String, Class> classes = new Hashtable<String, Class>();
    private List<String> delegated = new ArrayList<String>();


    public ParentLastJarClassLoader() {
        this(new ArrayList<URL>(), Thread.currentThread().getContextClassLoader());
    }

    public ParentLastJarClassLoader(ClassLoader classLoaderParent) {
        this(new ArrayList<URL>(), classLoaderParent);
    }

    public ParentLastJarClassLoader(List<URL> classpath) {
        this(classpath, Thread.currentThread().getContextClassLoader());
    }

    public ParentLastJarClassLoader(List<URL> classpath, ClassLoader classLoaderParent) {
        super(classLoaderParent);
        this.jarFiles = classpath;
        System.out.println("Using a class classloader of type [" + this.getClass().getName() + "], instance [" + Integer.toHexString(this.hashCode()) + "]");
    }

    public boolean addJar(URL jarFile) {
        if (this.jarFiles.contains(jarFile)) {
            return false;
        }
        this.jarFiles.add(jarFile);
        return true;
    }

    @Override
    public Class findClass(String name) throws ClassNotFoundException {
        return loadClass(name, true);
    }

    @Override
    protected synchronized Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
        //avoid infinit loop between this and parrent classloaders
        if (delegated.contains(className)) {
            delegated.remove(className);
            throw new ClassNotFoundException("Not found " + className);
        }

        System.out.println("Load class [" + className + "]");
        try {
            System.out.println("    Trying loading class [" + className + "] in child classloader [Hash=" + Integer.toHexString(this.hashCode()) + "]");
            byte classByte[];
            Class result = null;

            //checks in cached classes
            result = (Class) classes.get(className);
            if (result != null) {
                return result;
            }

            for (URL jarFile : jarFiles) {
                try {
                    JarFile jar = new JarFile(jarFile.getFile());
                    JarEntry entry = jar.getJarEntry(className.replace(".", "/") + ".class");
                    InputStream is = jar.getInputStream(entry);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    int nextValue = is.read();
                    while (-1 != nextValue) {
                        byteStream.write(nextValue);
                        nextValue = is.read();
                    }

                    classByte = byteStream.toByteArray();
                    result = defineClass(className, classByte, 0, classByte.length, null);
                    classes.put(className, result);
                } catch (Exception e) {
                    continue;
                }
            }

            result = (Class) classes.get(className);
            if (result != null) {
                System.out.println("    Class [" + className + "] loaded by child classloader [Hash=" + Integer.toHexString(this.hashCode()) + "]");
                return result;
            } else {
                throw new ClassNotFoundException("Not found " + className);
            }
        } catch (ClassNotFoundException e) {
            if (!delegated.contains(className)) {
                System.out.println("    Delegating loading class [" + className + "] to parent class classloader [Hash=" + Integer.toHexString(super.hashCode()) + "]");
                delegated.add(className);
                try {
                    Class<?> clazz = super.loadClass(className, resolve);
                    return clazz;
                } finally {
                    delegated.remove(className);
                }
            }
            throw e;
        }
    }
}