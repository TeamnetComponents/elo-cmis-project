package org.cmis.server.elo.config;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author andreeaf
 * @since 6/27/14 11:29 AM
 * <p/>
 * Set up OpenCMIS services factory.
 */
@Component
public class CmisLifecycleBean implements ServletContextAware, InitializingBean, DisposableBean {
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE = "elo.cmis.configuration.profile";
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER = "elo.cmis.configuration.server";
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J = "elo.cmis.configuration.log4j";

    @Autowired
    private ServletContext servletContext;
    @Autowired
    private CmisServiceFactory factory;
    @Value("${config.filename}")
    private String configFilename;
    private String configFilenameAbsolute;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setCmisServiceFactory(CmisServiceFactory factory) {
        this.factory = factory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (factory != null) {
            Map<String, String> props = readProperties();
            factory.init(props);
            servletContext.setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, factory);
        }
    }


    /**
     * Load properties
     *
     * @return a map with the properties defined for the server
     */
    private Map<String, String> readProperties() {
        Map<String, String> parameters = new HashMap<>();
        InputStream streamProperties = null;
        try {
            String configurationProfileName = System.getProperty(ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE);
            String configurationServiceFileName = System.getProperty(ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER);
            String configurationLog4JFileName = System.getProperty(ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J);

            //get logging configuration
            try {
                if (configurationProfileName == null || configurationProfileName.isEmpty()) {
                    //este config extern
                    //configurationLog4JFileName ramane asa cum este dat
                } else {
                    //este config intern
                    if (configurationLog4JFileName != null) {
                        configurationLog4JFileName = "/profiles" + (configurationProfileName.startsWith("/") ? "" : "/") + configurationProfileName + (configurationLog4JFileName.startsWith("/") ? "" : "/") + configurationLog4JFileName;
                    }
                }
                if (configurationLog4JFileName != null) {
                    PropertyConfigurator.configure(configurationLog4JFileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //get service configuration
            try {
                if (configurationProfileName == null || configurationProfileName.isEmpty()) {
                    //este config extern
                    streamProperties = new FileInputStream(configurationServiceFileName);
                } else {
                    //este config intern
                    configurationServiceFileName = "/WEB-INF/classes/profiles" + (configurationProfileName.startsWith("/") ? "" : "/") + configurationProfileName + (configurationServiceFileName.startsWith("/") ? "" : "/") + configurationServiceFileName;
                    streamProperties = servletContext.getResourceAsStream(configurationServiceFileName);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (streamProperties == null) {
                throw new IllegalStateException("Cannot find configuration for file <" + configurationServiceFileName + ">!");
            }

            Properties props = new Properties();
            try {
                props.load(streamProperties);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot find configuration for file <" + configurationServiceFileName + ">!");
            } finally {
                IOUtils.closeQuietly(streamProperties);
            }

            for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String value = props.getProperty(key);
                parameters.put(key, value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return parameters;
    }

    @Override
    public void destroy() throws Exception {
        if (factory != null) {
            factory.destroy();
        }
    }
}