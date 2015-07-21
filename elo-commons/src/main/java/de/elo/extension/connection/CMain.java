package de.elo.extension.connection;

import org.platform.common.utils.file.FileUtils;

import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 6/4/2015.
 */
public class CMain {
    public static void main(String[] args) throws Exception {
        String osFilePath = "C:\\__DOWNLOADS\\ELO-YMENS\\elo-cmis-server.properties";
        Properties properties = FileUtils.openOsResource(osFilePath);


        IXPoolableConnectionManager IXPoolableConnectionManager = new IXPoolableConnectionManager(properties);

        if (1 == 1) {
            //return;
        }


        System.out.println("");
        IXConnectionKey ixConnectionKey = new IXConnectionKeyBuilder().setDefaultCredentials().build();
        IXPoolableConnection IXPoolableConnection = IXPoolableConnectionManager.retrieveConnection(ixConnectionKey);

        System.out.println(IXPoolableConnection + "-" + IXPoolableConnection.getIxConnection());
        IXPoolableConnection.close();
        System.out.println(IXPoolableConnection);


        System.out.println("");
        IXPoolableConnection = IXPoolableConnectionManager.retrieveConnection(ixConnectionKey);
        System.out.println(IXPoolableConnection + "-" + IXPoolableConnection.getIxConnection());
        //poolableIXConnection.close();
        System.out.println(IXPoolableConnection);

        System.out.println("");
        IXPoolableConnection IXPoolableConnection2 = IXPoolableConnectionManager.retrieveConnection(ixConnectionKey);
        System.out.println(IXPoolableConnection2 + "-" + IXPoolableConnection2.getIxConnection());

        System.out.println("");
        IXPoolableConnection.close();
        IXPoolableConnection2.close();

        System.out.println("");
        IXPoolableConnectionManager.close();
        System.out.println("");
    }
}
