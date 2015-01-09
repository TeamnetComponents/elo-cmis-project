package org.cmis.server.elo;

import de.elo.ix.client.IXConnFactory;
import de.elo.ix.client.IXConnection;
import de.elo.utils.net.RemoteException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.MutableCallContext;
import org.cmis.server.elo.connection.EloConnectionDetails;
import org.cmis.server.elo.connection.EloConnectionPools;
import org.cmis.util.CmisServiceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 6/5/2014.
 */
public class EloCmisConnectionManager {

    //ELO CONNECTION CONTEXT
    public static final String ELO_CONNECTION_CONTEXT = "elo.connection";

    private static final Logger LOG = LoggerFactory.getLogger(EloCmisConnectionManager.class);

    private CmisServiceParameters cmisServiceParameters;
    //private IXConnFactory ixConnFactory;
    private Properties applicationInfo;
    private EloConnectionPools eloConnectionPools;
    private EloConnectionDetails defaultEloConnectionDetails;

    public EloCmisConnectionManager(CmisServiceParameters cmisServiceParameters) {
        this.cmisServiceParameters = cmisServiceParameters;
        this.applicationInfo = getApplicationInfo();
        this.eloConnectionPools = new EloConnectionPools(this);
        //this.ixConnFactory = null;
        this.defaultEloConnectionDetails = new EloConnectionDetails(this);
    }

    public String getDefaultApplicationName() {
        return applicationInfo.getProperty("finalName", "");
    }

    public String getDefaultApplicationVersion() {
        return applicationInfo.getProperty("version", "");
    }

    public IXConnFactory getIxConnFactory() throws RemoteException {
//        if (this.ixConnFactory == null) {
//            this.ixConnFactory.
//            try {
//                this.ixConnFactory = (new EloConnectionDetails(this)).createIXConnFactory();
//            } catch (IOException e) {
//                throw new IllegalStateException(e);
//            }
//        }
        return defaultEloConnectionDetails.createIXConnFactory();
    }


//    private void setIxConnFactory(IXConnFactory ixConnFactory) {
//        this.ixConnFactory = ixConnFactory;
//    }

    public CmisServiceParameters getCmisServiceParameters() {
        return this.cmisServiceParameters;
    }

    public void destroy() {
        //TO BE implement cleanup of ELO Connections
    }

    private Properties getApplicationInfo() {
        Properties properties = new Properties();

        String path = "/application.info";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            try {
                properties.load(stream);
                stream.close();
            } catch (IOException e) {
            }
        }
        return properties;
    }

    //throw new CmisPermissionDeniedException("Unable to create connection to ELO Server.", e);


    public IXConnection getConnection(CallContext callContext) {
        IXConnection ixConnection = null;
        EloConnectionDetails eloConnectionDetails = new EloConnectionDetails(this, callContext);
        if (callContext != null) {
            ixConnection = (IXConnection) callContext.get(ELO_CONNECTION_CONTEXT);
            if (ixConnection != null) {
                if (true || eloConnectionDetails.isValidConnection(ixConnection)) {
                    return ixConnection;
                } else {
                    MutableCallContext mutableCallContext = (MutableCallContext) callContext;
                    mutableCallContext.remove(ELO_CONNECTION_CONTEXT);
                    try {
                        this.eloConnectionPools.invalidateObject(eloConnectionDetails, ixConnection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ixConnection = null;
                }
            }
        }
        if (ixConnection == null) {
            try {
                ixConnection = this.eloConnectionPools.borrowObject(eloConnectionDetails);
                if (callContext != null) {
                    MutableCallContext mutableCallContext = (MutableCallContext) callContext;
                    mutableCallContext.put(ELO_CONNECTION_CONTEXT, ixConnection);
                }
            } catch (Exception e) {
                throw new CmisPermissionDeniedException("Unable to create connection to ELO Server.", e);
            }
        }
        return ixConnection;
    }

    public boolean returnConnection(CallContext callContext) {
        boolean returned = false;
        IXConnection ixConnection = null;
        EloConnectionDetails eloConnectionDetails = new EloConnectionDetails(this, callContext);
        ixConnection = (IXConnection) callContext.get(ELO_CONNECTION_CONTEXT);
        if (ixConnection != null) {
            //return connection back to the pool
            try {
                this.eloConnectionPools.returnObject(eloConnectionDetails, ixConnection);
                returned = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            //update call context accordingly
            MutableCallContext mutableCallContext = (MutableCallContext) callContext;
            mutableCallContext.remove(ELO_CONNECTION_CONTEXT);
        }
        return returned;
    }

    public IXConnection getAdminConnection() {
        return getConnection(null);
    }

    public boolean returnAdminConnection(IXConnection ixConnection) {
        boolean returned = false;
        EloConnectionDetails eloConnectionDetails = new EloConnectionDetails(this, null);
        //return connection back to the pool
        try {
            this.eloConnectionPools.returnObject(eloConnectionDetails, ixConnection);
            returned = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returned;
    }
}
