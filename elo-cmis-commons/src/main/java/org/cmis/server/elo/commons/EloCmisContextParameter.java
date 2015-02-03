package org.cmis.server.elo.commons;

import org.apache.chemistry.opencmis.commons.SessionParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lucian.Dragomir on 12/27/2014.
 */
public class EloCmisContextParameter {
    //NOTE !!!! ALL CONTEXT PARAMETERS USER AS HEADERS MUST BE LOWERCASE

    // ---- general parameter ----
    public static final String ELO_CMIS_CONTEXT_PARAMETER_PREFIX = "elo.cmis.server.";
    public static final String COMPUTER_NAME = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.computer.name";

    //other session parameters (locale information)
    public static final String LOCALE_LANGUAGE = SessionParameter.LOCALE_ISO639_LANGUAGE;
    public static final String LOCALE_COUNTRY = SessionParameter.LOCALE_ISO3166_COUNTRY;
    public static final String LOCALE_VARIANT = SessionParameter.LOCALE_VARIANT;

    //AUTHENTICATION TYPE
    public static final String AUTHENTICATION_TYPE = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.type";
    //AS
    public static final String USER_AS = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.userAs".toLowerCase(); //fixed for tomcat that has the headers converted to lowercase
    //KERBEROS
    public static final String KERBEROS_REALM = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.kerberos.realm";
    public static final String KERBEROS_KDC = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.kerberos.kdc";
    public static final String KERBEROS_PRINCIPAL = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.kerberos.principal";
    //TICKET
    public static final String TICKET = ELO_CMIS_CONTEXT_PARAMETER_PREFIX + "authentication.ticket";
    //BASIC
    //public static final String USER = SessionParameter.USER;
    //public static final String PASSWORD = SessionParameter.PASSWORD;
    public static final String USER = "username";
    public static final String PASSWORD = "password";

    //ALL PARAMETERS FROM THIS CLASS
    private static List<String> contextParameterList;

    // utility class
    private EloCmisContextParameter() {
    }

    public static List<String> getEloCmisContextParameters() {
        return contextParameterList;
    }

    static {
        //init class properties in the list
        contextParameterList = initContextParameterList();
    }


    private static List<String> initContextParameterList() {
        List<String> parameterList = new ArrayList<String>();
        Field[] declaredFields = EloCmisContextParameter.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers())) {
                String fieldValue = null;
                try {
                    fieldValue = String.valueOf(field.get(null));
                } catch (IllegalAccessException e) {

                }
                if (fieldValue != null) {
                    parameterList.add(fieldValue);
                }
            }
        }
        return parameterList;
    }
}