package org.elo.jmeter;

import de.elo.extension.connection.EloUtilsConnection;
import de.elo.extension.service.EloUtilsService;
import de.elo.ix.client.IXConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.platform.common.utils.file.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 4/22/2015.
 */
public class FilesUpload extends BasicSamplerClient {

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(ELO_SERVICE_FILE_NAME, "");
        defaultParameters.addArgument(PATH_LOCAL, "");
        defaultParameters.addArgument(PATH_DMS, "");
        defaultParameters.addArgument(RUN_USER, "");
        defaultParameters.addArgument(RUN_PASSWORD, "");
        defaultParameters.addArgument(PROCESS_FILE_PATH, "");
        return defaultParameters;
    }


    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult sampleResult = new SampleResult();

        //start counters
        //sampleResult.sampleStart();

        //init variables

        InputStream inputStream = null;
        boolean allowCreatePath = true;
        String documentIdentifier = null;
        Map<String, Object> documentProperties = null;
        int maskId = 0;

        String pathLocal = context.getParameter(PATH_LOCAL);
        String pathDMS = context.getParameter(PATH_DMS);
        String user = context.getParameter(RUN_USER);
        String password = context.getParameter(RUN_PASSWORD);
        String processFilePath = context.getParameter(PROCESS_FILE_PATH);


        try {
            sampleResult.sampleStart();
            refreshConnection(user, password);
            documentIdentifier = fileUpload(ixConnection, pathDMS, pathLocal, processFilePath, documentProperties, maskId);

            //set ok info
            sampleResult.setResponseData(documentIdentifier, null);
            sampleResult.setSuccessful(true);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessageOK();

        } catch (Exception e) {
            //set error info
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode("500");
            sampleResult.setResponseMessage(ExceptionUtils.getStackTrace(e));

        } finally {
            sampleResult.sampleEnd();
        }
        return sampleResult;
    }


    public static String fileUpload(IXConnection ixConnection, String destinationPathNameRoot, String sourcePathNameRoot, String sourceDocumentPathName, Map<String, Object> documentProperties, int maskId) throws Exception {
        String documentIdentifier = null;
        InputStream inputStream = null;
        int inputStreamLength = 0;
        try {
            String destinationPathId;
            byte[] bytes = IOUtils.toByteArray(new FileInputStream(new File(sourceDocumentPathName)));
            inputStream = new ByteArrayInputStream(bytes);
            inputStreamLength = bytes.length;

            destinationPathNameRoot = fileUtilsRegular.convertPathName(destinationPathNameRoot,fileUtilsDMS);
            String sourceDocumentPathNameRelative = fileUtilsOS.convertPathNameWithoutNormalize(sourceDocumentPathName.substring(sourcePathNameRoot.length()), fileUtilsDMS);
            String destinationDocumentPathName = destinationPathNameRoot + sourceDocumentPathNameRelative;
            String destinationPathName = fileUtilsDMS.getParentFolderPathName(destinationDocumentPathName);

            //create parent folder tree (if not exists)
            if (!EloUtilsService.existSord(ixConnection, destinationPathName)) {
                destinationPathId = EloUtilsService.createSord(ixConnection, destinationPathName, String.valueOf(maskId), documentProperties);
            } else {
                destinationPathId = destinationPathName;
            }

            //upload document
            documentIdentifier = EloUtilsService.uploadDocument(ixConnection, destinationPathId, fileUtilsDMS.getFileName(destinationDocumentPathName), maskId, fileUtilsDMS.getFileName(destinationDocumentPathName), inputStream, inputStreamLength, documentProperties, "new version", "JMeter uploaded at " + (new Date()));
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                //do nothing
            }
        }
        return documentIdentifier;
    }

    public static void main(String[] args) throws Exception {
        String documentIdentifier;
        String eloConnectionFileName;
        String destinationPathNameRoot;
        String sourcePathNameRoot;
        String sourceDocumentPathName;
        Map<String, Object> documentProperties = null;
        int maskId = 0;
        InputStream inputStream;

        //list ELO
        eloConnectionFileName = "C:\\__JAVA\\jmeter\\test\\elo\\elo-elofoamvip.properties";


        destinationPathNameRoot = "/JMeter/upload/elo/upload_test";
        sourcePathNameRoot = "C:\\__JAVA\\jmeter\\test\\elo\\upload";
        sourceDocumentPathName = "C:\\__JAVA\\jmeter\\test\\elo\\upload\\db2_3.pdf";

        IXConnection ixConnection = null;
        if (StringUtils.isNotEmpty(eloConnectionFileName)) {
            Properties generalProperties = FileUtils.openOsResource(eloConnectionFileName);
            ixConnection = EloUtilsConnection.createIXConnection(generalProperties);
        }

        byte[] bytes = IOUtils.toByteArray(new FileInputStream(new File(sourceDocumentPathName)));
        inputStream = new ByteArrayInputStream(bytes);

        documentIdentifier = fileUpload(ixConnection, destinationPathNameRoot, sourcePathNameRoot, sourceDocumentPathName, documentProperties, maskId);
        EloUtilsConnection.destroyConnection(ixConnection);
    }

}
