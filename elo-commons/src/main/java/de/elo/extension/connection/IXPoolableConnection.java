package de.elo.extension.connection;

import de.elo.ix.client.IXConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lucian.Dragomir on 6/3/2015.
 */
public final class IXPoolableConnection implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IXPoolableConnectionManager.class);

    private IXPoolableConnectionManager ixPoolableConnectionManager;
    private IXConnectionKey ixConnectionKey;
    private IXConnection ixConnection;

    public IXPoolableConnection(IXPoolableConnectionManager ixPoolableConnectionManager, IXConnectionKey ixConnectionKey) throws Exception {
        this.ixPoolableConnectionManager = ixPoolableConnectionManager;
        this.ixConnectionKey = ixConnectionKey;
        this.ixConnection = this.ixPoolableConnectionManager.borrowObject(this.ixConnectionKey);
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

    public IXConnection getIxConnection() {
        return this.ixConnection;
    }

    public boolean isValid() {
        return EloUtilsConnection.isValidConnection(this.getIxConnection());
    }

    public boolean hasIXConnectionKey(IXConnectionKey ixConnectionKey) {
        return this.ixConnectionKey.equals(ixConnectionKey);
    }

    public synchronized void close(boolean invalidate) throws Exception {
        if (this.ixPoolableConnectionManager != null) {
            if (invalidate) {
                this.ixPoolableConnectionManager.invalidateObject(this.ixConnectionKey, this.ixConnection);
            }
            this.ixPoolableConnectionManager.returnObject(this.ixConnectionKey, this.ixConnection);
            this.ixConnection = null;
            this.ixConnectionKey = null;
            this.ixPoolableConnectionManager = null;
        }
    }

    @Override
    public void close() throws Exception {
        close(false);
    }

}
