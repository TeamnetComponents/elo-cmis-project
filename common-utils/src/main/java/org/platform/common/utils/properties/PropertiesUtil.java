package org.platform.common.utils.properties;

import java.math.BigInteger;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 4/22/2015.
 */
public class PropertiesUtil {

    public static boolean checkProperties(Object... properties) {
        boolean valid = true;
        int index = 0;
        while ((valid) && (index < properties.length)) {
            valid = valid && ((properties[index] == null) || (properties[index] instanceof Properties) || (properties[index] instanceof Map));
            index++;
        }
        return valid;
    }

    private static String getProperty(String key, Object defaultValue, Object... properties) {
        int index = 0;
        if (properties != null) {
            while (index < properties.length) {
                //try to find the requested property key
                if (properties[index] instanceof Properties) {
                    if (((Properties) properties[index]).containsKey(key)) {
                        return ((Properties) properties[index]).getProperty(key);
                    }
                } else if (properties[index] instanceof Map) {
                    if (((Map) properties[index]).containsKey(key)) {
                        return String.valueOf(((Map) properties[index]).get(key));
                    }
                } else if (properties[index] == null) {
                    //do nothing, just jump to the new index
                } else {
                    throw new RuntimeException("PropertiesUtil.getProperty received a properties item different than Properties or Map.");
                }
                index++;
            }
        }
        if (defaultValue == null) {
            return null;
        } else {
            return String.valueOf(defaultValue);
        }
    }

    public static BigInteger getPropertyAsBigInteger(String key, BigInteger defaultValue, Object... properties) {
        String value = null;
        try {
            value = getProperty(key, defaultValue, properties);
            return new BigInteger(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to cast [" + value + "] to BigInteger for property name [" + key + "]");
        }
    }

    public static Long getPropertyAsLong(String key, Long defaultValue, Object... properties) {
        String value = null;
        try {
            value = getProperty(key, defaultValue, properties);
            return new Long(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to cast [" + value + "] to Long for property name [" + key + "]");
        }
    }

    public static Integer getPropertyAsInteger(String key, Integer defaultValue, Object... properties) {
        String value = null;
        try {
            value = getProperty(key, defaultValue, properties);
            return new Integer(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to cast [" + value + "] to Integer for property name [" + key + "]");
        }
    }

    public static String getPropertyAsString(String key, String defaultValue, Object... properties) {
        String value = null;
        try {
            value = getProperty(key, defaultValue, properties);
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve String property name [" + key + "]");
        }
    }

    public static Boolean getPropertyAsBoolean(String key, Boolean defaultValue, Object... properties) {
        String value = null;
        try {
            value = getProperty(key, defaultValue, properties);
            return new Boolean(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to cast [" + value + "] to Boolean for property name [" + key + "]");
        }
    }
}
