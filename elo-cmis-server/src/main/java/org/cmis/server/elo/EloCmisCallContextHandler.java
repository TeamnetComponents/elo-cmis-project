package org.cmis.server.elo;

import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author andreeaf
 * @since 9/8/2014 3:45 PM
 */
public class EloCmisCallContextHandler extends BasicAuthCallContextHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisCallContextHandler.class);

    @Override
    public Map<String, String> getCallContextMap(HttpServletRequest request) {
        super.getCallContextMap(request);
        Map<String, String> callContextMap = super.getCallContextMap(request);
        if (callContextMap == null || callContextMap.isEmpty()) {
            callContextMap = new HashMap<>();
        }

        Enumeration headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = (String) headerNames.nextElement();
            callContextMap.put(headerName, request.getHeader(headerName));
        }

        return callContextMap;
    }
}
