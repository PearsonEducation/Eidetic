/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Judah Walker
 */
public interface Monitor {
    
    boolean areAllThreadsDead(List<? extends EideticSubThread> threads);
    
    ArrayList<ArrayList<Volume>> splitArrayList(ArrayList<Volume> volumes, Integer splitFactor);
    
}
