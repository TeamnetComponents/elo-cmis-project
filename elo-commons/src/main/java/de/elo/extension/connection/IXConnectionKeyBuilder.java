package de.elo.extension.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lucian.Dragomir on 6/4/2015.
 */
public final class IXConnectionKeyBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(IXPoolableConnectionManager.class);

    private Map<String, String> properties;

    public IXConnectionKeyBuilder() {
        this.properties = new HashMap<String, String>();
    }

    public IXConnectionKeyBuilder setDefaultCredentials() {
        clearAuthenticationInfo();
        return this;
    }

    public IXConnectionKeyBuilder setBasicCredentials(String user, String password) {
        clearAuthenticationInfo();
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloAuthenticationType.BASIC.name());
        this.properties.put(EloUtilsConnection.USER, user);
        this.properties.put(EloUtilsConnection.PASSWORD, password);
        return this;
    }

    public IXConnectionKeyBuilder setBasicAsCredentials(String user, String password, String userAs) {
        clearAuthenticationInfo();
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloAuthenticationType.BASIC_AS.name());
        this.properties.put(EloUtilsConnection.USER, user);
        this.properties.put(EloUtilsConnection.PASSWORD, password);
        this.properties.put(EloUtilsConnection.USER_AS, userAs);
        return this;
    }

    public IXConnectionKeyBuilder setBasicAsCredentials(String userAs) {
        clearAuthenticationInfo();
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloAuthenticationType.BASIC_AS.name());
        this.properties.remove(EloUtilsConnection.USER);
        this.properties.remove(EloUtilsConnection.PASSWORD);
        this.properties.put(EloUtilsConnection.USER_AS, userAs);
        return this;
    }

    public IXConnectionKeyBuilder setTicketCredentials(String ticket) {
        clearAuthenticationInfo();
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloAuthenticationType.TICKET.name());
        this.properties.put(EloUtilsConnection.TICKET, ticket);
        return this;
    }

    public IXConnectionKeyBuilder setKerberosCredentials(String realm, String kdc, String servicePrincipal) {
        clearAuthenticationInfo();
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloAuthenticationType.KERBEROS.name());
        this.properties.put(EloUtilsConnection.KERBEROS_REALM, realm);
        this.properties.put(EloUtilsConnection.KERBEROS_KDC, kdc);
        this.properties.put(EloUtilsConnection.KERBEROS_PRINCIPAL, servicePrincipal);
        return this;
    }

    public IXConnectionKeyBuilder setLocale(String country, String language, String timezone) {
        this.properties.put(EloUtilsConnection.ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY, country);
        this.properties.put(EloUtilsConnection.ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE, language);
        this.properties.put(EloUtilsConnection.ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE, timezone);
        return this;
    }

    private void clearAuthenticationInfo() {
        for (String propertyName : this.properties.keySet()) {
            if (propertyName.startsWith(EloUtilsConnection.ELO_CONNECTION_PARAMETER_PREFIX + EloUtilsConnection.ELO_CONNECTION_AUTHENTICATION_PREFIX)) {
                this.properties.remove(propertyName);
            }
        }
        this.properties.put(EloUtilsConnection.AUTHENTICATION_TYPE, EloUtilsConnection.AUTHENTICATION_TYPE_SERVER_APPLICATION);
    }

    public IXConnectionKey build() {
        return new IXConnectionKey(this.properties);
    }
}
