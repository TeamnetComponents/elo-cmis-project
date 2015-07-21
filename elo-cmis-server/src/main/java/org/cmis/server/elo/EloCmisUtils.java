package org.cmis.server.elo;

import de.elo.extension.service.EloUtilsService;
import de.elo.ix.client.IXConnection;
import de.elo.ix.client.Sord;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.platform.common.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

/**
 * Created by Lucian.Dragomir on 6/9/2014.
 */
public class EloCmisUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisUtils.class);

    //versioning constants
    private static final String VERSION_INITIAL = "0.0";
    private static final String VERSION_DELIMITER = ".";

    //FileUtils constants
    private static final FileUtils FILE_UTILS_ELO = EloUtilsService.fileUtilsElo;
    private static final FileUtils FILE_UTILS_CMIS = EloUtilsService.fileUtilsRegular;

    public static FileUtils getFileUtilsElo() {
        return FILE_UTILS_ELO;
    }

    public static FileUtils getFileUtilsCmis() {
        return FILE_UTILS_CMIS;
    }



    public static boolean isValidName(String name) {
        return FILE_UTILS_CMIS.isValidName(name) && FILE_UTILS_ELO.isValidName(name);
    }

    public static boolean existsChildSord(Sord parentSord, String name, IXConnection ixConnection) {
        try {
            return EloUtilsService.existsChildSord(ixConnection, parentSord, name);
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    public static String convertCmisPath2EloPath(String cmisPath) {
        return FILE_UTILS_CMIS.convertPathName(cmisPath, FILE_UTILS_ELO);
    }

    public static String convertEloPath2CmisPath(String eloPath) {
        return FILE_UTILS_ELO.convertPathName(eloPath, FILE_UTILS_CMIS);
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


}
