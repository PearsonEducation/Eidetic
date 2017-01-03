/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.network.http;

import com.pearson.eidetic.api.SyncSnapshot;
import com.pearson.eidetic.network.JettyServer;
import com.pearson.eidetic.utilities.StackTrace;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettySync implements JettyServer {

    private static final Logger logger = LoggerFactory.getLogger(JettySync.class.getName());

    private final int port_;
    private final int stopServerTimeout_;
    private Server jettyServer_;

    public JettySync(int port, int stopServerTimeout) {
        port_ = port;
        stopServerTimeout_ = stopServerTimeout;
    }

    public void startServer() {

        try {
            jettyServer_ = new Server(port_);
            jettyServer_.setStopAtShutdown(true);
            jettyServer_.setStopTimeout(stopServerTimeout_);
            ServletHandler handler = new ServletHandler();
            jettyServer_.setHandler(handler);
            handler.addServletWithMapping(SyncSnapshot.class, "/api/syncsnapshot");
            jettyServer_.start();
            jettyServer_.join();
        } catch (Exception e) {
            if (e.toString().contains("Permission")) {
                logger.error("Cannot start jetty on port " + port_ + ". Try running as superuser or change port to be above port 1024."
                        + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            } else {
                logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
    }

    @Override
    public void stopServer() {
        try {
            jettyServer_.stop();
            jettyServer_ = null;
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
    }

}
