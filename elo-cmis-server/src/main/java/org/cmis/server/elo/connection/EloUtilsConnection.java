//package org.cmis.server.elo.connection;
//
//import de.elo.ix.client.ClientInfo;
//import de.elo.ix.client.IXConnFactory;
//import de.elo.ix.client.IXConnection;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.rmi.RemoteException;
//import java.utils.Locale;
//import java.utils.Properties;
//import java.utils.TimeZone;
//
//import static org.cmis.server.elo.commons.EloCmisContextParameter.COMPUTER_NAME;
//
///**
// * Created by Lucian.Dragomir on 4/22/2015.
// */
//public class EloUtilsConnection {
//
//    public static final String ELO_CONNECTION_PARAMETER_PREFIX = "elo.connection.";
//
//    //------------------------------------------------------------------------------------------------------------------
//    //ELO CONNECTION FACTORY PROPERTIES
//    //------------------------------------------------------------------------------------------------------------------
//    //ELO CONNECTION PROPERTIES OPTIONS
//    public static final String ELO_CONNECTION_FACTORY_PROPERTY_URL = "elo.connection.factory.property.url";
//    public static final String ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS = "elo.connection.factory.property.nbOfCnns";
//    public static final String ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS = "elo.connection.factory.property.timeoutSeconds";
//    //ELO SESSION OPTIONS
//    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_NAME = "elo.connection.factory.session.appName";
//    public static final String ELO_CONNECTION_FACTORY_SESSION_APP_VERSION = "elo.connection.factory.session.appVersion";
//    //------------------------------------------------------------------------------------------------------------------
//    //ELO CONNECTION PROPERTIES
//    //------------------------------------------------------------------------------------------------------------------
//    //ELO CONNECTION CREDENTIALS
//    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_USER = "elo.connection.factory.connection.appUser";
//    public static final String ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD = "elo.connection.factory.connection.appPassword";
//    //ELO DEFAULT CLIENT INFO PROPERTIES
//    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY = "elo.connection.factory.connection.client.country";
//    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE = "elo.connection.factory.connection.client.language";
//    public static final String ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE = "elo.connection.factory.connection.client.timeZone";
//
//    private static final String AUTHENTICATION_TYPE_SERVER_APPLICATION = "SERVER_APPLICATION";
//
//    //AUTHENTICATION TYPE
//    public static final String AUTHENTICATION_TYPE = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.type";
//    //BASIC
//    public static final String USER = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.username";
//    public static final String PASSWORD = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.password";
//    //AS
//    public static final String USER_AS = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.userAs".toLowerCase(); //fixed for tomcat that has the headers converted to lowercase
//    //KERBEROS
//    public static final String KERBEROS_REALM = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.kerberos.realm";
//    public static final String KERBEROS_KDC = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.kerberos.kdc";
//    public static final String KERBEROS_PRINCIPAL = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.kerberos.principal";
//    //TICKET
//    public static final String TICKET = ELO_CONNECTION_PARAMETER_PREFIX + "authentication.ticket";
//
//
//    public static IXConnFactory createIXConnFactory(Object... properties) throws de.elo.utils.net.RemoteException {
//        IXConnFactory ixConnFactory = null;
//        // set connection properties
//        String url = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_PROPERTY_URL, "", properties);
//        int nbOfCnns = PropertiesUtil.getPropertyAsInteger(ELO_CONNECTION_FACTORY_PROPERTY_NB_OF_CNNS, 0, properties);
//        int timeoutSeconds = PropertiesUtil.getPropertyAsInteger(ELO_CONNECTION_FACTORY_PROPERTY_TIMEOUT_SECONDS, 0, properties);
//        Properties connectionProperties = IXConnFactory.createConnProps(url, nbOfCnns, timeoutSeconds);
//
//        // set session options
//        String appName = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_SESSION_APP_NAME, "Default Application Name", properties);
//        String appVersion = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_SESSION_APP_VERSION, "0.0", properties);
//        Properties sessionOptions = IXConnFactory.createSessionOptions(appName, appVersion);
//
//        //create and return IXConnectionFactory object
//        ixConnFactory = new IXConnFactory(connectionProperties, sessionOptions);
//        return ixConnFactory;
//    }
//
//    public static ClientInfo createClientInfo(Object... properties) {
//        ClientInfo clientInfo = null;
//
//        //get default client parameters
//        String contextCountry = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_COUNTRY, Locale.getDefault().getCountry(), properties);
//        String contextLanguage = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_LANGUAGE, Locale.getDefault().getLanguage(), properties);
//        String contextTimeZone = PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_CLIENT_TIMEZONE, TimeZone.getDefault().getID(), properties);
//
//        //get context client parameters
//        String contextCallId = "";
//        String contextTicket = null;
//
//        clientInfo = new ClientInfo(
//                contextCallId,
//                contextCountry,
//                contextLanguage,
//                contextTicket,
//                contextTimeZone
//        );
//        return clientInfo;
//    }
//
//    private static String getComputerName(Object... properties) {
//        return PropertiesUtil.getPropertyAsString(COMPUTER_NAME, getDefaultComputerName(), properties);
//    }
//
//
//    public static IXConnection createIXConnection(Object... properties) throws de.elo.utils.net.RemoteException {
//        return createIXConnection(null, null, properties);
//    }
//
//    public static IXConnection createIXConnection(IXConnFactory ixConnFactory, ClientInfo clientInfo, Object... properties) throws de.elo.utils.net.RemoteException {
//        IXConnection ixConnection = null;
//        if (ixConnFactory == null) {
//            ixConnFactory = createIXConnFactory(properties);
//        }
//        if (clientInfo == null) {
//            clientInfo = createClientInfo(properties);
//        }
//
//        try {
//            if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, AUTHENTICATION_TYPE_SERVER_APPLICATION, properties)).equals(AUTHENTICATION_TYPE_SERVER_APPLICATION)) {
//                ixConnection = ixConnFactory.create(
//                        clientInfo,
//                        PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_APP_USER, "", properties),
//                        PropertiesUtil.getPropertyAsString(ELO_CONNECTION_FACTORY_CONNECTION_APP_PASSWORD, "", properties),
//                        getComputerName(properties),
//                        "");
//            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.BASIC.name())) {
//                ixConnection = ixConnFactory.create(
//                        clientInfo,
//                        PropertiesUtil.getPropertyAsString(USER, "", properties),
//                        PropertiesUtil.getPropertyAsString(PASSWORD, "", properties),
//                        getComputerName(properties),
//                        "");
//            } else if (/*((String) detailMap.get(AUTHENTICATION_TYPE)).equals(EloCmisAuthenticationType.AS.name()) ||*/
//                    (PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.BASIC_AS.name())) {
//                ixConnection = ixConnFactory.create(
//                        clientInfo,
//                        PropertiesUtil.getPropertyAsString(USER, "", properties),
//                        PropertiesUtil.getPropertyAsString(PASSWORD, "", properties),
//                        getComputerName(properties),
//                        PropertiesUtil.getPropertyAsString(USER_AS, "", properties));
//            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.TICKET.name())) {
//                ixConnection = ixConnFactory.createFromTicket(clientInfo);
//            } else if ((PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)).equals(EloAuthenticationType.KERBEROS.name())) {
//                ixConnection = ixConnFactory.createKrb(clientInfo,
//                        PropertiesUtil.getPropertyAsString(KERBEROS_REALM, "", properties),
//                        PropertiesUtil.getPropertyAsString(KERBEROS_KDC, "", properties),
//                        PropertiesUtil.getPropertyAsString(KERBEROS_PRINCIPAL, "", properties),
//                        getComputerName(properties)
//                );
//            } else {
//                throw new de.elo.utils.net.RemoteException("Authentication type \"" + (PropertiesUtil.getPropertyAsString(AUTHENTICATION_TYPE, "", properties)) + "\" not supported");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
//        return ixConnection;
//    }
//
//    public static Properties createCredentialsBasic(String user, String password) {
//        Properties properties = new Properties();
//        properties.setProperty(AUTHENTICATION_TYPE, EloAuthenticationType.BASIC.name());
//        properties.setProperty(USER, user);
//        properties.setProperty(PASSWORD, password);
//        return properties;
//    }
//
//    public static Properties createCredentialsBasicAs(String user, String password, String userAs) {
//        Properties properties = new Properties();
//        properties.setProperty(AUTHENTICATION_TYPE, EloAuthenticationType.BASIC_AS.name());
//        properties.setProperty(USER, user);
//        properties.setProperty(PASSWORD, password);
//        properties.setProperty(USER_AS, userAs);
//        return properties;
//    }
//
//    public static void destroyConnection(IXConnection ixConnection) throws Exception {
//        try {
//            if (ixConnection != null) {
//                ixConnection.ix().logout();
//            }
//        } catch (RemoteException e) {
//            //e.printStackTrace();
//        }
//    }
//
//    public static boolean isValidConnection(IXConnection ixConnection) {
//        boolean isValid = false;
//        try {
//            ixConnection.ix().alive();
//            isValid = true;
//        } catch (RemoteException e) {
//            //e.printStackTrace();
//        }
//        return isValid;
//    }
//
//    private static String getDefaultComputerName() {
//        String host = "unknown";
//        InetAddress inetAddress = null;
//        try {
//
//            inetAddress = InetAddress.getLocalHost();
//            host = inetAddress.getHostName();
//        } catch (UnknownHostException ex) {
//            if (inetAddress != null) {
//                host = inetAddress.getHostAddress();
//            }
//        }
//        return host;
//    }
//}
