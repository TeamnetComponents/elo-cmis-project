package org.elo.jmeter;

import de.elo.extension.connection.EloUtilsConnection;
import de.elo.ix.client.IXConnection;
import de.elo.utils.net.RemoteException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.platform.common.utils.file.FileUtils;

import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 11/13/2014.
 */
public abstract class BasicSamplerClient extends AbstractJavaSamplerClient {
    protected static FileUtils fileUtilsOS = new FileUtils(getRoot(), System.getProperty("file.separator"));
    protected static FileUtils fileUtilsDMS = new FileUtils("ARCPATH:", String.valueOf((char) 182));
    protected static FileUtils fileUtilsRegular = new FileUtils("/", "/");

    public static String ELO_SERVICE_FILE_NAME = "ELO_SERVICE_FILE_NAME";
    public static String PATH_LOCAL = "PATH_LOCAL";
    public static String PATH_DMS = "PATH_DMS";
    public static String RUN_USER = "RUN_USER";
    public static String RUN_PASSWORD = "RUN_PASSWORD";
    public static String PROCESS_FILE_PATH = "PROCESS_FILE_PATH";

    protected IXConnection ixConnection;
    protected Properties generalProperties;
    protected String lastUser;

    private static String getRoot() {
        String root = "";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            root = "C:";
        } else {
            root = System.getProperty("file.separator");
        }
        return root;
    }

    public IXConnection getIXConnection(String fileConfiguration) {
        return null;
    }

    protected void refreshConnection(String user, String password) {
        if (StringUtils.equals(lastUser, user) && (ixConnection != null)) {
            //do nothing
        } else {
            Properties overwriteProperties = null;
            if (StringUtils.isNotEmpty(user)) {
                overwriteProperties = EloUtilsConnection.createCredentialsBasic(user, password);
            }

            //destroy previous connection (if exists)
            try {
                EloUtilsConnection.destroyConnection(ixConnection);
            } catch (Exception e) {
                e.printStackTrace();

            }

            //create new connection (using new credentials - user/password)
            try {
                ixConnection = EloUtilsConnection.createIXConnection(overwriteProperties, generalProperties);
            } catch (RemoteException e) {
                ixConnection = null;
                e.printStackTrace();
            }
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        String pathLocal = context.getParameter(PATH_LOCAL);
        String pathDMS = context.getParameter(PATH_DMS);
        String user = context.getParameter(RUN_USER);
        String password = context.getParameter(RUN_PASSWORD);
        String processFilePath = context.getParameter(PROCESS_FILE_PATH);

        throw new UnsupportedOperationException();
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        lastUser = null;
        try {
            System.out.println("BasicSamplerClient-setupTest-ResourceFile: " + context.getParameter(ELO_SERVICE_FILE_NAME));
            if (StringUtils.isNotEmpty(context.getParameter(ELO_SERVICE_FILE_NAME))) {
                generalProperties = FileUtils.openOsResource(context.getParameter(ELO_SERVICE_FILE_NAME));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            EloUtilsConnection.destroyConnection(ixConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (generalProperties != null) {
            generalProperties.clear();
        }
    }





/*




    public static DocumentIdentifier testUploadDocument(StoreService storeService, StoreContext storeContext, String destinationPathNameRoot, String sourcePathNameRoot, String sourceDocumentPathName, Map<String, Object> documentProperties, String documentType) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(sourceDocumentPathName));
            String sourceDocumentPathNameRelative = fileUtilsOS.convertPathNameWithoutNormalize(sourceDocumentPathName.substring(sourcePathNameRoot.length()), fileUtilsDMS);
            String destinationDocumentPathName = destinationPathNameRoot + sourceDocumentPathNameRelative;
            String destinationPathName = fileUtilsDMS.getParentFolderPathName(destinationDocumentPathName);

            DocumentIdentifier documentIdentifier = DocumentIdentifier.builder().withPath(destinationDocumentPathName).build();
            DocumentInfo documentInfo = new DocumentInfo(destinationPathName, destinationDocumentPathName, documentType, documentProperties);
            DocumentIdentifier documentIdentifierStored = storeService.storeDocument(storeContext, documentInfo, inputStream, true, VersioningType.MAJOR);
            return documentIdentifierStored;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    public static DocumentIdentifier testUploadDocument(SampleResult sampleResult, StoreService storeService, StoreContext storeContext, String destinationPathNameRoot, String sourcePathNameRoot, String sourceDocumentPathName, Map<String, Object> documentProperties, String documentType) throws Exception {
        InputStream inputStream = null;
        try {
            byte[] bytes = IOUtils.toByteArray(new FileInputStream(new File(sourceDocumentPathName)));
            inputStream = new ByteArrayInputStream(bytes);

            sampleResult.sampleStart();
            String sourceDocumentPathNameRelative = fileUtilsOS.convertPathNameWithoutNormalize(sourceDocumentPathName.substring(sourcePathNameRoot.length()), fileUtilsDMS);
            String destinationDocumentPathName = destinationPathNameRoot + sourceDocumentPathNameRelative;
            String destinationPathName = fileUtilsDMS.getParentFolderPathName(destinationDocumentPathName);

            DocumentIdentifier documentIdentifier = DocumentIdentifier.builder().withPath(destinationDocumentPathName).build();
            DocumentInfo documentInfo = new DocumentInfo(destinationPathName, destinationDocumentPathName, documentType, documentProperties);
            DocumentIdentifier documentIdentifierStored = storeService.storeDocument(storeContext, documentInfo, inputStream, true, VersioningType.MAJOR);

            //set ok info
            sampleResult.setResponseData(documentIdentifierStored.toString(), null);
            sampleResult.setSuccessful(true);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessageOK();

            return documentIdentifierStored;
        } catch (Exception e) {
            //set error info
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode("500");
            sampleResult.setResponseMessage(ExceptionUtils.getStackTrace(e));

            throw e;
        } finally {

            try {
                sampleResult.sampleEnd();
            } catch (Exception e) {
                //do nothing
            }
            try {
                inputStream.close();
            } catch (Exception e) {
                //do nothing
            }

        }
    }


    public static String testDownloadDocument(SampleResult sampleResult, StoreService storeService, StoreContext storeContext, String destinationPathNameRoot, String sourcePathNameRoot, String sourceDocumentPathName) throws Exception {
        InputStream inputStream = null;
        try {
            sampleResult.sampleStart();

            String filePartName = sourceDocumentPathName.substring(sourcePathNameRoot.length());

            String sourceDocumentPathNameRelative = fileUtilsDMS.convertPathNameWithoutNormalize(filePartName, fileUtilsOS);
            String destinationDocumentPathName = destinationPathNameRoot + sourceDocumentPathNameRelative;
            String destinationPathName = fileUtilsDMS.getParentFolderPathName(destinationDocumentPathName);

            DocumentIdentifier documentIdentifier = DocumentIdentifier.builder().withPath(sourceDocumentPathName).build();
            DocumentInfo documentInfo = storeService.getDocumentInfo(storeContext, documentIdentifier);
            DocumentStream documentStream = storeService.downloadDocument(storeContext, documentIdentifier);
            inputStream = documentStream.getInputStream();
            File targetFile = new File(destinationDocumentPathName);
            org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, targetFile);

            //set ok info
            sampleResult.setResponseData(destinationDocumentPathName.toString(), null);
            sampleResult.setSuccessful(true);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessageOK();

            return destinationDocumentPathName;
        } catch (Exception e) {
            //set error info
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode("500");
            sampleResult.setResponseMessage(ExceptionUtils.getStackTrace(e));
            throw e;
        } finally {

            try {
                sampleResult.sampleEnd();
            } catch (Exception e) {
                //do nothing
            }
            try {
                inputStream.close();
            } catch (Exception e) {
                //do nothing
            }

        }

    }


    public static String testDownloadDocument(StoreService storeService, StoreContext storeContext, String destinationPathNameRoot, String sourcePathNameRoot, String sourceDocumentPathName) throws Exception {

        String filePartName = sourceDocumentPathName.substring(sourcePathNameRoot.length());

        String sourceDocumentPathNameRelative = fileUtilsDMS.convertPathNameWithoutNormalize(filePartName, fileUtilsOS);
        String destinationDocumentPathName = destinationPathNameRoot + sourceDocumentPathNameRelative;
        String destinationPathName = fileUtilsDMS.getParentFolderPathName(destinationDocumentPathName);

        DocumentIdentifier documentIdentifier = DocumentIdentifier.builder().withPath(sourceDocumentPathName).build();
        DocumentInfo documentInfo = storeService.getDocumentInfo(storeContext, documentIdentifier);
        DocumentStream documentStream = storeService.downloadDocument(storeContext, documentIdentifier);

        File targetFile = new File(destinationDocumentPathName);
        org.apache.commons.io.FileUtils.copyInputStreamToFile(documentStream.getInputStream(), targetFile);
        return destinationDocumentPathName;
    }


    public static void main(String[] args) throws Exception {
        StoreService storeService = null;
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-fo.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-jms.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-local.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-sibiac.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-jms-prod.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\ss-pos-fo-staging.properties");
        //storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\siamc-ss-integration.properties");
        storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\siamc-ss-fo-db.properties");


//        StoreServiceImpl_Integration storeServiceImpl_integration = (StoreServiceImpl_Integration) storeService;
//
//        storeServiceImpl_integration.getIntegrationService().addStoreServiceMessageListener(new StoreServiceMessageListener() {
//            @Override
//            public void onReceive(StoreServiceMessageEvent storeServiceMessageEvent) {
//                // croco based code
//
//            }
//        });


        StoreContext storeContext = StoreContext.builder()
                .communicationType(StoreContext.COMMUNICATION_TYPE_VALUES.SYNCHRONOUS)
                        //.loginAs("Mihai.Viscea")
                .build();
        String destinationPathNameRoot = "/JMeter/upload3";
        String sourcePathNameRoot = "C:\\__JAVA\\jmeter\\test\\dmsutils\\upload";
        String sourceDocumentPathName = sourcePathNameRoot + "\\" + "db2.pdf";
        Map<String, Object> documentProperties = null;
        String documentType = null;
        documentType = "cmis:document-mask-23";
        documentType = "cmis:document~mask~Cerere de finantare";
        documentType = "cmis:document~mask~Intrare elementarã";
        documentType = "cmis:document";


        DocumentIdentifier documentIdentifier = testUploadDocument(storeService, storeContext,
                destinationPathNameRoot,
                sourcePathNameRoot,
                sourceDocumentPathName,
                documentProperties, documentType);

        System.out.println(documentIdentifier.toString());

    }


    public static void main2(String[] args) throws Exception {
        StoreService storeService = getStoreService("C:\\__JAVA\\jmeter\\test\\dmsutils\\cmis");
        StoreContext storeContext = StoreContext.builder().build();
        String sourcePathNameRoot = "/JMeter/upload";
        String destinationPathNameRoot = "C:\\__JAVA\\jmeter\\test\\dmsutils\\download";
        String sourceDocumentPathName = sourcePathNameRoot + "/" + "db2";
        Map<String, Object> documentProperties = null;
        String documentType = "cmis:document";

        String filePathName = testDownloadDocument(storeService, storeContext,
                destinationPathNameRoot,
                sourcePathNameRoot,
                sourceDocumentPathName);

    }

    public static void mainr(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        String fileStoreService = "C:\\__JAVA\\jmeter\\test\\dmsutils\\cmis-local";
        //StoreService storeService = getStoreService(fileStoreService);
        StoreContext storeContext = StoreContext.builder().build();
        String sourcePathNameRoot = "/JMeter/upload";
        sourcePathNameRoot = "/Proiecte/Dosar3954";
        String fileList = "C:\\__JAVA\\jmeter\\test\\dmsutils\\fileList.csv";
        JmeterFileUtils.listFileNames(sourcePathNameRoot, fileStoreService, fileList);
    }
*/
}
