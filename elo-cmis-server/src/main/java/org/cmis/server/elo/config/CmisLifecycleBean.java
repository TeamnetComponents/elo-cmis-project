package org.cmis.server.elo.config;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.platform.common.utils.template.TemplateEngine;
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
 * Set up OpenCMIS services cmisServiceFactory.
 */

@Component
public class CmisLifecycleBean implements ServletContextAware, InitializingBean, DisposableBean {
    private static final String SERVLET_NAME = "servlet";
    private static final String SERVLET_CODE = "servletCode";
    private static final String ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE = "elo.cmis.${" + SERVLET_CODE + "}.configuration.profile";
    private static final String ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER = "elo.cmis.${" + SERVLET_CODE + "}.configuration.server";
    private static final String ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J = "elo.cmis.${" + SERVLET_CODE + "}.configuration.log4j";

    @Autowired
    private ServletContext servletContext;
    @Autowired
    private CmisServiceFactory cmisServiceFactory;

    private Map<String, String> parameters;


    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setCmisServiceFactory(CmisServiceFactory factory) {
        this.cmisServiceFactory = factory;
    }

    public String getServletCode() {
        String applicationPath = getApplicationPath();
        if (StringUtils.isNotEmpty(getSystemPropertyValue(applicationPath))) {
            return getSystemPropertyValue(applicationPath);
        }
        return SERVLET_NAME;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getSystemPropertyName(String propertyTemplate) {
        String[] items = new String[0];
        if (propertyTemplate.contains(SERVLET_CODE)) {
            items = new String[]{SERVLET_CODE, getServletCode()};
        }
        return TemplateEngine.getInstance().getValueFromTemplate(propertyTemplate, items);
    }

    public String getSystemPropertyValue(String propertyTemplate) {
        String propertyName = getSystemPropertyName(propertyTemplate);
        return System.getProperty(propertyName);
    }

    private String getApplicationPath() {
        String applicationPath;

        applicationPath = servletContext.getRealPath("/").replace(":", "").replace("/", ".").replace("\\", ".");
        if (applicationPath.endsWith(".")) {
            applicationPath = applicationPath.substring(0, applicationPath.length() - 1);
        }
        if (applicationPath.startsWith(".")) {
            applicationPath = applicationPath.substring(1);
        }
        return applicationPath;
    }

    private String getProfileName() {
        return getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE);
    }

    private String getServiceFileName() {
        return getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER);
    }

    private String getLog4JFileName() {
        return getSystemPropertyValue(ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J);
    }

    private void initLog4JConfiguration() {
        String configurationProfileName = getProfileName();
        String configurationLog4JFileName = getLog4JFileName();
        try {
            if (configurationProfileName == null || configurationProfileName.isEmpty()) {
                //este config extern
                //configurationLog4JFileName ramane asa cum este dat
            } else {
                //este config intern
                if (configurationLog4JFileName != null) {
                    configurationLog4JFileName = servletContext.getRealPath("/") + "/profiles" + (configurationProfileName.startsWith("/") ? "" : "/") + configurationProfileName + (configurationLog4JFileName.startsWith("/") ? "" : "/") + configurationLog4JFileName;
                }
            }
            if (configurationLog4JFileName != null) {
                PropertyConfigurator.configure(configurationLog4JFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void initServiceConfiguration() {
        String configurationProfileName = getProfileName();
        String configurationServiceFileName = getServiceFileName();

        Map<String, String> fileParameters = new HashMap<String, String>();

        //get service configuration
        InputStream streamProperties = null;
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
            fileParameters.put(key, value);
        }

        setParameters(fileParameters);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (cmisServiceFactory != null) {

            printApplicationProperties();

            initLog4JConfiguration();
            initServiceConfiguration();

            cmisServiceFactory.init(getParameters());
            servletContext.setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, cmisServiceFactory);
        }
    }

    private void printApplicationProperties() {

        System.out.println("Starting CMIS enviroment  <" + getServletCode() + ">");

        Logger LOG = LoggerFactory.getLogger(CmisLifecycleBean.class);
        LOG.info("Starting CMIS enviroment  <" + getServletCode() + ">");
        LOG.info("Configuration:");
        LOG.info("Instance name:" + getServletCode());
        LOG.info("    -D" + getApplicationPath() + "=" + getServletCode());
        LOG.info("Profile: " + getProfileName());
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_PROFILE) + "=" + getProfileName());
        LOG.info("Service configuration: " + getServiceFileName());
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_SERVER) + "=" + getServiceFileName());
        LOG.info("Log4j configuration: " + getServiceFileName());
        LOG.info("    -D" + getSystemPropertyName(ENVIRONMENT_PARAMETER_CONFIGURATION_LOG4J) + "=" + getLog4JFileName());
    }

    @Override
    public void destroy() throws Exception {
        if (cmisServiceFactory != null) {
            cmisServiceFactory.destroy();
        }
        if (parameters != null) {
            parameters.clear();
        }
    }
}