/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.Monitor;
import com.pearson.eidetic.driver.MonitorMethods;
import com.pearson.eidetic.driver.threads.subthreads.ErrorChecker;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class MonitorErrorChecker extends MonitorMethods implements Runnable, Monitor {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;

    private final HashMap<Region, ArrayList<ErrorChecker>> EideticSubThreads_ = new HashMap<>();

    public MonitorErrorChecker(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
    }

    @Override
    public void run() {
        ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots;
        
        //Only need it for the regions
        localCopyVolumeSnapshots = awsAccount_.getCopyVolumeSnapshots_Copy();
        
        
        while (true) {
            try {
                
                
                for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                    Region region = entry.getKey();

                    //List<List<Volume>> lolz = Lists.partition(localVolumeNoTime.get(region), splitFactor_.get(region));
                    //localVolumeNoTimeList_.put(region, splitArrayList(localVolumeNoTime.get(region), splitFactor_.get(region)));
                    ArrayList<ErrorChecker> threads = new ArrayList<>();

                    threads.add(new ErrorChecker(awsAccount_,
                            awsAccount_.getUniqueAwsAccountIdentifier(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            ApplicationConfiguration.getAwsCallRetryAttempts()));

                    EideticSubThreads_.put(region, threads);
                }

                //AND THEY'RE OFF
                for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                    Region region = entry.getKey();
                    
                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), 1, 600, TimeUnit.SECONDS);
                }
                
                Threads.sleepMinutes(10);

            } catch (Exception e) {
                logger.error("Error=\"MonitorErrorCheckerFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }

    }
}