package org.cmis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;

/**
 * Created by Lucian.Dragomir on 6/4/2014.
 */
@Component
public class CmisServiceParameters {
    private static final Logger LOG = LoggerFactory.getLogger(CmisServiceParameters.class);

    private Map<String, String> parameters;

    private CmisServiceParameters() {
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public CmisServiceParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public BigInteger getBigIntegerParameter(String key, BigInteger def) {
        String value = parameters.get(key);
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return new BigInteger(value);
    }

    public Long getLongParameter(String key, Long def) {
        String value = parameters.get(key);
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return new Long(value);
    }

    public Integer getIntegerParameter(String key, Integer def) {
        String value = parameters.get(key);
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return new Integer(value);
    }

    public String getStringParameter(String key, String def) {
        String value = parameters.get(key);
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return value;
    }

    public Boolean getBooleanParameter(String key, Boolean def) {
        String value = parameters.get(key);
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return Boolean.parseBoolean(value);
    }
}
