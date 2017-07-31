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
import com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker;
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
 * @author Judah Walker
 */
public class MonitorSnapshotChecker extends MonitorMethods implements Runnable, Monitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());
    
    private final AwsAccount awsAccount_;
    
    private final HashMap<Region, ArrayList<SnapshotChecker>> EideticSubThreads_ = new HashMap<>();
    
    public MonitorSnapshotChecker(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
    }
    
    @Override
    public void run() {
        Threads.sleepMinutes(3); //Let regular Eidetic process run first @ start up so we do not alert.
        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime;
        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime;
        
        while (true) {
            try {
                try {
                    localVolumeTime = awsAccount_.getVolumeTime_Copy();
                    localVolumeNoTime = awsAccount_.getVolumeNoTime_Copy();
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }
                
                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localVolumeNoTime.get(region).isEmpty() && localVolumeTime.get(region).isEmpty()) {
                        continue;
                    }
                    
                    ArrayList<SnapshotChecker> threads = new ArrayList<>();
                    
                    threads.add(new SnapshotChecker(
                            awsAccount_.getAwsAccessKeyId(),
                            awsAccount_.getAwsSecretKey(),
                            awsAccount_.getUniqueAwsAccountIdentifier(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            region,
                            localVolumeNoTime.get(region),
                            localVolumeTime.get(region)
                    ));
                    
                    EideticSubThreads_.put(region, threads);
                }

                //AND THEY'RE OFF
                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localVolumeNoTime.get(region).isEmpty() && localVolumeTime.get(region).isEmpty()) {
                        continue;
                    }
                    
                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), 1, 3600, TimeUnit.SECONDS);
                }
                
                Threads.sleepMinutes(30);
                
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorSnapshotCheckerFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }
    }
    
}
