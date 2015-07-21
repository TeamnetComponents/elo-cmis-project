package org.cmis.server.elo;

import de.elo.extension.connection.IXConnectionKey;
import de.elo.extension.connection.IXConnectionKeyBuilder;
import de.elo.extension.connection.IXPoolableConnection;
import de.elo.extension.connection.IXPoolableConnectionManager;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.MutableCallContext;
import org.cmis.server.elo.commons.EloCmisAuthenticationType;
import org.cmis.server.elo.commons.EloCmisContextParameter;
import org.cmis.util.CmisServiceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by Lucian.Dragomir on 6/5/2014.
 */
public class EloCmisConnectionManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EloCmisConnectionManager.class);
    private static final String ELO_CONNECTION_CONTEXT = "elo.connection";

    private CmisServiceParameters cmisServiceParameters;
    private IXPoolableConnectionManager ixPoolableConnectionManager;

    public EloCmisConnectionManager(CmisServiceParameters cmisServiceParameters) {
        this.cmisServiceParameters = cmisServiceParameters;
        this.ixPoolableConnectionManager = new IXPoolableConnectionManager(cmisServiceParameters.getParameters());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }

    @Override
    public void close() throws Exception {
        if (this.ixPoolableConnectionManager != null) {
            this.ixPoolableConnectionManager.close();
            this.ixPoolableConnectionManager = null;
            ;
        }
    }

    public CmisServiceParameters getCmisServiceParameters() {
        return this.cmisServiceParameters;
    }


    public IXConnectionKey createIXConnectionKey(CallContext callContext) {
        IXConnectionKey ixConnectionKey;
        if (callContext == null) {
            ixConnectionKey = new IXConnectionKeyBuilder().setDefaultCredentials().build();
        } else {
            String authenticationType = (String) callContext.get(EloCmisContextParameter.AUTHENTICATION_TYPE);
            if (authenticationType == null) {
                authenticationType = EloCmisAuthenticationType.BASIC.name();
            }
            IXConnectionKeyBuilder ixConnectionKeyBuilder = new IXConnectionKeyBuilder();

            if (authenticationType.equals(EloCmisAuthenticationType.BASIC.name())) {
                ixConnectionKeyBuilder.setBasicCredentials(callContext.getUsername(), callContext.getPassword());
            } else if (authenticationType.equals(EloCmisAuthenticationType.BASIC_AS.name())) {
                if (callContext.getUsername() != null && callContext.getUsername().length() > 0) {
                    ixConnectionKeyBuilder.setBasicAsCredentials(callContext.getUsername(), callContext.getPassword(), (String) callContext.get(EloCmisContextParameter.USER_AS));
                } else {
                    ixConnectionKeyBuilder.setBasicAsCredentials((String) callContext.get(EloCmisContextParameter.USER_AS));
                }
            } else if (authenticationType.equals(EloCmisAuthenticationType.KERBEROS.name())) {
                ixConnectionKeyBuilder.setKerberosCredentials((String) callContext.get(EloCmisContextParameter.KERBEROS_REALM), (String) callContext.get(EloCmisContextParameter.KERBEROS_KDC), (String) callContext.get(EloCmisContextParameter.KERBEROS_PRINCIPAL));

            } else if (authenticationType.equals(EloCmisAuthenticationType.KERBEROS.name())) {
                ixConnectionKeyBuilder.setTicketCredentials((String) callContext.get(EloCmisContextParameter.TICKET));
            } else {
                //force authentication error
                ixConnectionKeyBuilder = null;
            }

            //de uitat si la client locale

            ixConnectionKey = ixConnectionKeyBuilder.build();
        }

        return ixConnectionKey;
    }


    public IXPoolableConnection getConnection(CallContext callContext) throws CmisConnectionException {
        IXPoolableConnection ixPoolableConnection = null;
        IXConnectionKey ixConnectionKey = createIXConnectionKey(callContext);
        if (callContext != null) {
            ixPoolableConnection = (IXPoolableConnection) callContext.get(ELO_CONNECTION_CONTEXT);
            if (ixPoolableConnection != null) {
                if (ixPoolableConnection.hasIXConnectionKey(ixConnectionKey) && ixPoolableConnection.isValid()) {
                    return ixPoolableConnection;
                } else {
                    MutableCallContext mutableCallContext = (MutableCallContext) callContext;
                    mutableCallContext.remove(ELO_CONNECTION_CONTEXT);
                    try {
                        ixPoolableConnection.close(true);
                    } catch (Exception e) {
                        throw new CmisConnectionException("Unable to get connection to ", e);
                    }
                    ixPoolableConnection = null;
                }
            }
        }
        if (ixPoolableConnection == null) {
            try {
                ixPoolableConnection = this.ixPoolableConnectionManager.retrieveConnection(ixConnectionKey);
                if (callContext != null) {
                    MutableCallContext mutableCallContext = (MutableCallContext) callContext;
                    mutableCallContext.put(ELO_CONNECTION_CONTEXT, ixPoolableConnection);
                }
            } catch (Exception e) {
                throw new CmisPermissionDeniedException("Unable to create connection to ELO Server.", e);
            }
        }
        return ixPoolableConnection;
    }

//    public boolean returnConnection(CallContext callContext) {
//        boolean returned = false;
//        IXPoolableConnection ixPoolableConnection = (IXPoolableConnection) callContext.get(ELO_CONNECTION_CONTEXT);
//        if (ixPoolableConnection != null) {
//            //return connection back to the pool
//            try {
//                ixPoolableConnection.close();
//                returned = true;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            //update call context accordingly
//            MutableCallContext mutableCallContext = (MutableCallContext) callContext;
//            mutableCallContext.remove(ELO_CONNECTION_CONTEXT);
//        }
//        return returned;
//    }

//    public IXConnection getAdminConnection() throws Exception {
//        IXConnectionKey ixConnectionKey = createIXConnectionKey(null);
//        IXPoolableConnection ixPoolableConnection = this.ixPoolableConnectionManager.retrieveConnection(ixConnectionKey);
//        return ixPoolableConnection.getIxConnection();
//    }

//    public boolean returnAdminConnection(IXConnection ixConnection) {
//        boolean returned = false;
//        IXConnection ixConnection1;
//        EloConnectionDetails eloConnectionDetails = new EloConnectionDetails(this, null);
//        //return connection back to the pool
//        try {
//            this.eloConnectionPools.returnObject(eloConnectionDetails, ixConnection);
//            returned = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return returned;
//    }

}
