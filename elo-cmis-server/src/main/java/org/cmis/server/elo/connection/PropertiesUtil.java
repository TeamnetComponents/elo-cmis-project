//package org.cmis.server.elo.connection;
//
//import java.math.BigInteger;
//import java.utils.Map;
//import java.utils.Properties;
//
///**
// * Created by Lucian.Dragomir on 4/22/2015.
// */
//public class PropertiesUtil {
//
//    public static boolean checkProperties(Object... properties) {
//        boolean valid = true;
//        int index = 0;
//        while ((valid) && (index < properties.length)) {
//            valid = valid && ((properties[index] == null) || (properties[index] instanceof Properties) || (properties[index] instanceof Map));
//            index++;
//        }
//        return valid;
//    }
//
//    private static String getProperty(String key, String defaultValue, Object... properties) {
//        int index = 0;
//        while (index < properties.length) {
//            //try to find the requested property key
//            if (properties[index] instanceof Properties) {
//                if (((Properties) properties[index]).containsKey(key)) {
//                    return ((Properties) properties[index]).getProperty(key);
//                }
//            } else if (properties[index] instanceof Map) {
//                if (((Map) properties[index]).containsKey(key)) {
//                    return String.valueOf(((Map) properties[index]).get(key));
//                }
//            } else if (properties[index] == null) {
//                //do nothing, just jump to the new index
//            } else {
//                throw new RuntimeException("PropertiesUtil.getProperty received a properties item different than Properties or Map.");
//            }
//            index++;
//        }
//        return defaultValue;
//    }
//
//    public static BigInteger getPropertyAsBigInteger(String key, BigInteger defaultValue, Object... properties) {
//        return new BigInteger(getProperty(key, String.valueOf(defaultValue), properties));
//    }
//
//    public static Long getPropertyAsLong(String key, Long defaultValue, Object... properties) {
//        return new Long(getProperty(key, String.valueOf(defaultValue), properties));
//    }
//
//    public static Integer getPropertyAsInteger(String key, Integer defaultValue, Object... properties) {
//        return new Integer(getProperty(key, String.valueOf(defaultValue), properties));
//    }
//
//    public static String getPropertyAsString(String key, String defaultValue, Object... properties) {
//        return getProperty(key, defaultValue, properties);
//    }
//
//    public static Boolean getPropertyAsBoolean(String key, Boolean defaultValue, Object... properties) {
//        return Boolean.parseBoolean(getProperty(key, String.valueOf(defaultValue), properties));
//    }
//}
