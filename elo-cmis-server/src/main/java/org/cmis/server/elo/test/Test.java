package org.cmis.server.elo.test;

import de.elo.ix.client.*;
import de.elo.utils.net.RemoteException;

import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 6/14/2014.
 */
public class Test {
    private final static String IX_URL = "http://192.168.229.129:8080/ix-elo2/ix";
    private final static String LOGIN_NAME = "Administrator";
    private final static String LOGIN_PWD = "elo";
    private final static String CNN_NAME = "IX-Workshop";
    private static IXConnection ix;
    private static IXServicePortC CONS;
    private static EditInfoC EDIT_INFO;
    private static SordC SORD;
    private static LockC LOCK;
    private static ArchivingModeC ARCHIVING_MODE;
    private static DocMaskC DOC_MASK;
    private static DocMaskLineC DOC_MASK_LINE;

    public static void connectIx() throws RemoteException {
        Properties connProps = IXConnFactory.createConnProps(IX_URL);
        Properties sessOpts = IXConnFactory.createSessionOptions("IX-Example", "1.0");
        IXConnFactory connFact = new IXConnFactory(connProps, sessOpts);
        ix = connFact.create(LOGIN_NAME, LOGIN_PWD, CNN_NAME, null);

        // Get contstant values (cached by IXClient object)
        CONS = ix.getCONST();
        EDIT_INFO = CONS.getEDIT_INFO();
        SORD = CONS.getSORD();
        LOCK = CONS.getLOCK();
        ARCHIVING_MODE = CONS.getARCHIVING_MODE();
        DOC_MASK = CONS.getDOC_MASK();
        DOC_MASK_LINE = CONS.getDOC_MASK_LINE();

    }

    // Logout from IndexServer
    public static void disconnectIx() {
        if (ix != null) {
            ix.logout();
        }
        System.out.println("Logged off");
    }


    public static void main(String[] args) {
        try {
            // Login
            connectIx();

            // do the tests
            IXServicePortC CONST = ix.getCONST();
            Sord sord = ix.ix().checkoutSord("ARCPATH:\\Proiecte\\Dosar87778", EditInfoC.mbSord, LockC.NO).getSord();
            sord = ix.ix().checkoutSord("ARCPATH:\\Program\\RFI\\RFI_20.02.2012.08.26",SordC.mbAll, LockC.NO);
            ArcPath[] refPathses =  sord.getRefPaths();

            int i = 0;


        } catch (RemoteException e) {
            System.out.println(e.toString());

        } finally {

            // logout from Index Server
            disconnectIx();
        }
    }
}
