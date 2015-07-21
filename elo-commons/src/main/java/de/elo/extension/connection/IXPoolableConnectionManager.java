package de.elo.extension.connection;

import de.elo.ix.client.IXConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 6/4/2015.
 */
public final class IXPoolableConnectionManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IXPoolableConnectionManager.class);

    private final Properties properties;
    private IXConnectionKeyedObjectPoolFactory ixConnectionKeyedObjectPoolFactory;
    private IXConnectionKeyedObjectPool[] ixConnectionKeyedObjectPools;


    public IXPoolableConnectionManager(Map properties) {
        this.properties = new Properties();
        //filter only connection relevant properties
        for (Object propertyName : properties.keySet()) {
            if ((String.valueOf(propertyName)).startsWith(EloUtilsConnection.ELO_CONNECTION_PARAMETER_PREFIX)) {
                this.properties.setProperty(String.valueOf(propertyName), String.valueOf(properties.get(propertyName)));
            }
        }
    }

    public IXPoolableConnectionManager(Properties properties) {
        this.properties = new Properties();
        //filter only connection relevant properties
        for (Object propertyName : properties.keySet()) {
            if (((String) propertyName).startsWith(EloUtilsConnection.ELO_CONNECTION_PARAMETER_PREFIX)) {
                this.properties.setProperty((String) propertyName, properties.getProperty((String) propertyName));
            }
        }
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

    private synchronized IXConnectionKeyedObjectPoolFactory getIXConnectionKeyedObjectPoolFactory() {
        if (this.ixConnectionKeyedObjectPoolFactory == null) {
            this.ixConnectionKeyedObjectPoolFactory = new IXConnectionKeyedObjectPoolFactory(this);
        }
        return this.ixConnectionKeyedObjectPoolFactory;
    }

    private IXConnectionKeyedObjectPool[] getIXConnectionKeyedObjectPools() {
        //create connection pools on the first request
        if (this.ixConnectionKeyedObjectPools == null) {
            synchronized (IXConnectionKeyedObjectPool.class) {
                if (this.ixConnectionKeyedObjectPools == null) {
                    this.ixConnectionKeyedObjectPools = this.getIXConnectionKeyedObjectPoolFactory().createConnectionPools();
                }
            }
        }
        return this.ixConnectionKeyedObjectPools;
    }

    private int getPoolIndex(IXConnectionKey ixConnectionKey) {
        for (int index = 0; index < getIXConnectionKeyedObjectPools().length; index++) {
            IXConnectionKeyedObjectPool ixConnectionKeyedObjectPool = getIXConnectionKeyedObjectPools()[index];
            if (ixConnectionKeyedObjectPool.allow(ixConnectionKey)) {
                return index;
            }
        }
        throw new RuntimeException("No connection pool is compatible with provided ELO connection details.");
    }

    /*default*/ IXConnection borrowObject(IXConnectionKey ixConnectionKey) throws Exception {
        IXConnection ixConnection;
        if (this.getIXConnectionKeyedObjectPoolFactory().getPoolCount() == 0) {
            ixConnection = this.getIXConnectionKeyedObjectPoolFactory().makeObject(ixConnectionKey);
        } else {
            int index = getPoolIndex(ixConnectionKey);
            IXConnectionKeyedObjectPool ixConnectionKeyedObjectPool = getIXConnectionKeyedObjectPools()[index];
            ixConnection = ixConnectionKeyedObjectPool.borrowObject(ixConnectionKey);

        }
        return ixConnection;
    }

    /*default*/ void returnObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) throws Exception {
        if (this.getIXConnectionKeyedObjectPoolFactory().getPoolCount() == 0) {
            this.getIXConnectionKeyedObjectPoolFactory().destroyObject(ixConnectionKey, ixConnection);
        } else {
            int index = getPoolIndex(ixConnectionKey);
            IXConnectionKeyedObjectPool ixConnectionKeyedObjectPool = getIXConnectionKeyedObjectPools()[index];
            ixConnectionKeyedObjectPool.returnObject(ixConnectionKey, ixConnection);
        }
    }

    /*default*/ void invalidateObject(IXConnectionKey ixConnectionKey, IXConnection ixConnection) throws Exception {
        if (this.getIXConnectionKeyedObjectPoolFactory().getPoolCount() == 0) {
            this.getIXConnectionKeyedObjectPoolFactory().destroyObject(ixConnectionKey, ixConnection);
        } else {
            int index = getPoolIndex(ixConnectionKey);
            IXConnectionKeyedObjectPool ixConnectionKeyedObjectPool = getIXConnectionKeyedObjectPools()[index];
            ixConnectionKeyedObjectPool.invalidateObject(ixConnectionKey, ixConnection);
        }
    }

    Properties getProperties() {
        return properties;
    }

    public IXPoolableConnection retrieveConnection(IXConnectionKey ixConnectionKey) throws Exception {
        return new IXPoolableConnection(this, ixConnectionKey);
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.ixConnectionKeyedObjectPoolFactory != null) {
            this.properties.clear();
            for (int index = 0; index < this.ixConnectionKeyedObjectPools.length; index++) {
                this.ixConnectionKeyedObjectPools[index].close();
                this.ixConnectionKeyedObjectPools[index] = null;
            }
            this.ixConnectionKeyedObjectPoolFactory = null;
        }
    }
}
