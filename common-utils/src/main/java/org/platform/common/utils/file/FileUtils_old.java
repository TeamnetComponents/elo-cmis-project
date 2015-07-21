//package org.platform.common.utils.file;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.activation.MimetypesFileTypeMap;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.utils.Properties;
//import java.utils.regex.Pattern;
//
///**
// * Created by Lucian.Dragomir on 6/29/2014.
// */
//public class FileUtils {
//    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);
//
//
//    private static final String EXTENSION_DELIMITER = ".";
//    private static MimetypesFileTypeMap mimetypesFileTypeMap = null;
//
//
//
//    private String rootPath;
//    private String pathDelimiter;
//    private String pathDelimiterQuoted;
//
//    public FileUtils(String rootPath, String pathDelimiter) {
//        this.rootPath = rootPath;
//        this.pathDelimiter = pathDelimiter;
//        this.pathDelimiterQuoted = Pattern.quote(pathDelimiter);
//    }
//
//    private static MimetypesFileTypeMap createMimetypesFileTypeMap() {
//        MimetypesFileTypeMap mimetypesFileTypeMapReturn = new MimetypesFileTypeMap();
//        return mimetypesFileTypeMapReturn;
//    }
//
//    private static MimetypesFileTypeMap getMimetypesFileTypeMap() {
//        if (mimetypesFileTypeMap == null) {
//            synchronized (FileUtils.class) {
//                if (mimetypesFileTypeMap == null) {
//                    mimetypesFileTypeMap = createMimetypesFileTypeMap();
//                }
//            }
//        }
//        return mimetypesFileTypeMap;
//    }
//
//    public static String getExtensionDelimiter() {
//        return EXTENSION_DELIMITER;
//    }
//
//    public static void main(String[] args) {
//        //elo
//        FileUtils fuElo = new FileUtils("ARCPATH:", String.valueOf((char) 182));
//        FileUtils fuCmis = new FileUtils("/", "/");
//
//        FileUtils fu = fuCmis;
//        FileUtils fuOther = (fu.equals(fuElo) ? fuCmis : fuElo);
//
//        String[] fileNames = {
//                fu.getRootPath() + fu.getPathDelimiter(),
//                fu.getRootPath(),
//                fu.getPathDelimiter() + "gigi",
//                fu.getPathDelimiter() + "gigi" + fu.getPathDelimiter() + "ionel" + fu.getPathDelimiter() + "textfile.txt",
//                fu.getRootPath() + fu.getPathDelimiter() + "gigi" + fu.getPathDelimiter() + "ionel" + fu.getPathDelimiter() + "textfile.txt",
//                fu.getPathDelimiter() + "gigi" + fu.getPathDelimiter() + "ionel" + fu.getPathDelimiter() + "noextfile",
//                fu.getPathDelimiter(),
//                ""};
//
//        for (String pathName : fileNames) {
//            System.out.println("-----------------------------------------------------------------");
//            System.out.println("path:                    >" + pathName + "<");
//            System.out.println("getNormalizedPathName:   >" + fu.getNormalizedPathName(pathName) + "<");
//            System.out.println("getFileName:             >" + fu.getFileName(pathName) + "<");
//            System.out.println("getFileExtension:        >" + fu.getFileExtension(pathName) + "<");
//            System.out.println("getFileBaseName:         >" + fu.getFileBaseName(pathName) + "<");
//            System.out.println("getParentFolderPathName: >" + fu.getParentFolderPathName(pathName) + "<");
//            System.out.println("converdedPathName:       >" + fu.convertPathName(pathName, fuOther) + "<");
//            System.out.println();
//        }
//
//    }
//
//    public String getRootPath() {
//        return rootPath;
//    }
//
//    public String getPathDelimiter() {
//        return pathDelimiter;
//    }
//
//    public String getMimeType(String filePathName) {
//        String mimeType = null;
//        try {
//            String fileName = getFileName(filePathName).toLowerCase();
//            //mimeType = Files.probeContentType(FileSystems.getFileUtilsDMS().getPath(fileName));
//            synchronized (FileUtils.class) {
//                mimeType = getMimetypesFileTypeMap().getContentType(fileName);
//            }
//
//            if (mimeType == null)
//                return "application/octet-stream";
//
//            //it is just to keep the exception catch-er required when Files/FileSystems from nio is used with jdk1.7
//            if (false) {
//                throw new IOException();
//            }
//        } catch (IOException e) {
//            return "application/octet-stream";
//        }
//        return mimeType;
//    }
//
//    /*
//    public String getMimeType(String filePathName) {
//        try {
//            String fileName = getFileName(filePathName);
//            return Files.probeContentType(FileSystems.getDefault().getPath(fileName));
//        } catch (IOException e) {
//            return "application/octet-stream";
//        }
//    }
//    */
//
//    public String getFileName(String filePathName) {
//        String fileName = filePathName;
//        int lastDelimiterIndex = filePathName.lastIndexOf(pathDelimiter);
//        if (lastDelimiterIndex != -1) {
//            fileName = filePathName.substring(lastDelimiterIndex + 1);
//        }
//        if (fileName.startsWith(rootPath)) {
//            fileName = fileName.substring(rootPath.length());
//        }
//        return fileName;
//    }
//
//    public String getFolderName(String folderPathName) {
//        return getFileName(folderPathName);
//    }
//
//    public String getFileExtension(String filePathName) {
//        String fileExtension = "";
//        String fileName = getFileName(filePathName);
//        int lastDelimiterIndex = fileName.lastIndexOf(EXTENSION_DELIMITER);
//        if (lastDelimiterIndex != -1) {
//            fileExtension = fileName.substring(lastDelimiterIndex + 1);
//        }
//        return fileExtension;
//    }
//
//    public String getFileBaseName(String filePathName) {
//        String fileName = getFileName(filePathName);
//        String fileBaseName = fileName;
//        int lastDelimiterIndex = fileName.lastIndexOf(EXTENSION_DELIMITER);
//        if (lastDelimiterIndex != -1) {
//            fileBaseName = fileName.substring(0, lastDelimiterIndex);
//        }
//        return fileBaseName;
//    }
//
//    public boolean isValidName(String name) {
//        if (name == null
//                || name.isEmpty()
//                || name.indexOf(File.separatorChar) != -1
//                || name.indexOf(this.pathDelimiter) != -1
//                || !name.equalsIgnoreCase(name.trim())) {
//            return false;
//        }
//        return true;
//    }
//
//    public String getNormalizedPathName(String pathName) {
//        if (!pathName.startsWith(rootPath)) {
//            if (pathName.startsWith(pathDelimiter)) {
//                pathName = rootPath + pathName;
//            } else {
//                pathName = rootPath + pathDelimiter + pathName;
//            }
//        }
//        if (isRootDelimiter() && (pathName.startsWith(rootPath + pathDelimiter))) {
//            pathName = rootPath + pathName.substring((rootPath + pathDelimiter).length());
//        }
//        if (pathName.equals(rootPath + pathDelimiter)) {
//            pathName = rootPath;
//        }
//        return pathName;
//    }
//
//    public String getParentFolderPathName(String pathName) {
//        String parentFolderPathName = pathName;
//        int lastDelimiterIndex = parentFolderPathName.lastIndexOf(pathDelimiter);
//        if (lastDelimiterIndex != -1 && lastDelimiterIndex != 0) {
//            parentFolderPathName = parentFolderPathName.substring(0, lastDelimiterIndex);
//        } else {
//            parentFolderPathName = rootPath;
//        }
//        return parentFolderPathName;
//    }
//
//    public String concatenate(String pathName, String name) {
//        if (name == null || name.trim().isEmpty()) {
//            return pathName;
//        }
//        if (!name.equals(getFileName(name))) {
//            throw new RuntimeException("The name is a tree path name, the method accepts only simple name (without path).");
//        }
//        return pathName + (pathName.endsWith(pathDelimiter) ? "" : pathDelimiter) + name;
//    }
//
//    public String convertPathName(String pathName, FileUtils to) {
//        pathName = getNormalizedPathName(pathName).substring(rootPath.length());
//        pathName = pathName.replaceAll(this.pathDelimiterQuoted, to.getPathDelimiter());
//        return to.getNormalizedPathName(pathName);
//    }
//
//    public String convertPathNameWithoutNormalize(String pathName, FileUtils to) {
//        String toPathDelimiter = java.utils.regex.Matcher.quoteReplacement(to.getPathDelimiter());
//        pathName = pathName.replaceAll(this.pathDelimiterQuoted, toPathDelimiter);
//        return pathName;
//    }
//
//    private boolean isRootDelimiter() {
//        return rootPath.endsWith(pathDelimiter);
//    }
//
//    public static Properties openOsResource(String osFilePath) throws IOException {
//        Properties prop = new Properties();
//        InputStream in = null;
//        try {
//            in = new FileInputStream(osFilePath);
//            prop.load(in);
//        } catch (IOException e) {
//            throw e;
//        } finally {
//            try {
//                in.close();
//            } catch (Exception e) {
//                //do nothing
//            }
//
//        }
//        return prop;
//    }
//
//    public static String getOsRoot() {
//        //avem limitare pentru windows, partitia "C:" este considerata root
//        String root = "";
//        if (System.getProperty("os.name").toLowerCase().contains("win")) {
//            root = "C:";
//        } else {
//            root = System.getProperty("file.separator");
//        }
//        return root;
//    }
//
//    public static String getOsFileSeparator() {
//        return System.getProperty("file.separator");
//    }
//}
