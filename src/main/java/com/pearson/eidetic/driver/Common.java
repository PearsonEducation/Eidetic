/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class Common {
   private static final Logger logger = LoggerFactory.getLogger(Common.class.getName());

    public static ClientConfiguration setClientConfigurationSettings(ClientConfiguration clientConfiguration) {
        clientConfiguration.setConnectionTimeout(10000);
        clientConfiguration.setMaxConnections(10000);
        clientConfiguration.setProtocol(Protocol.HTTPS);
        clientConfiguration.setSocketTimeout(60000);
        
        return clientConfiguration;
    } 
}
