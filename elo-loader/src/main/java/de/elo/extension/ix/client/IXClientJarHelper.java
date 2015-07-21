package de.elo.extension.ix.client;

import de.elo.extension.ix.status.IXServiceStatusHelper;
import de.elo.extension.utils.classloader.ParentLastJarClassLoader;
import de.elo.extension.utils.http.HttpDownloadUtils;
import de.elo.extension.utils.zip.ZipUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Lucian.Dragomir on 5/21/2015.
 */
public class IXClientJarHelper {
    private String ixURL;
    private String basePath;
    private String version;


    public String getBasePath() {
        return basePath;
    }

    public String getVersion() {
        if (this.version == null) {
            try {
                askIx();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return version;
    }

    public String getJarPath() {
        String jarPath = null;
        if (getVersion() != null) {
            jarPath = getBasePath() + File.separator + getVersion();
        }
        return jarPath;
    }

    public List<URL> getJars() throws MalformedURLException {
        List<URL> urlList;
        File jarDirectory = new File(getJarPath());
        File[] jarFiles = jarDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        urlList = new ArrayList<URL>();
        for (File jarFile : jarFiles) {
            urlList.add(jarFile.toURI().toURL());
        }
        return urlList;
    }

    public IXClientJarHelper(String ixURL, String basePath) throws IOException {
        this.ixURL = ixURL;
        this.basePath = basePath;
        this.version = null;
    }

    public void addJarsToSystemClassLoader() throws MalformedURLException {
        List<URL> jars = this.getJars();
        for (URL jar : jars) {
            addURLToClassLoader(jar, (URLClassLoader) ClassLoader.getSystemClassLoader());
        }
    }

    public void addJarsToContextClassLoader() throws MalformedURLException {
        List<URL> jars = this.getJars();
        for (URL jar : jars) {
            addURLToClassLoader(jar, (URLClassLoader) Thread.currentThread().getContextClassLoader());
        }
    }

    public synchronized void setParentLastContextLoader() throws MalformedURLException {
        List<URL> jars = this.getJars();
        ClassLoader eloClassLoader = new ParentLastJarClassLoader(jars);
        Thread.currentThread().setContextClassLoader(eloClassLoader);
    }


    public synchronized static void addURLToClassLoader(URL url, URLClassLoader classLoader) throws MalformedURLException {
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
        try {
            Method method = classLoaderClass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[]{url});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MalformedURLException("Error when adding url to system ClassLoader ");
        }
    }

    private synchronized void askIx() throws IOException {
        String tempVersion = null;
        IXServiceStatusHelper IXServiceStatusHelper = new IXServiceStatusHelper(ixURL);
        Map<String, String> statusProperties = IXServiceStatusHelper.getIXStatusProperties();
        Map<String, String> links = IXServiceStatusHelper.getIXStatusLinks();

        tempVersion = statusProperties.get(IXServiceStatusHelper.VERSION);

        //create path if not exists
        File baseDirectory = new File(basePath);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdir();
        }

        //create version folder if not exists
        String versionPath = basePath + File.separator + tempVersion;
        String versionPathTemp = basePath + File.separator + tempVersion + "-temp";

        File versionDirectory = new File(versionPath);
        if (!versionDirectory.exists()) {
            File versionDirectoryTemp = new File(versionPathTemp);
            if (versionDirectoryTemp.exists()) {
                versionDirectoryTemp.delete();
            }
            versionDirectoryTemp.mkdir();

            //download ELO Client zip archive
            String archiveUrl = links.get(IXServiceStatusHelper.JAVA_CLIENT);
            String eloArchive = HttpDownloadUtils.execute(archiveUrl, versionPathTemp);
            if (eloArchive != null & eloArchive.length() > 0) {
                ZipUtils.unzip(eloArchive, versionPathTemp);
            }
            //delete archive
            File archiveFile = new File(eloArchive);
            archiveFile.delete();

            //rename temp directory
            versionDirectoryTemp.renameTo(versionDirectory);
        }

        if (versionDirectory.exists()) {
            this.version = tempVersion;
        }
    }
}
