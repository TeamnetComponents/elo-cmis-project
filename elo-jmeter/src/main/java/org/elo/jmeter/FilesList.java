package org.elo.jmeter;

import de.elo.extension.connection.EloUtilsConnection;
import de.elo.ix.client.IXConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.platform.common.utils.file.FileUtils;

import java.util.Properties;

/**
 * Created by Lucian.Dragomir on 11/18/2014.
 */
public class FilesList extends BasicSamplerClient {
    private static String PATH_TO_DIR = "PATH_TO_DIR";
    private static String FILE_DESTINATION = "FILE_DESTINATION";

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(ELO_SERVICE_FILE_NAME, "");
        defaultParameters.addArgument(PATH_TO_DIR, "");
        defaultParameters.addArgument(FILE_DESTINATION, "");
        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        IXConnection ixConnection = null;

        String filePath = context.getParameter(PATH_TO_DIR);
        String fileDest = context.getParameter(FILE_DESTINATION);
        String storeServiceFile = context.getParameter(ELO_SERVICE_FILE_NAME);


        System.out.println("FilesList-runTest-ResourceFile: " + context.getParameter(ELO_SERVICE_FILE_NAME));


        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart(); // start stopwatch
        try {
            if (StringUtils.isNotEmpty(storeServiceFile)) {
                Properties generalProperties = FileUtils.openOsResource(storeServiceFile);
                ixConnection = EloUtilsConnection.createIXConnection(generalProperties);
            }
            JMeterFileUtils.listFileNames(JMeterFileUtils.listFileNames(filePath, ixConnection), fileDest);

            sampleResult.setResponseData(fileDest, null);
            sampleResult.setSuccessful(true);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessageOK();
        } catch (Exception e) {
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode("500");
            sampleResult.setResponseMessage(ExceptionUtils.getStackTrace(e));
        } finally {
            sampleResult.sampleEnd();
            try {
                EloUtilsConnection.destroyConnection(ixConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sampleResult;
    }

}
