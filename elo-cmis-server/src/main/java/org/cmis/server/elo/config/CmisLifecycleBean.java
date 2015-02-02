package org.cmis.server.elo.config;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.cmis.util.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static String SERVLET_CODE = "servletCode";
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE = "elo.cmis.${" + SERVLET_CODE + "}.configuration.profile";
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER = "elo.cmis.${" + SERVLET_CODE + "}.configuration.server";
    private static String ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J = "elo.cmis.${" + SERVLET_CODE + "}.configuration.log4j";

    @Autowired
    private ServletContext servletContext;
    @Autowired
    private CmisServiceFactory factory;
    //@Value("${config.filename}")
    //private String configFilename;
    //private String configFilenameAbsolute;

    private String servletCode;

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

    private String getSystemPropertyName(String propertyTemplate) {
        return TemplateEngine.getInstance().getValueFromTemplate(propertyTemplate, SERVLET_CODE, servletCode);
    }

    private String getSystemPropertyValue(String propertyTemplate) {
        String propertyName = getSystemPropertyName(propertyTemplate);
        //TemplateEngine.getInstance().getValueFromTemplate(propertyTemplate, SERVLET_CODE, servletCode);
        return System.getProperty(propertyName);
    }

    /**
     * Load properties
     *
     * @return a map with the properties defined for the server
     */
    private Map<String, String> readProperties() {
        Map<String, String> parameters = new HashMap<>();
        InputStream streamProperties = null;
        String configurationProfileName = null;
        String configurationServiceFileName = null;
        String configurationLog4JFileName = null;
        String environmentPropertyServletCode = null;
        this.servletCode = "servlet";
        try {
            String servletContextRealPath = servletContext.getRealPath("/");
            environmentPropertyServletCode = servletContextRealPath.replace(":", "").replace("/", ".").replace("\\", ".");
            if (environmentPropertyServletCode.endsWith(".")) {
                environmentPropertyServletCode = environmentPropertyServletCode.substring(0, environmentPropertyServletCode.length() - 1);
            }
            if (environmentPropertyServletCode.startsWith(".")) {
                environmentPropertyServletCode = environmentPropertyServletCode.substring(1);
            }
            if (StringUtils.isNotEmpty(getSystemPropertyValue(environmentPropertyServletCode))) {
                this.servletCode = getSystemPropertyValue(environmentPropertyServletCode);
            }
            configurationProfileName = getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE);
            configurationServiceFileName = getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER);
            configurationLog4JFileName = getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J);

            //get logging configuration
            try {
                if (configurationProfileName == null || configurationProfileName.isEmpty()) {
                    //este config extern
                    //configurationLog4JFileName ramane asa cum este dat
                } else {
                    //este config intern
                    if (configurationLog4JFileName != null) {
                        configurationLog4JFileName = servletContextRealPath + "/profiles" + (configurationProfileName.startsWith("/") ? "" : "/") + configurationProfileName + (configurationLog4JFileName.startsWith("/") ? "" : "/") + configurationLog4JFileName;
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

        System.out.println("Starting CMIS enviroment  <" + environmentPropertyServletCode + ">");

        Logger LOG = LoggerFactory.getLogger(CmisLifecycleBean.class);
        LOG.info("Starting CMIS enviroment  <" + environmentPropertyServletCode + ">");
        LOG.info("Configuration:");
        LOG.info("Instance name:" + this.servletCode);
        LOG.info("    -D" + environmentPropertyServletCode + "=" + this.servletCode);
        LOG.info("Profile: " + configurationProfileName);
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE) + "=" + configurationProfileName);
        LOG.info("Service configuration: " + configurationServiceFileName);
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER) + "=" + configurationServiceFileName);
        LOG.info("Log4j configuration: " + configurationLog4JFileName);
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J) + "=" + configurationLog4JFileName);

        return parameters;
    }

    @Override
    public void destroy() throws Exception {
        if (factory != null) {
            factory.destroy();
        }
    }
}