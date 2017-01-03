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
import com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeSyncValidator;
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
 * @author Judah Walker
 */
public class MonitorVolumeSyncValidator extends MonitorMethods implements Runnable, Monitor {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;

    private final Integer runTimeInterval_;

    private final HashMap<Region, ArrayList<SnapshotVolumeSyncValidator>> EideticSubThreads_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<Volume>>> localVolumeSyncValidateList_ = new HashMap<>();

    public MonitorVolumeSyncValidator(AwsAccount awsAccount, Integer runTimeInterval) {
        this.awsAccount_ = awsAccount;
        this.runTimeInterval_ = runTimeInterval * 60;
    }

    @Override
    public void run() {
        /**
         * Every runTimeInterval_ Mins it will run to see if it needs to take a
         * snapshot of something
         */

        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeSyncValidate;
        
        while (true) {
            try {
                try {
                    localVolumeSyncValidate = awsAccount_.getVolumeSyncValidate_Copy();
                } catch (Exception e) {
                    logger.error("Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }

                HashMap<Region, Integer> secsSlept = new HashMap<>();
                HashMap<Region, Boolean> allDead = new HashMap<>();
                HashMap<Region, Integer> timeLeftOver = new HashMap<>();

                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeSyncValidate.entrySet()) {
                    Region region = entry.getKey();
                    if (localVolumeSyncValidate.get(region).isEmpty()) {
                        continue;
                    }

                    List<List<Volume>> listOfLists = Lists.partition(localVolumeSyncValidate.get(region), localVolumeSyncValidate.get(region).size());

                    localVolumeSyncValidateList_.put(region, listsToArrayLists(listOfLists));
                    
                    ArrayList<SnapshotVolumeSyncValidator> threads = new ArrayList<>();

                    for (ArrayList<Volume> vols : localVolumeSyncValidateList_.get(region)) {
                        threads.add(new SnapshotVolumeSyncValidator(
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

                    //Initializing content
                    timeLeftOver.put(region, 0);

                    EideticSubThreads_.put(region, threads);
                }

                //AND THEY'RE OFF
                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeSyncValidate.entrySet()) {
                    Region region = entry.getKey();
                    if (localVolumeSyncValidate.get(region).isEmpty()) {
                        continue;
                    }
                    
                    if (localVolumeSyncValidateList_.get(region) == null || localVolumeSyncValidateList_.get(region).isEmpty()) {
                        continue;
                    }
                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), 1, runTimeInterval_, TimeUnit.SECONDS);
                }

                //LETS SEE IF THEY'RE DEAD
                Boolean ejection = false;
                Boolean theyreDead;
                while (true) {
                    for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeSyncValidate.entrySet()) {
                        Region region = entry.getKey();
                        if (localVolumeSyncValidate.get(region).isEmpty()) {
                            continue;
                        }
                        if (areAllThreadsDead(EideticSubThreads_.get(region))) {
                            allDead.put(region, true);
                        } else {
                            secsSlept.replace(region, secsSlept.get(region), secsSlept.get(region) + 1);
                            if (secsSlept.get(region) > runTimeInterval_) {
                                ejection = true;
                                break;
                            }

                        }

                    }

                    //I dont like this
                    theyreDead = true;
                    for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeSyncValidate.entrySet()) {
                        Region region = entry.getKey();
                        if (localVolumeSyncValidate.get(region).isEmpty()) {
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
                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeSyncValidate.entrySet()) {
                    Region region = entry.getKey();
                    if (localVolumeSyncValidate.get(region).isEmpty()) {
                        continue;
                    }

                    //Left over sleep time
                    int timeRemaining = runTimeInterval_ - secsSlept.get(region);
                    if (timeRemaining > 0) {
                        timeLeftOver.put(region, timeRemaining);
                    }
                }
                
                   
                //Sleep our remaining time
                Map.Entry<Region, Integer> maxEntry = null;
                for (Map.Entry<Region, Integer> entry : timeLeftOver.entrySet()) {
                    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                        Threads.sleepSeconds(10);
                        maxEntry = entry;
                    }
                }
                
                if (maxEntry != null && maxEntry.getValue() > 0) {
                    Threads.sleepSeconds(maxEntry.getValue());
                } else {
                    Threads.sleepSeconds(runTimeInterval_);
                }

                localVolumeSyncValidateList_.clear();
                EideticSubThreads_.clear();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorSnapshotVolumeNoTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
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
