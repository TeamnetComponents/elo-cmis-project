package org.cmis.server.elo;

import de.elo.extension.connection.IXPoolableConnectionManager;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.MutableCallContext;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CmisServiceWrapperManager;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.cmis.util.CmisServiceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.Map;

/**
 * ELO Service Factory.
 */

@Component
public class EloCmisServiceFactory extends AbstractServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisServiceFactory.class);

    private static final String SERVICE_TEMP_DIRECTORY = "service.tempDirectory";
    private static final String SERVICE_MEMORY_THERESHOLD = "service.memoryThreshold";
    private static final String SEVICE_MAX_CONTENT_SIZE = "service.maxContentSize";

    private static final String SERVICE_DEFAULT_MAX_ITEMS_OBJECTS = "service.defaultMaxItems";
    private static final String SERVICE_DEFAULT_DEPTH_OBJECTS = "service.defaultDepth";
    private static final String SERVICE_DEFAULT_MAX_ITEMS_TYPES = "service.defaultTypesMaxItems";
    private static final String SERVICE_DEFAULT_DEPTH_TYPES = "service.defaultTypesDepth";

    private ThreadLocal<CallContextAwareCmisService> threadLocalService = new ThreadLocal<CallContextAwareCmisService>();

    private CmisServiceWrapperManager cmisServiceWrapperManager;
    private CmisServiceParameters serviceParameters;
    private EloCmisConnectionManager eloCmisConnectionManager;

    private BigInteger defaultMaxItems;
    private BigInteger defaultDepth;
    private BigInteger defaultTypesMaxItems;
    private BigInteger defaultTypesDepth;
    private File tempDirectory;
    private int memoryThreshold;
    private long maxContentSize;

    @Autowired
    public EloCmisServiceFactory(final CmisServiceWrapperManager cmisServiceWrapperManager,
                                 final CmisServiceParameters cmisServiceParameters) {
        this.cmisServiceWrapperManager = cmisServiceWrapperManager;
        this.serviceParameters = cmisServiceParameters;
    }

    @Override
    public void init(Map<String, String> parameters) {
        // set received parameters
        this.serviceParameters.setParameters(parameters);
        //this.serviceParameters = new CmisServiceParameters(parameters);

        this.eloCmisConnectionManager = new EloCmisConnectionManager(this.serviceParameters);

        // get configuration parameters from repository.properties file
        try {
            String tempDirectoryString = serviceParameters.getStringParameter(SERVICE_TEMP_DIRECTORY, null);
            this.tempDirectory = (tempDirectoryString == null || tempDirectoryString.trim().length() == 0 ? super.getTempDirectory() : new File(tempDirectoryString.trim()));
            this.memoryThreshold = serviceParameters.getIntegerParameter(SERVICE_MEMORY_THERESHOLD, super.getMemoryThreshold());
            this.maxContentSize = serviceParameters.getLongParameter(SEVICE_MAX_CONTENT_SIZE, super.getMaxContentSize());

            this.defaultMaxItems = serviceParameters.getBigIntegerParameter(SERVICE_DEFAULT_MAX_ITEMS_OBJECTS, null);
            this.defaultDepth = serviceParameters.getBigIntegerParameter(SERVICE_DEFAULT_DEPTH_OBJECTS, null);
            this.defaultTypesMaxItems = serviceParameters.getBigIntegerParameter(SERVICE_DEFAULT_MAX_ITEMS_TYPES, null);
            this.defaultTypesDepth = serviceParameters.getBigIntegerParameter(SERVICE_DEFAULT_DEPTH_TYPES, null);
        } catch (NumberFormatException e) {
            throw new CmisRuntimeException("Could not parse " + this.getClass().getSimpleName() + " configuration values: " + e.getMessage(), e);
        }

        // set wrapper manager
//        this.cmisServiceWrapperManager = new CmisServiceWrapperManager();
        this.cmisServiceWrapperManager.addWrappersFromServiceFactoryParameters(parameters);
        this.cmisServiceWrapperManager.addOuterWrapper(ConformanceCmisServiceWrapper.class, defaultTypesMaxItems, defaultTypesDepth, defaultMaxItems, defaultDepth);
    }

    @Override
    public void destroy() {
        this.threadLocalService = null;
        super.destroy();
    }

    @Override
    public CmisService getService(CallContext context) {
        CallContextAwareCmisService service;
        try {
            // get service object for this thread
            service = threadLocalService.get();
            if (service == null) {
                EloCmisService eloCmisService = new EloCmisService(this.eloCmisConnectionManager);

                // wrap it with the chain of wrappers
                service = (CallContextAwareCmisService) cmisServiceWrapperManager.wrap(eloCmisService);
                threadLocalService.set(service);
            }

            // Stash any object into the call context and then pass it to our service
            // so that it can be shared with any extensions.
            // Here is where you would put in a reference to a native api object if needed.
            MutableCallContext mutableCallContext = (MutableCallContext) context;
            service.setCallContext(mutableCallContext);

        } catch (Exception e) {
            e.printStackTrace();
            throw new CmisConnectionException(e.getMessage());
        }
        return service;

    }

    protected CmisServiceParameters getServiceParameters() {
        return serviceParameters;
    }

    @Override
    public File getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    @Override
    public long getMaxContentSize() {
        return maxContentSize;
    }

    @Override
    public boolean encryptTempFiles() {
        return super.encryptTempFiles();
    }
}