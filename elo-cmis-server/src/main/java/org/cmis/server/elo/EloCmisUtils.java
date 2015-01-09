package org.cmis.server.elo;

import de.elo.ix.client.*;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.cmis.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Lucian.Dragomir on 6/9/2014.
 */
public class EloCmisUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisUtils.class);

    public static final int FOLDER_ROOT_ID_ELO = 1;
    //public static final char FOLDER_SEPARATOR_ELO = (char) 182;
    //public static final String FOLDER_DELIMITER_ELO = String.valueOf(FOLDER_SEPARATOR_ELO);
    //public static final String FOLDER_ROOT_CMIS = "/";
    //public static final String FOLDER_ROOT_ELO = "ARCPATH:";
    //public static final String FOLDER_DELIMITER_CMIS = "/";
    private final static String VERSION_INITIAL = "0.0";
    private final static String VERSION_DELIMITER = ".";
    private final static int NUMBER_OF_RESULTS = 7;
    private final static FileUtils fileUtilsElo = new FileUtils("ARCPATH:", String.valueOf((char) 182));
    private final static FileUtils fileUtilsCmis = new FileUtils("/", "/");
    public static String DOCUMENT_ID_MISSING = "-1";
    private static String DOCUMENT_DELIMITER = "-";
    private static String DOCUMENT_DELIMITER_PATTERN = Pattern.quote(DOCUMENT_DELIMITER);

    private static DecimalFormatSymbols eloDecSymbols = new DecimalFormatSymbols();
    private static DecimalFormat decimalFormat;
    static {
        eloDecSymbols.setDecimalSeparator(',');
        decimalFormat = new DecimalFormat("##0.###", eloDecSymbols);
    }

    public static FileUtils getFileUtilsElo() {
        return fileUtilsElo;
    }

    public static FileUtils getFileUtilsCmis() {
        return fileUtilsCmis;
    }

    public static BaseTypeId getBaseTypeId(Sord sord) {
        //sord is root folder
        if (sord.getType() == SordC.LBT_ARCHIVE) {
            return BaseTypeId.CMIS_FOLDER;
        }
        //sord is folder
        if (0 < sord.getType() && sord.getType() < SordC.LBT_DOCUMENT) {
            return BaseTypeId.CMIS_FOLDER;
        }
        //sord is document
        if (SordC.LBT_DOCUMENT <= sord.getType() && sord.getType() <= SordC.LBT_DOCUMENT_MAX) {
            return BaseTypeId.CMIS_DOCUMENT;
        }
        return null;
    }

    public static boolean isRootFolder(Sord sord) {
        return (sord.getType() == SordC.LBT_ARCHIVE);
    }

    public static boolean isRootFolder(int id) {
        return (id == FOLDER_ROOT_ID_ELO);
    }

    public static boolean isValidName(String name) {
        return fileUtilsCmis.isValidName(name) && fileUtilsElo.isValidName(name);
    }

    public static boolean existsChildSord(Sord parentSord, String name, IXConnection ixConnection) {
        boolean exists = false;
        String ckeckPath = EloCmisUtils.getPathElo(parentSord, name);
        try {
            Sord checkSord = ixConnection.ix().checkoutSord(ckeckPath, SordC.mbAll, LockC.NO);
            exists = true;
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code != 5023) //cale incorecta
            {
                throw new CmisRuntimeException(e.getMessage(), e);
            }
        }
        return exists;
    }

    public static List<Sord> findSords(FindInfo findInfo, BigInteger maxItems, BigInteger skipCount, IXConnection ixConnection) {
        final List<Sord> sordList = new ArrayList<Sord>();

        // skip and max
        int added = 0;
        int skipped = 0;
        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) {
            skip = 0;
        }
        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }

        FindResult findResult;
        try {
            findResult = ixConnection.ix().findFirstSords(findInfo, NUMBER_OF_RESULTS, SordC.mbAll);
            List<Sord> sordPartialList = Arrays.asList(findResult.getSords());
            if (skip == 0 && max == Integer.MAX_VALUE) {
                sordList.addAll(sordPartialList);
            } else {
                int toSkip = 0;
                int toAdd = 0;
                if (skip > 0) {
                    toSkip = Math.min(sordPartialList.size(), skip - skipped);
                }
                toAdd = Math.min(sordPartialList.size() - toSkip, max - added);
                sordList.addAll(sordPartialList.subList(toSkip, toSkip + toAdd));
                skipped += toSkip;
                added += toAdd;
            }
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        int count = 1;
        while (findResult.isMoreResults() && (added < max)) {
            List<Sord> sordPartialList;
            try {
                findResult = ixConnection.ix().findNextSords(findResult.getSearchId(), count * NUMBER_OF_RESULTS, NUMBER_OF_RESULTS, SordC.mbAll);
                sordPartialList = Arrays.asList(findResult.getSords());
            } catch (RemoteException e) {
                throw new CmisRuntimeException(e.getMessage(), e);
            }
            count++;
            if (skip == 0 && max == Integer.MAX_VALUE) {
                sordList.addAll(sordPartialList);
            } else {
                int toSkip = 0;
                int toAdd = 0;
                if (skip > 0) {
                    toSkip = Math.min(sordPartialList.size(), skip - skipped);
                }
                toAdd = Math.min(sordPartialList.size() - toSkip, max - added);
                sordList.addAll(sordPartialList.subList(toSkip, toSkip + toAdd));
                skipped += toSkip;
                added += toAdd;
            }
        }
        try {
            ixConnection.ix().findClose(findResult.getSearchId());
        } catch (RemoteException e) {
            findResult = null;
        }

        return sordList;
    }

    public static String getPathElo(Sord sord) {
        return fileUtilsElo.concatenate(fileUtilsElo.getNormalizedPathName(sord.getRefPaths()[0].getPathAsString()), EloCmisUtils.isRootFolder(sord) ? "" : sord.getName());
//        String eloPath = sord.getRefPaths()[0].getPathAsString() + (EloCmisUtils.isRootFolder(sord) ? "" : (EloCmisUtils.isRootFolder(sord.getParentId()) ? "" : EloCmisUtils.FOLDER_DELIMITER_ELO) + sord.getName());
//        eloPath = convertEloPath2CmisPath(eloPath);
//        eloPath = convertCmisPath2EloPath(eloPath);
//        return eloPath;
    }

    public static String getPathElo(Sord parentSord, String name) {
        return fileUtilsElo.getNormalizedPathName(fileUtilsElo.concatenate(getPathElo(parentSord), name));
//        String eloPath = parentSord.getRefPaths()[0].getPathAsString() + (EloCmisUtils.isRootFolder(parentSord) ? "" : (EloCmisUtils.isRootFolder(parentSord.getParentId()) ? "" : EloCmisUtils.FOLDER_DELIMITER_ELO) + parentSord.getName());
//        eloPath = eloPath + (EloCmisUtils.isRootFolder(parentSord.getId()) ? "" : EloCmisUtils.FOLDER_DELIMITER_ELO) + name;
//        return convertCmisPath2EloPath(convertEloPath2CmisPath(eloPath));
    }

    public static String convertCmisPath2EloPath(String cmisPath) {
        return fileUtilsCmis.convertPathName(cmisPath, fileUtilsElo);
//        String eloPath;
//        if (!cmisPath.substring(0, 1).equalsIgnoreCase(EloCmisUtils.FOLDER_DELIMITER_CMIS)) {
//            throw new CmisInvalidArgumentException("Path does not start with " + EloCmisUtils.FOLDER_DELIMITER_CMIS);
//        }
//        eloPath = EloCmisUtils.FOLDER_ROOT_ELO + EloCmisUtils.FOLDER_DELIMITER_ELO + cmisPath.substring(1).replace(EloCmisUtils.FOLDER_DELIMITER_CMIS, EloCmisUtils.FOLDER_DELIMITER_ELO);
//        if (eloPath.endsWith(EloCmisUtils.FOLDER_DELIMITER_ELO)) {
//            eloPath = eloPath.substring(0, eloPath.length() - EloCmisUtils.FOLDER_DELIMITER_ELO.length());
//        }
//        return eloPath;
    }

    public static String convertEloPath2CmisPath(String eloPath) {
        return fileUtilsElo.convertPathName(eloPath, fileUtilsCmis);
//        String cmisPath;
//        // remove FOLDER_ROOT_ELO prefix
//        if (eloPath.startsWith(EloCmisUtils.FOLDER_ROOT_ELO)) {
//            eloPath = eloPath.substring(EloCmisUtils.FOLDER_ROOT_ELO.length());
//        }
//        if (eloPath.length() == 0) {
//            eloPath = FOLDER_DELIMITER_ELO;
//        }
//        if (!eloPath.startsWith(EloCmisUtils.FOLDER_DELIMITER_ELO)) {
//            throw new CmisInvalidArgumentException("Path does not start with " + EloCmisUtils.FOLDER_DELIMITER_ELO);
//        }
//        cmisPath = eloPath.replace(EloCmisUtils.FOLDER_DELIMITER_ELO, EloCmisUtils.FOLDER_DELIMITER_CMIS);
//        if (cmisPath.endsWith(EloCmisUtils.FOLDER_DELIMITER_CMIS) && !cmisPath.equalsIgnoreCase(EloCmisUtils.FOLDER_DELIMITER_CMIS)) {
//            cmisPath = cmisPath.substring(0, cmisPath.length() - EloCmisUtils.FOLDER_DELIMITER_CMIS.length());
//        }
//        return cmisPath;
    }

    public static boolean isMajorVersion(String version) {
        return version != null && version.equals(getMajorVersion(version));
    }

    public static String getMajorVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return VERSION_INITIAL;
        }
        String[] versionItems = version.split("\\" + VERSION_DELIMITER);
        if (versionItems.length > 2) {
            throw new RuntimeException("Incorrect version number");
        }
        int majorVersion;
        int minorVersion = 0;
        try {
            majorVersion = Integer.parseInt(versionItems[0]);
        } catch (NumberFormatException e) {
            return VERSION_INITIAL;
            //throw new RuntimeException("Incorrect version major number", e);
        }
        return "" + majorVersion + VERSION_DELIMITER + minorVersion;
    }

    public static String getNextVersion(String version, VersioningState versioningState) {
        String nextVersion = null;
        if (VersioningState.NONE.equals(versioningState)) {
            return nextVersion;
        }
        if (version == null || version.trim().length() == 0) {
            version = VERSION_INITIAL;
        }
        String[] versionItems = version.split("\\" + VERSION_DELIMITER);
        if (versionItems.length != 2) {
            version = VERSION_INITIAL;
            versionItems = version.split("\\" + VERSION_DELIMITER);
            //throw new RuntimeException("Incorrect version number");
        }

        int majorVersion;
        int minorVersion;
        try {
            majorVersion = Integer.parseInt(versionItems[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Incorrect version major number", e);
        }
        try {
            minorVersion = Integer.parseInt(versionItems[1]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Incorrect version minor number", e);
        }

        if (VersioningState.MAJOR.equals(versioningState)) {
            majorVersion++;
            minorVersion = 0;
        } else if (VersioningState.MINOR.equals(versioningState)) {
            //keep major version
            minorVersion++;
        }
        nextVersion = "" + majorVersion + VERSION_DELIMITER + minorVersion;
        return nextVersion;
    }

    public static String getIdParts(String objectId, int partId, String defaultValue) {
        String partValue = defaultValue;
        String[] idParts = objectId.split(DOCUMENT_DELIMITER_PATTERN);
        if (idParts.length <= partId) {
            partValue = defaultValue;
        } else {
            partValue = idParts[partId];
            if (partValue.isEmpty()) {
                partValue = defaultValue;
            }
        }
        return partValue;
    }

    public static String getSordId(String objectId) {
        return getIdParts(objectId, 0, null);
    }

    public static String getDocumentId(String objectId) {
        return getIdParts(objectId, 1, DOCUMENT_ID_MISSING);
    }

    public static String calcObjectId(String... ids) {
        return StringUtils.join(ids, DOCUMENT_DELIMITER);
    }

    public static String calcObjectId(Object... objects) {
        EditInfo editInfo = null;
        Sord sord = null;
        DocVersion docVersion = null;

        for (Object object : objects) {
            if (object instanceof EditInfo) {
                editInfo = (EditInfo) object;
                sord = editInfo.getSord();
            } else if (object instanceof Sord) {
                editInfo = null;
                sord = (Sord) object;
            } else if (object instanceof DocVersion) {
                docVersion = (DocVersion) object;
            }
        }
        if (docVersion == null) {
            docVersion = sord.getDocVersion();
        }
        if (sord == null) {
            throw new RuntimeException("Sord must be provided.");
        }
//        if (docVersion == null) {
//            throw new RuntimeException("DocVersion must be provided.");
//        }
        return sord.getId() + ((docVersion != null) ? DOCUMENT_DELIMITER + docVersion.getId() : "");
    }


    public static Number getEloNumberValueAsDecimal(String eloNumberVal) {
//        DecimalFormatSymbols eloDecSymbols = new DecimalFormatSymbols();
//        String eloDecPattern = "##0.###";
//        eloDecSymbols.setDecimalSeparator(',');
//        DecimalFormat decimalFormat = new DecimalFormat("##0.###", eloDecSymbols);
//        System.out.println(decimalFormat.parse("178978,99"));
//        System.out.println(decimalFormat.format(178978.99));
        Number number = null;
        try {
            number = decimalFormat.parse(eloNumberVal);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return number;
    }

}
