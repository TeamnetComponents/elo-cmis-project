package org.cmis.server.elo.commons;

/**
 * @author andreeaf
 * @since 9/11/2014 1:26 PM
 */
public enum EloCmisAuthenticationType {
    BASIC,
    BASIC_AS,
    AS,
    KERBEROS,
    TICKET;

    public static EloCmisAuthenticationType fromValue(String v) {
        if (v == null) {
            v = EloCmisAuthenticationType.BASIC.name();
        }
        for (EloCmisAuthenticationType c : EloCmisAuthenticationType.values()) {
            if (c.name().toLowerCase().equals(v.toLowerCase())) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
