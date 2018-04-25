/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.network.http.JettySync;
import com.pearson.eidetic.utilities.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class JettyThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JettyThread.class.getName());
    private static JettySync jettySync_;

    @Override
    public void run() {
        try {
            logger.info("Starting Jetty Server Process");
            jettySync_ = new JettySync(ApplicationConfiguration.getSyncServerHttpListenerPort(), 30000);
            jettySync_.startServer();
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            logger.info("Unable to launch jetty Server. Shutting down application");
            System.exit(-1);
        }
    }

}
