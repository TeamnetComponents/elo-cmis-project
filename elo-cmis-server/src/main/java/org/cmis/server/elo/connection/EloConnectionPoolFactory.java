//package org.cmis.server.elo.connection;
//
//import de.elo.ix.client.IXConnection;
//import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
//import org.cmis.server.elo.EloCmisConnectionManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by Lucian.Dragomir on 6/10/2014.
// */
//public class EloConnectionPoolFactory extends BaseKeyedPoolableObjectFactory<EloConnectionDetails, IXConnection> {
//    private static final Logger LOG = LoggerFactory.getLogger(EloConnectionPoolFactory.class);
//    private static final String POOL_COUNT = "elo.connection.pool.count";
//
//    private EloCmisConnectionManager eloCmisConnectionManager;
//
//    public EloConnectionPoolFactory(EloCmisConnectionManager eloCmisConnectionManager) {
//        this.eloCmisConnectionManager = eloCmisConnectionManager;
//    }
//
//    public EloCmisConnectionManager getEloCmisConnectionManager() {
//        return eloCmisConnectionManager;
//    }
//
//    public int getPoolCount() {
//        return this.eloCmisConnectionManager.getCmisServiceParameters().getIntegerParameter(POOL_COUNT, 0);
//    }
//
//    public EloConnectionPool[] createConnectionPools() {
//        EloConnectionPool[] eloConnectionPools = new EloConnectionPool[getPoolCount()];
//        for (int index = 0; index < getPoolCount(); index++) {
//            eloConnectionPools[index] = new EloConnectionPool(this, index);
//        }
//        return eloConnectionPools;
//    }
//
//    @Override
//    public IXConnection makeObject(EloConnectionDetails eloConnectionDetails) throws Exception {
//        IXConnection obj = eloConnectionDetails.createConnection();
//        //System.out.println("makeObject - eloConnectionDetails:" + eloConnectionDetails.hashCode() + "(" +System.identityHashCode(eloConnectionDetails) + ")" + ",IXConnection:" + obj.hashCode());
//        return obj;
//    }
//
//    @Override
//    public void destroyObject(EloConnectionDetails eloConnectionDetails, IXConnection obj) throws Exception {
//        //System.out.println("destroyObject - eloConnectionDetails:" + eloConnectionDetails.hashCode() + "(" +System.identityHashCode(eloConnectionDetails) + ")" + ",IXConnection:" + obj.hashCode());
//        eloConnectionDetails.destroyConnection(obj);
//    }
//
//    @Override
//    public boolean validateObject(EloConnectionDetails eloConnectionDetails, IXConnection obj) {
//        //System.out.println("validateObject - eloConnectionDetails:" + eloConnectionDetails.hashCode() + "(" +System.identityHashCode(eloConnectionDetails) + ")" + ",IXConnection:" + obj.hashCode());
//        return eloConnectionDetails.isValidConnection(obj);
//    }
//
//    @Override
//    public void activateObject(EloConnectionDetails eloConnectionDetails, IXConnection obj) throws Exception {
//        //System.out.println("activateObject - eloConnectionDetails:" + eloConnectionDetails.hashCode() + "(" +System.identityHashCode(eloConnectionDetails) + ")" + ",IXConnection:" + obj.hashCode());
//        //DO NOTHING
//    }
//
//    @Override
//    public void passivateObject(EloConnectionDetails eloConnectionDetails, IXConnection obj) throws Exception {
//        //System.out.println("passivateObject - eloConnectionDetails:" + eloConnectionDetails.hashCode() + "(" +System.identityHashCode(eloConnectionDetails) + ")" + ",IXConnection:" + obj.hashCode());
//        //DO NOTHING
//    }
//
//
//}
