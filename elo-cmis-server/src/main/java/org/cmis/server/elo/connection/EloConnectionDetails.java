package org.cmis.server.elo.connection;

import de.elo.ix.client.ClientInfo;
import de.elo.ix.client.IXConnFactory;
import de.elo.ix.client.IXConnection;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.commons.lang.StringUtils;
import org.cmis.server.elo.EloCmisConnectionManager;
import org.cmis.server.elo.commons.EloCmisAuthenticationType;
import org.cmis.server.elo.commons.EloCmisContextParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;

import static org.cmis.server.elo.commons.EloCmisContextParameter.*;

/**
 * Created by Lucian.Dragomir on 12/26/2014.
 */
public class EloConnectionDetails {
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION FACTORY PROPERTIES
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION PROPERTIES OPTIONS
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_URL = "elo.connection.factory.property.url";
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS = "elo.connection.factory.property.nbOfCnns";
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS = "elo.connection.factory.property.timeoutSeconds";
    //ELO SESSION OPTIONS
    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_NAME = "elo.connection.factory.session.appName";
    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_VERSION = "elo.connection.factory.session.appVersion";
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION PROPERTIES
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION CREDENTIALS
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_USER = "elo.connection.factory.connection.appUser";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD = "elo.connection.factory.connection.appPassword";
    //ELO DEFAULT CLIENT INFO PROPERTIES
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY = "elo.connection.factory.connection.client.country";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE = "elo.connection.factory.connection.client.language";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE = "elo.connection.factory.connection.client.timeZone";


    //private static final Logger LOG = LoggerFactory.getLogger(EloConnectionDetails.class);
    private static final String AUTHENTICATION_TYPE_SERVER_APPLICATION = "SERVER_APPLICATION";

    private EloCmisConnectionManager eloCmisConnectionManager;
    private ClientInfo clientInfo;
    private Map<String, Object> detailMap;


    private EloConnectionDetails() {
        detailMap = new HashMap<>();
    }

    public EloConnectionDetails(EloCmisConnectionManager eloCmisConnectionManager) {
        this(eloCmisConnectionManager, null);
    }

    private static Logger getLogger() {
        return getLogger(EloConnectionDetails.class);
    }

    private static Logger getLogger(Class clazz) {
        return LoggerFactory.getLogger(clazz);
    }


    public EloConnectionDetails(EloCmisConnectionManager eloCmisConnectionManager, CallContext callContext) {
        //contextual connection using credentials provided when connecting to elo-cmis-server
        this();
        this.eloCmisConnectionManager = eloCmisConnectionManager;
        if (callContext == null) {
            //implicit connection using credentials defined in the application config file "repository.properties"
            detailMap.put(AUTHENTICATION_TYPE, AUTHENTICATION_TYPE_SERVER_APPLICATION);
            detailMap.put(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER, eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER, ""));
            detailMap.put(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD, eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD, ""));
        } else {
            //prepare detailMap with context details relevant for elo connections
            for (String key : EloCmisContextParameter.getEloCmisContextParameters()) {
                Object value = callContext.get(key);
                if (key != null) {
                    detailMap.put(key, value);
                }
            }
            detailMap.put(AUTHENTICATION_TYPE, EloCmisAuthenticationType.fromValue((String) callContext.get(AUTHENTICATION_TYPE)).name());
        }
        this.clientInfo = createClientInfo();
    }

    public IXConnFactory createIXConnFactory() throws de.elo.utils.net.RemoteException {
        IXConnFactory ixConnFactory = null;
        // set connection properties
        String url = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_PROPERTY_URL, "");
        int nbOfCnns = eloCmisConnectionManager.getCmisServiceParameters().getIntegerParameter(ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS, 0);
        int timeoutSeconds = eloCmisConnectionManager.getCmisServiceParameters().getIntegerParameter(ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS, 0);
        Properties connectionProperties = IXConnFactory.createConnProps(url, nbOfCnns, timeoutSeconds);

        // set session options
        String appName = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_SESSION_APP_NAME, eloCmisConnectionManager.getDefaultApplicationName());
        String appVersion = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_SESSION_APP_VERSION, eloCmisConnectionManager.getDefaultApplicationVersion());
        Properties sessionOptions = IXConnFactory.createSessionOptions(appName, appVersion);

        //create and return IXConnectionFactory object
        ixConnFactory = new IXConnFactory(connectionProperties, sessionOptions);
        return ixConnFactory;
    }

    private ClientInfo createClientInfo() {
        ClientInfo clientInfo = null;

        //get default client parameters
        String defaultCountry = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY, Locale.getDefault().getCountry());
        String defaultLanguage = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE, Locale.getDefault().getLanguage());
        String defaultTimeZone = eloCmisConnectionManager.getCmisServiceParameters().getStringParameter(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE, TimeZone.getDefault().getID());

        //get context client parameters
        String contextCallId = "";
        String contextCountry = null;
        String contextLanguage = null;
        String contextTicket = (String) detailMap.get(TICKET);
        String contextTimeZone = null;
        try {
            Locale contextLocale = new Locale((String) detailMap.get(LOCALE_LANGUAGE), (String) detailMap.get(LOCALE_COUNTRY), (String) detailMap.get(LOCALE_VARIANT));
            contextLanguage = contextLocale.getLanguage();
            contextCountry = contextLocale.getCountry();
        } catch (NullPointerException e) {
            //all locale parameters were null
        }

        clientInfo = new ClientInfo(
                contextCallId,
                StringUtils.defaultIfEmpty(contextCountry, defaultCountry),
                StringUtils.defaultIfEmpty(contextLanguage, defaultLanguage),
                contextTicket,
                StringUtils.defaultIfEmpty(contextTimeZone, defaultTimeZone)
        );
        return clientInfo;
    }

    public IXConnFactory getIXConnFactory() throws de.elo.utils.net.RemoteException {
        return this.eloCmisConnectionManager.getIxConnFactory();
        //return createIXConnFactory();
    }

    private String getComputerName() {
        String defaultComputerName = getDefaultComputerName();
        String contextComputerName = (String) detailMap.get(COMPUTER_NAME);
        return StringUtils.defaultIfEmpty(contextComputerName, defaultComputerName);
    }

    public IXConnection createConnection() throws de.elo.utils.net.RemoteException {
        IXConnFactory ixConnFactory = getIXConnFactory();
        return createConnection(ixConnFactory);
    }

    public IXConnection createConnection(IXConnFactory ixConnFactory) throws de.elo.utils.net.RemoteException {
        IXConnection ixConnection = null;
        String computerName = getComputerName();

        getLogger().debug("Create ELO connection with the following details:");
        getLogger().debug("Authentication Type: " + ((String) detailMap.get(AUTHENTICATION_TYPE)));
        getLogger().debug("Computer Name: " + computerName);

        try {
            if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(AUTHENTICATION_TYPE_SERVER_APPLICATION)) {
                getLogger().debug("User: " + (String) this.detailMap.get(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER));
                getLogger().debug("Password: " + (String) this.detailMap.get(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD));
                ixConnection = ixConnFactory.create(
                        this.clientInfo,
                        (String) this.detailMap.get(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER),
                        (String) this.detailMap.get(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD),
                        computerName,
                        "");
            } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.BASIC.name())) {
                getLogger().debug("User: " + (String) this.detailMap.get(USER));
                getLogger().debug("Password: " + (String) this.detailMap.get(PASSWORD));
                ixConnection = ixConnFactory.create(
                        this.clientInfo,
                        (String) this.detailMap.get(USER),
                        (String) this.detailMap.get(PASSWORD),
                        computerName,
                        "");
            } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.AS.name()) ||
                    ((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.BASIC_AS.name())) {
                getLogger().debug("User: " + (String) this.detailMap.get(USER));
                getLogger().debug("Password: " + (String) this.detailMap.get(PASSWORD));
                getLogger().debug("UserAs: " + (String) this.detailMap.get(USER_AS));
                ixConnection = ixConnFactory.create(
                        this.clientInfo,
                        (String) this.detailMap.get(USER),
                        (String) this.detailMap.get(PASSWORD),
                        computerName,
                        (String) this.detailMap.get(USER_AS));
            } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.TICKET.name())) {
                ixConnection = ixConnFactory.createFromTicket(this.clientInfo);
            } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.KERBEROS.name())) {
                getLogger().debug("Realm: " + (String) this.detailMap.get(KERBEROS_REALM));
                getLogger().debug("KDC: " + (String) this.detailMap.get(KERBEROS_KDC));
                getLogger().debug("Principal: " + (String) this.detailMap.get(KERBEROS_PRINCIPAL));
                ixConnection = ixConnFactory.createKrb(this.clientInfo,
                        (String) this.detailMap.get(KERBEROS_REALM),
                        (String) this.detailMap.get(KERBEROS_KDC),
                        (String) this.detailMap.get(KERBEROS_PRINCIPAL),
                        computerName
                );
            } else {
                throw new de.elo.utils.net.RemoteException("Authentication type \"" + String.valueOf(detailMap.get(AUTHENTICATION_TYPE)) + "\" not supported");
            }
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().debug("Connection error: " + e.toString());
            getLogger().error("Connection error: " + e.toString());
            throw e;
        }
        return ixConnection;
    }

    public void destroyConnection(IXConnection obj) throws Exception {
        try {
            obj.ix().logout();
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    public boolean isValidConnection(IXConnection ixConnection) {
        boolean isValid = false;
        try {
            ixConnection.ix().alive();
            isValid = true;
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
        return isValid;
    }

    public boolean isInIdentityList(String identities) {
        String delimiter = ",";
        String identity = null;
        if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(AUTHENTICATION_TYPE_SERVER_APPLICATION)) {
            identity = (String) this.detailMap.get(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER);
        } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.BASIC.name())) {
            identity = (String) this.detailMap.get(USER);
        } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.AS.name()) ||
                ((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.BASIC_AS.name())) {
            identity = (String) this.detailMap.get(USER_AS);
        } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.TICKET.name())) {
            identity = this.clientInfo.getTicket();
        } else if (((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.KERBEROS.name())) {
            identity = (String) this.detailMap.get(KERBEROS_PRINCIPAL);
        }
        return ((identity != null) && (identities.equals("*") || (delimiter + identities + delimiter).contains(delimiter + identity + delimiter)));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || (!(obj instanceof EloConnectionDetails))) {
            return false;
        }
        return this.detailMap.equals(((EloConnectionDetails) obj).detailMap);
    }

    @Override
    public int hashCode() {
        return detailMap.hashCode();
    }

    private String getDefaultComputerName() {
        String host = "unknown";
        InetAddress inetAddress = null;
        try {

            inetAddress = InetAddress.getLocalHost();
            host = inetAddress.getHostName();
        } catch (UnknownHostException ex) {
            if (inetAddress != null) {
                host = inetAddress.getHostAddress();
            }
        }
        return host;
    }
}
