package de.elo.extension.connection;

import de.elo.ix.client.IXConnection;
import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lucian.Dragomir on 6/4/2015.
 */
public final class IXConnectionKeyedObjectPoolFactory extends BaseKeyedPoolableObjectFactory<IXConnectionKey, IXConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(IXConnectionKeyedObjectPoolFactory.class);
    private static final String POOL_COUNT = EloUtilsConnection.ELO_CONNECTION_PARAMETER_PREFIX + "pool.count";

    private IXPoolableConnectionManager IXPoolableConnectionManager;

    public IXConnectionKeyedObjectPoolFactory(IXPoolableConnectionManager IXPoolableConnectionManager) {
        this.IXPoolableConnectionManager = IXPoolableConnectionManager;
    }

    public IXPoolableConnectionManager getIXPoolableConnectionManager() {
        return this.IXPoolableConnectionManager;
    }

    public int getPoolCount() {
        return Integer.parseInt(this.IXPoolableConnectionManager.getProperties().getProperty(POOL_COUNT, "0"));
    }

    public IXConnectionKeyedObjectPool[] createConnectionPools() {
        IXConnectionKeyedObjectPool[] eloConnectionPools = new IXConnectionKeyedObjectPool[getPoolCount()];
        for (int index = 0; index < getPoolCount(); index++) {
            eloConnectionPools[index] = new IXConnectionKeyedObjectPool(this, index);
        }
        return eloConnectionPools;
    }

    @Override
    public IXConnection makeObject(IXConnectionKey ixConnectionKey) throws Exception {
        IXConnection ixConnection;
        try {
            ixConnection = EloUtilsConnection.createIXConnection(ixConnectionKey.getProperties(), this.IXPoolableConnectionManager.getProperties());
            if (LOG.isDebugEnabled()) {
                LOG.debug("makeObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode());
                System.out.println("makeObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode());
            }
        } catch (Exception e) {
            LOG.error("makeObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")", e);
            System.out.println("makeObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", Exception:" + e);
            throw e;
        }
        return ixConnection;
    }

    @Override
    public void destroyObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) throws Exception {
        String logMessage = "destroyObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode();
        try {
            EloUtilsConnection.destroyConnection(ixConnection);
            if (LOG.isDebugEnabled()) {
                LOG.debug(logMessage);
                System.out.println(logMessage);
            }
        } catch (Exception e) {
            LOG.error(logMessage, e);
            System.out.println(logMessage + ", Exception:" + e);
            throw e;
        }
    }

    @Override
    public boolean validateObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) {
        String logMessage = "validateObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode();
        boolean valid = false;
        try {
            valid = EloUtilsConnection.isValidConnection(ixConnection);
            if (LOG.isDebugEnabled()) {
                LOG.debug(logMessage);
                System.out.println(logMessage);
            }
        } catch (Exception e) {
            LOG.error(logMessage, e);
            System.out.println(logMessage + ", Exception:" + e);
            throw e;
        }
        return valid;
    }

    @Override
    public void activateObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) throws Exception {
        String logMessage = "activateObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode();
        try {
            //DO NOTHING
            //..........
            //DO NOTHING
            if (LOG.isDebugEnabled()) {
                LOG.debug(logMessage);
                System.out.println(logMessage);
            }
        } catch (Exception e) {
            LOG.error(logMessage, e);
            System.out.println(logMessage + ", Exception:" + e);
            throw e;
        }
    }

    @Override
    public void passivateObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) throws Exception {
        String logMessage = "passivateObject - IXConnectionKey:" + ixConnectionKey.hashCode() + "(" + System.identityHashCode(ixConnectionKey) + ")" + ", IXConnection:" + ixConnection.hashCode();
        try {
            //DO NOTHING
            //..........
            //DO NOTHING
            if (LOG.isDebugEnabled()) {
                LOG.debug(logMessage);
                System.out.println(logMessage);
            }
        } catch (Exception e) {
            LOG.error(logMessage, e);
            System.out.println(logMessage + ", Exception:" + e);
            throw e;
        }
    }

}
