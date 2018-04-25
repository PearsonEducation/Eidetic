/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.globals;

import com.amazonaws.regions.Region;
import com.pearson.eidetic.aws.AwsAccount;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Judah Walker
 */
public class GlobalVariables {
    private static final Logger logger = LoggerFactory.getLogger(GlobalVariables.class.getName());
    
    // k=unique identifer per 'aws account', v=request count
    public final static ConcurrentHashMap<String,AtomicLong> apiRequestAttemptCountersByAwsAccount = new ConcurrentHashMap<>();
    
    // k=unique identifer per 'aws account', v=request count
    public final static ConcurrentHashMap<String,AtomicLong> apiRequestCountersByAwsAccount = new ConcurrentHashMap<>();
    

}
