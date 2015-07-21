package de.elo.extension;

import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 6/5/2015.
 */
public class IXConnData {
    public static String PROPERTY_URL = "URL";
    public static String PROPERTY_USER = "USER";
    public static String PROPERTY_PASSWORD = "PASSWORD";
    public static String PROPERTY_BASE_DIR = "BASE_DIR";

    public static Properties properties;

    static {
        setProperties();
    }

    public static void setProperties() {
        properties = new Properties();

        properties.put(PROPERTY_BASE_DIR, "C:\\__TEMP\\elo");

        properties.put(PROPERTY_URL, "http://sol-rhel-10:8080/ix-elo/ix");
        properties.put(PROPERTY_USER, "Administrator");
        properties.put(PROPERTY_PASSWORD, "elo@RENNS2015");

        properties.put(PROPERTY_URL, "http://10.6.17.79:8080/ix-elo/ix");
        properties.put(PROPERTY_USER, "Administrator");
        properties.put(PROPERTY_PASSWORD, "elo$$");
    }

    public static String getURL() {
        return properties.getProperty(PROPERTY_URL);
    }

    public static String getUser() {
        return properties.getProperty(PROPERTY_USER);
    }

    public static String getPassword() {
        return properties.getProperty(PROPERTY_PASSWORD);
    }

    public static String getBaseDir() {
        return properties.getProperty(PROPERTY_BASE_DIR);
    }
}
