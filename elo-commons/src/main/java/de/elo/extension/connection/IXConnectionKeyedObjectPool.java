package de.elo.extension.connection;

import de.elo.ix.client.IXConnection;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.platform.common.utils.properties.PropertiesUtil;
import org.platform.common.utils.template.TemplateEngine;

/**
 * Created by Lucian.Dragomir on 6/4/2015.
 */
public final class IXConnectionKeyedObjectPool extends GenericKeyedObjectPool<IXConnectionKey, IXConnection> {

    private static final String ANY_USER_ALLOWED = "*";

    private static final String INDEX_PROPERTY_NAME = "index";
    private static final String INDEX_VARIABLE_NAME = "${" + INDEX_PROPERTY_NAME + "}";
    private static final String MAX_ACTIVE = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".maxActive";
    private static final String WHEN_EXHAUSTED_ACTION = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".whenExhaustedAction";
    private static final String MAX_WAIT = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".maxWait";
    private static final String MIN_IDLE = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".minIdle";
    private static final String MAX_IDLE = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".maxIdle";
    private static final String MAX_TOTAL = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".maxTotal";
    private static final String TEST_ON_BORROW = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".testOnBorrow";
    private static final String TEST_ON_RETURN = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".testOnReturn";
    private static final String TIME_BETWEEN_EVICTION_RUNS_MILLIS = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".timeBetweenEvictionRunsMillis";
    private static final String NUM_TESTS_PER_EVICTION_RUN = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".numTestsPerEvictionRun";
    private static final String MIN_EVICTABLE_IDLE_TIME_MILLIS = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".minEvictableIdleTimeMillis";
    private static final String TEST_WHILE_IDLE = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".testWhileIdle";

    private static final String ALLOWED_USERS = "elo.connection.pool." + INDEX_VARIABLE_NAME + ".allowed.users";

    private int index;
    private String allowedUsers;

    private IXPoolableConnectionManager IXPoolableConnectionManager;


    public IXConnectionKeyedObjectPool(IXConnectionKeyedObjectPoolFactory factory, int index) {
        super(factory);
        this.index = index;
        this.IXPoolableConnectionManager = factory.getIXPoolableConnectionManager();

        String[] variables = new String[]{INDEX_PROPERTY_NAME, String.valueOf(this.index)};

        //apache pool attributes
        this.setMaxActive(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(MAX_ACTIVE, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setWhenExhaustedAction(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(WHEN_EXHAUSTED_ACTION, variables), 0, this.IXPoolableConnectionManager.getProperties()).byteValue());
        this.setMaxWait(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(MAX_WAIT, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setMinIdle(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(MIN_IDLE, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setMaxIdle(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(MAX_IDLE, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setMaxTotal(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(MAX_TOTAL, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setTestOnBorrow(PropertiesUtil.getPropertyAsBoolean(TemplateEngine.getInstance().getValueFromTemplate(TEST_ON_BORROW, variables), false, this.IXPoolableConnectionManager.getProperties()));
        this.setTestOnReturn(PropertiesUtil.getPropertyAsBoolean(TemplateEngine.getInstance().getValueFromTemplate(TEST_ON_RETURN, variables), false, this.IXPoolableConnectionManager.getProperties()));
        this.setTimeBetweenEvictionRunsMillis(PropertiesUtil.getPropertyAsLong(TemplateEngine.getInstance().getValueFromTemplate(TIME_BETWEEN_EVICTION_RUNS_MILLIS, variables), Long.valueOf(0), this.IXPoolableConnectionManager.getProperties()));
        this.setNumTestsPerEvictionRun(PropertiesUtil.getPropertyAsInteger(TemplateEngine.getInstance().getValueFromTemplate(NUM_TESTS_PER_EVICTION_RUN, variables), 0, this.IXPoolableConnectionManager.getProperties()));
        this.setMinEvictableIdleTimeMillis(PropertiesUtil.getPropertyAsLong(TemplateEngine.getInstance().getValueFromTemplate(MIN_EVICTABLE_IDLE_TIME_MILLIS, variables), Long.valueOf(0), this.IXPoolableConnectionManager.getProperties()));
        this.setTestWhileIdle(PropertiesUtil.getPropertyAsBoolean(TemplateEngine.getInstance().getValueFromTemplate(TEST_WHILE_IDLE, variables), false, this.IXPoolableConnectionManager.getProperties()));

        //extended attributes
        this.allowedUsers = PropertiesUtil.getPropertyAsString(TemplateEngine.getInstance().getValueFromTemplate(ALLOWED_USERS, variables), null, this.IXPoolableConnectionManager.getProperties());
    }

    private IXPoolableConnectionManager getIXPoolableConnectionManager() {
        return this.IXPoolableConnectionManager;
    }

    public String getAllowedUsers() {
        return allowedUsers;
    }

    public boolean allow(IXConnectionKey ixConnectionKey) {
        String delimiter = ",";
        String identity = null;

        if (((String) ixConnectionKey.getProperties().get(EloUtilsConnection.AUTHENTICATION_TYPE)).equals(EloUtilsConnection.AUTHENTICATION_TYPE_SERVER_APPLICATION)) {
            identity = (String) this.getIXPoolableConnectionManager().getProperties().get(EloUtilsConnection.ELO_CONNECTION_FACTORY_CONNECTION_APP_USER);
        } else if (((String) ixConnectionKey.getProperties().get(EloUtilsConnection.AUTHENTICATION_TYPE)).equals(EloAuthenticationType.BASIC.name())) {
            identity = (String) ixConnectionKey.getProperties().get(EloUtilsConnection.USER);
        } else if (((String) ixConnectionKey.getProperties().get(EloUtilsConnection.AUTHENTICATION_TYPE)).equals(EloAuthenticationType.BASIC_AS.name())) {
            identity = (String) ixConnectionKey.getProperties().get(EloUtilsConnection.USER_AS);
        } else if (((String) ixConnectionKey.getProperties().get(EloUtilsConnection.AUTHENTICATION_TYPE)).equals(EloAuthenticationType.TICKET.name())) {
            identity = (String) ixConnectionKey.getProperties().get(EloUtilsConnection.TICKET);
        } else if (((String) ixConnectionKey.getProperties().get(EloUtilsConnection.AUTHENTICATION_TYPE)).equals(EloAuthenticationType.KERBEROS.name())) {
            identity = (String) ixConnectionKey.getProperties().get(EloUtilsConnection.KERBEROS_PRINCIPAL);
        }
        return ((identity != null) && ((delimiter + getAllowedUsers() + delimiter).contains(delimiter + ANY_USER_ALLOWED + delimiter) || (delimiter + getAllowedUsers() + delimiter).contains(delimiter + identity + delimiter)));
    }

    @Override
    public synchronized void close() throws Exception {
        super.close();
    }
}
