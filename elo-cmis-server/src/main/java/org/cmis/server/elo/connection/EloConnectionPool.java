//package org.cmis.server.elo.connection;
//
//import de.elo.ix.client.IXConnection;
//import org.apache.commons.pool.impl.GenericKeyedObjectPool;
//import org.cmis.util.CmisServiceParameters;
//
///**
// * Created by Lucian.Dragomir on 9/22/2014.
// */
//public class EloConnectionPool extends GenericKeyedObjectPool<EloConnectionDetails, IXConnection> {
//
//    private static final String MAX_ACTIVE = "elo.connection.pool.${index}.maxActive";
//    private static final String WHEN_EXHAUSTED_ACTION = "elo.connection.pool.${index}.whenExhaustedAction";
//    private static final String MAX_WAIT = "elo.connection.pool.${index}.maxWait";
//    private static final String MIN_IDLE = "elo.connection.pool.${index}.minIdle";
//    private static final String MAX_IDLE = "elo.connection.pool.${index}.maxIdle";
//    private static final String MAX_TOTAL = "elo.connection.pool.${index}.maxTotal";
//    private static final String TEST_ON_BORROW = "elo.connection.pool.${index}.testOnBorrow";
//    private static final String TEST_ON_RETURN = "elo.connection.pool.${index}.testOnReturn";
//    private static final String TIME_BETWEEN_EVICTION_RUNS_MILLIS = "elo.connection.pool.${index}.timeBetweenEvictionRunsMillis";
//    private static final String NUM_TESTS_PER_EVICTION_RUN = "elo.connection.pool.${index}.numTestsPerEvictionRun";
//    private static final String MIN_EVICTABLE_IDLE_TIME_MILLIS = "elo.connection.pool.${index}.minEvictableIdleTimeMillis";
//    private static final String TEST_WHILE_IDLE = "elo.connection.pool.${index}.testWhileIdle";
//
//    private static final String ALLOWED_USERS = "elo.connection.pool.${index}.allowed.users";
//
//    private int index;
//    private String allowedUsers;
//
//    public EloConnectionPool(EloConnectionPoolFactory factory, int index) {
//        super(factory);
//        this.index = index;
//        CmisServiceParameters cmisServiceParameters = factory.getEloCmisConnectionManager().getCmisServiceParameters();
//
//        //apache pool attributes
//        this.setMaxActive(cmisServiceParameters.getIntegerParameter(replace(MAX_ACTIVE), null));
//        this.setWhenExhaustedAction(cmisServiceParameters.getIntegerParameter(replace(WHEN_EXHAUSTED_ACTION), null).byteValue());
//        this.setMaxWait(cmisServiceParameters.getIntegerParameter(replace(MAX_WAIT), null));
//        this.setMinIdle(cmisServiceParameters.getIntegerParameter(replace(MIN_IDLE), null));
//        this.setMaxIdle(cmisServiceParameters.getIntegerParameter(replace(MAX_IDLE), null));
//        this.setMaxTotal(cmisServiceParameters.getIntegerParameter(replace(MAX_TOTAL), null));
//        this.setTestOnBorrow(cmisServiceParameters.getBooleanParameter(replace(TEST_ON_BORROW), null));
//        this.setTestOnReturn(cmisServiceParameters.getBooleanParameter(replace(TEST_ON_RETURN), null));
//        this.setTimeBetweenEvictionRunsMillis(cmisServiceParameters.getLongParameter(replace(TIME_BETWEEN_EVICTION_RUNS_MILLIS), null).intValue());
//        this.setNumTestsPerEvictionRun(cmisServiceParameters.getIntegerParameter(replace(NUM_TESTS_PER_EVICTION_RUN), null));
//        this.setMinEvictableIdleTimeMillis(cmisServiceParameters.getLongParameter(replace(MIN_EVICTABLE_IDLE_TIME_MILLIS), null).intValue());
//        this.setTestWhileIdle(cmisServiceParameters.getBooleanParameter(replace(TEST_WHILE_IDLE), null));
//
//        //extended attributes
//        this.allowedUsers = cmisServiceParameters.getStringParameter(replace(ALLOWED_USERS), null);
//    }
//
//    private String replace(String template) {
//        return template.replace("${index}", String.valueOf(this.index));
//    }
//
//    public String getAllowedUsers() {
//        return allowedUsers;
//    }
//
//    public boolean allow(EloConnectionDetails eloConnectionDetails) {
//        return eloConnectionDetails.isInIdentityList(this.getAllowedUsers());
//    }
//
//}
