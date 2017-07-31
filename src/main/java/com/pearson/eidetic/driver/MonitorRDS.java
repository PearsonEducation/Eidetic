/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.pearson.eidetic.driver.threads.rds.RDSSubThread;
import java.util.List;


/**
 *
 * @author Judah Walker
 */
public interface MonitorRDS {
    
    boolean areAllThreadsDead(List<? extends RDSSubThread> threads);
    
    boolean areAllDBInstanceThreadsDead(List<? extends RDSSubThread> threads);
        
    boolean areAllDBClusterThreadsDead(List<? extends RDSSubThread> threads);
    
    
}