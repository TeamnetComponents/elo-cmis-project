package de.elo.extension.connection;

import de.elo.ix.client.ClientInfo;
import de.elo.ix.client.IXConnFactory;
import de.elo.ix.client.IXConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.platform.common.utils.properties.PropertiesUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;


/**
 * Created by Lucian.Dragomir on 4/22/2015.
 */
public final class EloUtilsConnection {

    private static List<String> connectionParameterList;
    public static final String AUTHENTICATION_TYPE_SERVER_APPLICATION = "SERVER_APPLICATION";

    public static final String ELO_CONNECTION_PARAMETER_PREFIX = "elo.connection.";
    public static final String ELO_CONNECTION_AUTHENTICATION_PREFIX = "authentication.";
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION PROPERTIES OPTIONS
    //------------------------------------------------------------------------------------------------------------------
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_URL = ELO_CONNECTION_PARAMETER_PREFIX + "factory.property.url";
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS = ELO_CONNECTION_PARAMETER_PREFIX + "factory.property.nbOfCnns";
    public static final String ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS = ELO_CONNECTION_PARAMETER_PREFIX + "factory.property.timeoutSeconds";
    //------------------------------------------------------------------------------------------------------------------
    //ELO SESSION OPTIONS
    //------------------------------------------------------------------------------------------------------------------
    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_NAME = ELO_CONNECTION_PARAMETER_PREFIX + "factory.session.appName";
    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_VERSION = ELO_CONNECTION_PARAMETER_PREFIX + "factory.session.appVersion";
    //------------------------------------------------------------------------------------------------------------------
    //ELO CONNECTION CREDENTIALS
    //------------------------------------------------------------------------------------------------------------------
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_USER = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.appUser";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.appPassword";
    //------------------------------------------------------------------------------------------------------------------
    //ELO DEFAULT CLIENT INFO PROPERTIES
    //------------------------------------------------------------------------------------------------------------------
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.client.country";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.client.language";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.client.timeZone";
    //------------------------------------------------------------------------------------------------------------------
    //ELO DEFAULT SERVER INFO PROPERTIES
    //------------------------------------------------------------------------------------------------------------------
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_SERVER_COUNTRY = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.server.country";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_SERVER_LANGUAGE = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.server.language";
    public static final String ELO_CONNECTION_FACTORY_CONNECTION_SERVER_TIMEZONE = ELO_CONNECTION_PARAMETER_PREFIX + "factory.connection.server.timeZone";
    //------------------------------------------------------------------------------------------------------------------
    //COMPUTER NAME
    //------------------------------------------------------------------------------------------------------------------
    public static final String COMPUTER_NAME = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "computer.name";
    //------------------------------------------------------------------------------------------------------------------
    //AUTHENTICATION TYPE
    //------------------------------------------------------------------------------------------------------------------
    public static final String AUTHENTICATION_TYPE = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.type";
    //BASIC
    public static final String USER = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "username";
    public static final String PASSWORD = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "password";
    //AS
    public static final String USER_AS = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "userAs".toLowerCase(); //fixed for tomcat that has the headers converted to lowercase
    //KERBEROS
    public static final String KERBEROS_REALM = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "kerberos.realm";
    public static final String KERBEROS_KDC = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "kerberos.kdc";
    public static final String KERBEROS_PRINCIPAL = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "kerberos.principal";
    //TICKET
    public static final String TICKET = ELO_CONNECTION_PARAMETER_PREFIX + ELO_CONNECTION_AUTHENTICATION_PREFIX + "ticket";



    public static IXConnFactory createIXConnFactory(Object... properties) throws de.elo.utils.net.RemoteException {
        IXConnFactory ixConnFactory = null;
        // set connection properties
        String url = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_PROPERTY_URL, "", properties);
        int nbOfCnns = PropertiesUtil.getPropertyAsInteger(ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS, 0, properties);
        int timeoutSeconds = PropertiesUtil.getPropertyAsInteger(ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS, 0, properties);
        Properties connectionProperties = IXConnFactory.createConnProps(url, nbOfCnns, timeoutSeconds);

        // set session options
        String appName = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_SESSION_APP_NAME, "Default Application Name", properties);
        String appVersion = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_SESSION_APP_VERSION, "1.0", properties);
        Properties sessionOptions = IXConnFactory.createSessionOptions(appName, appVersion);

        //create and return IXConnectionFactory object
        ixConnFactory = new IXConnFactory(connectionProperties, sessionOptions);
        return ixConnFactory;
    }

    public static Locale getServerLocale(Object... properties) {
        Locale locale;
        //get the server locale
        String contextCountry = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_SERVER_COUNTRY, null, properties);
        String contextLanguage = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_SERVER_LANGUAGE, null, properties);
        String contextTimeZone = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_SERVER_TIMEZONE, null, properties);

        if ((contextCountry == null) && (contextLanguage == null) && (contextTimeZone == null)) {
            locale = Locale.getDefault();
        } else {
            locale = (new Locale.Builder()).setRegion(contextCountry).setLanguage(contextLanguage).setVariant(contextTimeZone).build();
        }
        return locale;
    }

    public static ClientInfo createClientInfo(Object... properties) {
        ClientInfo clientInfo = null;

        //get default client parameters
        String contextCountry = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY, Locale.getDefault().getCountry(), properties);
        String contextLanguage = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE, Locale.getDefault().getLanguage(), properties);
        String contextTimeZone = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE, TimeZone.getDefault().getID(), properties);

        //get context client parameters
        String contextCallId = "";
        String contextTicket = null;

        clientInfo = new ClientInfo(
                contextCallId,
                contextCountry,
                contextLanguage,
                contextTicket,
                contextTimeZone
        );
        return clientInfo;
    }

    private static String getComputerName(Object... properties) {
        return PropertiesUtil.getPropertyAsString(COMPUTER_NAME, getDefaultComputerName(), properties);
    }

    public static IXConnection createIXConnection(Object... properties) throws de.elo.utils.net.RemoteException {
        return createIXConnection(null, null, properties);
    }

    public static IXConnection createIXConnection(IXConnFactory ixConnFactory, ClientInfo clientInfo, Object... properties) throws de.elo.utils.net.RemoteException {
        IXConnection ixConnection = null;
        if (ixConnFactory == null) {
            ixConnFactory = createIXConnFactory(properties);
        }
        if (clientInfo == null) {
            clientInfo = createClientInfo(properties);
        }

        try {
            if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, AUTHENTICATION_TYPE_SERVER_APPLICATION, properties)).equals(AUTHENTICATION_TYPE_SERVER_APPLICATION)) {
                ixConnection = ixConnFactory.create(
                        clientInfo,
                        PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER, "", properties),
                        PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD, "", properties),
                        getComputerName(properties),
                        "");
            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.BASIC.name())) {
                ixConnection = ixConnFactory.create(
                        clientInfo,
                        PropertiesUtil.getPropertyAsString(USER, "", properties),
                        PropertiesUtil.getPropertyAsString(PASSWORD, "", properties),
                        getComputerName(properties),
                        "");
            } else if (/*((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.AS.name()) ||*/
                    (PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.BASIC_AS.name())) {
                ixConnection = ixConnFactory.create(
                        clientInfo,
                        PropertiesUtil.getPropertyAsString(USER, "", properties),
                        PropertiesUtil.getPropertyAsString(PASSWORD, "", properties),
                        getComputerName(properties),
                        PropertiesUtil.getPropertyAsString(USER_AS, "", properties));
            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.TICKET.name())) {
                ixConnection = ixConnFactory.createFromTicket(clientInfo);
            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.KERBEROS.name())) {
                ixConnection = ixConnFactory.createKrb(clientInfo,
                        PropertiesUtil.getPropertyAsString(KERBEROS_REALM, "", properties),
                        PropertiesUtil.getPropertyAsString(KERBEROS_KDC, "", properties),
                        PropertiesUtil.getPropertyAsString(KERBEROS_PRINCIPAL, "", properties),
                        getComputerName(properties)
                );
            } else {
                throw new de.elo.utils.net.RemoteException("Authentication type \"" + (PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)) + "\" not supported");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return ixConnection;
    }

    public static Properties createCredentialsBasic(String user, String password) {
        return createCredentialsBasic(user, password, null);
    }

    public static Properties createCredentialsBasic(String user, String password, String host) {
        Properties properties = new Properties();
        properties.setProperty(AUTHENTICATION_TYPE, EloAuthenticationType.BASIC.name());
        properties.setProperty(USER, user);
        properties.setProperty(PASSWORD, password);
        if ((host == null) || (host.length() == 0)) {
            properties.setProperty(COMPUTER_NAME, getDefaultComputerName());
        } else {
            properties.setProperty(COMPUTER_NAME, host);
        }
        return properties;
    }

    public static Properties createCredentialsBasicAs(String user, String password, String userAs) {
        return createCredentialsBasicAs(user, password, userAs, null);
    }

    public static Properties createCredentialsBasicAs(String user, String password, String userAs, String host) {
        Properties properties = new Properties();
        properties.setProperty(AUTHENTICATION_TYPE, EloAuthenticationType.BASIC_AS.name());
        properties.setProperty(USER, user);
        properties.setProperty(PASSWORD, password);
        properties.setProperty(USER_AS, userAs);
        if ((host == null) || (host.length() == 0)) {
            properties.setProperty(COMPUTER_NAME, getDefaultComputerName());
        } else {
            properties.setProperty(COMPUTER_NAME, host);
        }
        return properties;
    }

    public static void destroyConnection(IXConnection ixConnection) throws Exception {
        try {
            if (ixConnection != null) {
                ixConnection.ix().logout();
            }
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    public static boolean isValidConnection(IXConnection ixConnection) {
        try {
            ixConnection.ix().alive();
            return true;
        } catch (RemoteException e) {
            //e.printStackTrace();
            return false;
        }
    }

    private static String getDefaultComputerName() {
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


    public static List<String> getConnectionParameterList() {
        return connectionParameterList;
    }

    static {
        //init class properties in the list
        connectionParameterList = initConnectionParameterList();
    }

    private static List<String> initConnectionParameterList() {
        List<String> connectionParameterList = new ArrayList<String>();
        Field[] declaredFields = EloUtilsConnection.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers())) {
                String parameterName = null;
                try {
                    parameterName = String.valueOf(field.get(null));
                } catch (IllegalAccessException e) {

                }
                if (parameterName != null && parameterName.startsWith(ELO_CONNECTION_PARAMETER_PREFIX)) {
                    connectionParameterList.add(parameterName);
                }
            }
        }
        return connectionParameterList;
    }

    //Methods to print parameter list to console

    public static void printConnectionParameterList() {
        //get values from system parameters
        System.out.println("Connection parameters:");
        System.out.println("--------------------------------------------------------------------------------------------");
        for (String propertyName : getConnectionParameterList()) {
            if (propertyName != null && !propertyName.equals(ELO_CONNECTION_PARAMETER_PREFIX)) {
                System.out.println(propertyName);
            }
        }
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    public static void testEloConnection() {
        int exitStatus;
        IXConnection ixConnection = null;
        //get connection parameters from system properties
        Properties properties = new Properties();
        String strOut;

        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("Connection parameters:");

        //get values from system parameters
        for (String propertyName : getConnectionParameterList()) {
            String propertyValue = System.getProperty(propertyName, "");
            if (propertyName != null && !propertyName.equals(ELO_CONNECTION_PARAMETER_PREFIX)) {
                properties.setProperty(propertyName, propertyValue);
                System.out.println(propertyName + "=" + propertyValue);
            }
        }
        System.out.println("--------------------------------------------------------------------------------------------");

        //try open a connection
        try {
            exitStatus = 0; //CONNECTION SUCCESS
            ixConnection = createIXConnection(properties);
            destroyConnection(ixConnection);
            strOut = "Connection status: SUCCESS";
        } catch (Exception e) {
            exitStatus = 1; //CONNECTION ERROR
            strOut = "Connection status: ERROR";
            strOut = strOut + "\n" + ExceptionUtils.getStackTrace(e);
        }
        System.out.println(strOut);
        System.out.println("--------------------------------------------------------------------------------------------");
        System.exit(exitStatus);
    }

    public static void main(String[] args) {
        testEloConnection();
        //printConnectionParameterList();
    }
}