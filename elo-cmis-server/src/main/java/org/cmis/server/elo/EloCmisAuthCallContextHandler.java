package org.cmis.server.elo;

import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.cmis.server.elo.commons.EloCmisContextParameter.ELO_CMIS_CONTEXT_PARAMETER_PREFIX;

/**
 * @author andreeaf
 * @since 9/8/2014 3:45 PM
 */
public class EloCmisAuthCallContextHandler extends BasicAuthCallContextHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisAuthCallContextHandler.class);

    @Override
    public Map<String, String> getCallContextMap(HttpServletRequest request) {
        Map<String, String> callContextMap = super.getCallContextMap(request);

        //pass through all header parameters starting with ELO_CMIS_CONTEXT_PARAMETER_PREFIX
        if (callContextMap == null || callContextMap.isEmpty()) {
            callContextMap = new HashMap<>();
        }
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (headerName.startsWith(ELO_CMIS_CONTEXT_PARAMETER_PREFIX)) {
                callContextMap.put(headerName, request.getHeader(headerName));
            }
        }

        return callContextMap;
    }
}
