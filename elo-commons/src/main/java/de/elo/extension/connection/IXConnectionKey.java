package de.elo.extension.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lucian.Dragomir on 6/3/2015.
 */
public final class IXConnectionKey {
    private static final Logger LOG = LoggerFactory.getLogger(IXPoolableConnectionManager.class);

    private final Map<String, String> properties;

    public IXConnectionKey() {
        this(new HashMap<String, String>());
        properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloUtilsConnection.AUTHENTICATION_TYPE_SERVER_APPLICATION);
    }

    public IXConnectionKey(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || (!(obj instanceof IXConnectionKey))) {
            return false;
        }
        if (this.properties == null || ((IXConnectionKey) obj).properties == null) {
            return false;
        }
        return (this.properties.hashCode() == ((IXConnectionKey) obj).properties.hashCode());
    }

    @Override
    public int hashCode() {
        return this.properties.hashCode();
    }

}
