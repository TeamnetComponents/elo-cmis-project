package org.cmis.config;

import org.apache.log4j.PropertyConfigurator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Lucian.Dragomir on 1/7/2015.
 */
public class Log4JConfig extends HttpServlet {
    public
    void init() {
        String prefix =  getServletContext().getRealPath("/");
        String file = getInitParameter("log4j-init-file");
        // if the log4j-init-file is not set, then no point in trying
        if(file != null) {
            PropertyConfigurator.configure(prefix + file);
        }
    }

    public
    void doGet(HttpServletRequest req, HttpServletResponse res) {
    }
}
