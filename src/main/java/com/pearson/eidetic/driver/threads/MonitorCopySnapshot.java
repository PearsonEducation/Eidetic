/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.model.Volume;
import com.google.common.collect.Lists;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.Monitor;
import com.pearson.eidetic.driver.MonitorMethods;
import static com.pearson.eidetic.driver.MonitorMethods.listsToArrayLists;
import com.pearson.eidetic.driver.threads.subthreads.CopySnapshot;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeNoTime;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class MonitorCopySnapshot extends MonitorMethods implements Runnable, Monitor {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;

    private final Integer runTimeInterval_;

    private final HashMap<Region, ArrayList<CopySnapshot>> EideticSubThreads_ = new HashMap<>();
    private final HashMap<Region, Integer> splitFactor_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<Volume>>> localCopyVolumeSnapshotsList_ = new HashMap<>();

    public MonitorCopySnapshot(AwsAccount awsAccount, Integer runTimeInterval) {
        this.awsAccount_ = awsAccount;
        this.runTimeInterval_ = runTimeInterval * 60;
    }

    @Override
    public void run() {
        /**
         * Every runTimeInterval_ Mins it will run to see if it needs to take a
         * snapshot of something
         */

        ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots;
        localCopyVolumeSnapshots = awsAccount_.getCopyVolumeSnapshots_Copy();
        for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
            Region region = entry.getKey();
            splitFactor_.put(region, 1);
        }

        while (true) {
            try {
                try {
                    localCopyVolumeSnapshots = awsAccount_.getCopyVolumeSnapshots_Copy();
                } catch (Exception e) {
                    logger.error("Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }

                HashMap<Region, Integer> secsSlept = new HashMap<>();
                HashMap<Region, Boolean> allDead = new HashMap<>();

                for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                    Region region = entry.getKey();
                    if (localCopyVolumeSnapshots.get(region).isEmpty()) {
                        continue;
                    }

                    List<List<Volume>> listOfLists = Lists.partition(localCopyVolumeSnapshots.get(region), splitFactor_.get(region));
                    
                    localCopyVolumeSnapshotsList_.put(region, listsToArrayLists(listOfLists));
                    
                    //List<List<Volume>> lolz = Lists.partition(localVolumeNoTime.get(region), splitFactor_.get(region));
                    
                    //localVolumeNoTimeList_.put(region, splitArrayList(localVolumeNoTime.get(region), splitFactor_.get(region)));
                    
                    ArrayList<CopySnapshot> threads = new ArrayList<>();

                    for (ArrayList<Volume> vols : localCopyVolumeSnapshotsList_.get(region)) {
                        threads.add(new CopySnapshot(
                                awsAccount_.getAwsAccessKeyId(),
                                awsAccount_.getAwsSecretKey(),
                                awsAccount_.getUniqueAwsAccountIdentifier(),
                                awsAccount_.getMaxApiRequestsPerSecond(),
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                region,
                                vols));
                    }

                    //Initializing content
                    secsSlept.put(region, 0);

                    //Initializing content
                    allDead.put(region, false);

                    EideticSubThreads_.put(region, threads);
                }

                //AND THEY'RE OFF
                for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                    Region region = entry.getKey();
                    if (localCopyVolumeSnapshots.get(region).isEmpty()) {
                        continue;
                    }
                    
                    if (localCopyVolumeSnapshotsList_.get(region) == null || localCopyVolumeSnapshotsList_.get(region).isEmpty()) {
                        continue;
                    }
                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), splitFactor_.get(region), runTimeInterval_, TimeUnit.SECONDS);
                }

                //LETS SEE IF THEY'RE DEAD
                Boolean ejection = false;
                Boolean theyreDead;
                while (true) {
                    for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                        Region region = entry.getKey();
                        if (localCopyVolumeSnapshots.get(region).isEmpty()) {
                            continue;
                        }
                        if (areAllThreadsDead(EideticSubThreads_.get(region))) {
                            allDead.put(region, true);
                        } else {
                            secsSlept.replace(region, secsSlept.get(region), secsSlept.get(region) + 1);
                            if (secsSlept.get(region) > runTimeInterval_) {
                                splitFactor_.replace(region, splitFactor_.get(region), splitFactor_.get(region) + 1);
                                logger.info("Event=\"increasing_splitFactor\", Monitor=\"SnapshotVolumeNoTime\", splitFactor=\""
                                        + Integer.toString(splitFactor_.get(region)) + "\", VolumeNoTimeSize=\"" + Integer.toString(localCopyVolumeSnapshots.get(region).size()) + "\"");
                                ejection = true;
                                break;
                            }

                        }

                    }

                    //I dont like this
                    theyreDead = true;
                    for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                        Region region = entry.getKey();
                        if (localCopyVolumeSnapshots.get(region).isEmpty()) {
                            continue;
                        }
                        //If any of them have false
                        if (!allDead.get(region)) {
                            theyreDead = false;
                        }
                    }

                    if (ejection || theyreDead) {
                        break;
                    }

                    Threads.sleepSeconds(1);
                }

                //See if decrease splitfactor
                for (Map.Entry<Region, ArrayList<Volume>> entry : localCopyVolumeSnapshots.entrySet()) {
                    Region region = entry.getKey();
                    if (localCopyVolumeSnapshots.get(region).isEmpty()) {
                        continue;
                    }

                    //Left over sleep time
                    int timeRemaining = runTimeInterval_ - secsSlept.get(region);

                    if ((splitFactor_.get(region) > 1) & (timeRemaining > 60)) {
                        splitFactor_.replace(region, splitFactor_.get(region), splitFactor_.get(region) - 1);
                        logger.info("Event=\"decreasing_splitFactor\", Monitor=\"CopySnapshot\", splitFactor=\""
                                + Integer.toString(splitFactor_.get(region)) + "\", CopyVolumeSnapshotsSize=\"" + Integer.toString(localCopyVolumeSnapshots.get(region).size()) + "\"");
                    }
                }

                localCopyVolumeSnapshotsList_.clear();
                EideticSubThreads_.clear();
                
                Threads.sleepMinutes(30);
            } catch (Exception e) {
                logger.error("Error=\"MonitorCopySnapshotFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }
        /*
         for (Region region : EideticSubThreads_.keySet()) {
         ArrayList<EideticSubThread> EideticSubThreads = EideticSubThreads_.get(region);
         EideticSubThreadMethods.areAllThreadsDead(EideticSubThreads);
         }
         */

    }
    

}


