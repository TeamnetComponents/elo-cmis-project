package org.elo.jmeter;

import de.elo.extension.connection.EloUtilsConnection;
import de.elo.extension.service.EloUtilsService;
import de.elo.ix.client.IXConnection;
import de.elo.ix.client.Sord;
import de.elo.utils.net.RemoteException;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.platform.common.utils.file.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 4/22/2015.
 */
public class JMeterFileUtils {

    public static List<String> listFileNames(String filePath, IXConnection ixConnection) {
        List<String> result = new ArrayList<String>();
        if (ixConnection == null) {
            Collection<File> fileCollection = org.apache.commons.io.FileUtils.listFiles(new File(filePath), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            for (File file : fileCollection) {
                result.add(file.getAbsolutePath());
            }
            fileCollection.clear();
        } else {
            EloUtilsService eloUtilsService = new EloUtilsService();
            String eloFilePath = EloUtilsService.fileUtilsRegular.convertPathName(filePath, EloUtilsService.fileUtilsElo);
            try {
                List<Sord> childList = eloUtilsService.listFolderContent(ixConnection, eloFilePath);
                for (Sord sord : childList) {
                    if (EloUtilsService.isDocument(sord)){
                        String childFilePath;
                        //childFilePath = EloCmisUtils.convertEloPath2CmisPath(EloUtilsService.getPathElo(sord /*, sord.getName()*/));
                        childFilePath = EloUtilsService.getPathElo(sord /*, sord.getName()*/);
                        result.add(childFilePath);
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static void listFileNames(List<String> listFileNames, String fileDestination) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        String eol = System.getProperty("line.separator");
        FileWriter fw = new FileWriter(fileDestination, false);
        for (String fileName : listFileNames) {
            fw.write(fileName + eol);
        }
        fw.close();
    }

    public static void main(String[] args) throws Exception {


        String eloConnectionFileName;
        String sourceFolder;
        String listFileName;


        listFileName = "C:\\__JAVA\\jmeter\\test\\elo\\fileList.csv";

        //list ELO
        eloConnectionFileName = "C:\\__JAVA\\jmeter\\test\\elo\\elo-eloboamvip.properties";
        sourceFolder = "/JMeter/upload_5KB_100fire_1VU/";


        //list LOCAL
        //eloConnectionFileName = "";
        //sourceFolder = "C:\\__JAVA\\jmeter\\test\\dmsutils";


        IXConnection ixConnection = null;
        if (StringUtils.isNotEmpty(eloConnectionFileName)) {
            Properties generalProperties = FileUtils.openOsResource(eloConnectionFileName);
            ixConnection = EloUtilsConnection.createIXConnection(generalProperties);
        }

        listFileNames(listFileNames(sourceFolder, ixConnection), listFileName);
        EloUtilsConnection.destroyConnection(ixConnection);
    }

}
