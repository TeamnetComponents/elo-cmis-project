//package org.cmis.server.elo.connection;
//
///**
// * Created by Lucian.Dragomir on 4/22/2015.
// */
//public enum EloAuthenticationType {
//    BASIC,
//    BASIC_AS,
//    //AS,
//    KERBEROS,
//    TICKET;
//
//    public static EloAuthenticationType fromValue(String v) {
//        if (v == null) {
//            v = EloAuthenticationType.BASIC.name();
//        }
//        for (EloAuthenticationType c : EloAuthenticationType.values()) {
//            if (c.name().toLowerCase().equals(v.toLowerCase())) {
//                return c;
//            }
//        }
//        throw new IllegalArgumentException(v);
//    }
//}