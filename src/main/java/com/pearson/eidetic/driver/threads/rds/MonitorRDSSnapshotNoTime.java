/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.google.common.collect.Lists;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.MonitorRDS;
import com.pearson.eidetic.driver.MonitorRDSMethods;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBInstanceNoTime;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Judah Walker
 */
public class MonitorRDSSnapshotNoTime extends MonitorRDSMethods implements Runnable, MonitorRDS {

    private static final Logger logger = LoggerFactory.getLogger(MonitorRDSSnapshotNoTime.class.getName());

    private final AwsAccount awsAccount_;

    private final Integer runTimeInterval_;

    private final HashMap<Region, ArrayList<RDSSnapshotDBInstanceNoTime>> DBInstanceSubThreads_ = new HashMap<>();
    private final HashMap<Region, ArrayList<RDSSnapshotDBClusterNoTime>> DBClusterSubThreads_ = new HashMap<>();
    private final HashMap<Region, Integer> splitFactor_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<DBInstance>>> localDBInstanceNoTimeList_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<DBCluster>>> localDBClusterNoTimeList_ = new HashMap<>();

    public MonitorRDSSnapshotNoTime(AwsAccount awsAccount, Integer runTimeInterval) {
        if (awsAccount == null || runTimeInterval == null) {
            throw new IllegalArgumentException("null is not valid for AWSAccount");
        }
        
        this.awsAccount_ = awsAccount;
        this.runTimeInterval_ = runTimeInterval * 60;
    }

    @Override
    public void run() {
        /**
         * Every runTimeInterval_ Mins it will run to see if it needs to take a
         * snapshot of something
         */

        ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime;
        ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime;
        localDBInstanceNoTime = awsAccount_.getDBInstanceNoTime_Copy();
        //Just for region population
        for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
            Region region = entry.getKey();
            splitFactor_.put(region, 1);
        }

        while (true) {
            try {
                //*********************
                //Now for localDBInstanceNoTime
                //*********************
                try {
                    localDBInstanceNoTime = awsAccount_.getDBInstanceNoTime_Copy();
                } catch (Exception e) {
                    logger.error("Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }

                HashMap<Region, Integer> secsSleptDBInstance = new HashMap<>();
                HashMap<Region, Boolean> allDeadDBInstance = new HashMap<>();
                HashMap<Region, Integer> timeLeftOverDBInstance = new HashMap<>();

                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBInstanceNoTime.get(region).isEmpty()) {
                        continue;
                    }

                    List<List<DBInstance>> listOfLists = Lists.partition(localDBInstanceNoTime.get(region), splitFactor_.get(region));

                    localDBInstanceNoTimeList_.put(region, listsOfDBInstancesToArrayLists(listOfLists));

                    ArrayList<RDSSnapshotDBInstanceNoTime> threads = new ArrayList<>();

                    for (ArrayList<DBInstance> dbInstances : localDBInstanceNoTimeList_.get(region)) {
                        threads.add(new RDSSnapshotDBInstanceNoTime(
                                awsAccount_.getAwsAccessKeyId(),
                                awsAccount_.getAwsSecretKey(),
                                awsAccount_.getAwsAccountId(),
                                awsAccount_.getUniqueAwsAccountIdentifier(),
                                awsAccount_.getMaxApiRequestsPerSecond(),
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                region,
                                dbInstances));
                    }

                    //Initializing content
                    secsSleptDBInstance.put(region, 0);

                    //Initializing content
                    allDeadDBInstance.put(region, false);

                    //Initializing content
                    timeLeftOverDBInstance.put(region, 0);

                    DBInstanceSubThreads_.put(region, threads);
                }
                //*********************
                //Now for localDBClusterNoTime
                //*********************
                try {
                    localDBClusterNoTime = awsAccount_.getDBClusterNoTime_Copy();
                } catch (Exception e) {
                    logger.error("Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }

                //*************
                HashMap<Region, Integer> secsSleptDBCluster = new HashMap<>();
                HashMap<Region, Boolean> allDeadDBCluster = new HashMap<>();
                HashMap<Region, Integer> timeLeftOverDBCluster = new HashMap<>();

                for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBClusterNoTime.get(region).isEmpty()) {
                        continue;
                    }

                    List<List<DBCluster>> listOfLists = Lists.partition(localDBClusterNoTime.get(region), splitFactor_.get(region));

                    localDBClusterNoTimeList_.put(region, listsOfDBClustersToArrayLists(listOfLists));

                    ArrayList<RDSSnapshotDBClusterNoTime> threads = new ArrayList<>();

                    for (ArrayList<DBCluster> dbClusters : localDBClusterNoTimeList_.get(region)) {
                        threads.add(new RDSSnapshotDBClusterNoTime(
                                awsAccount_.getAwsAccessKeyId(),
                                awsAccount_.getAwsSecretKey(),
                                awsAccount_.getAwsAccountId(),
                                awsAccount_.getUniqueAwsAccountIdentifier(),
                                awsAccount_.getMaxApiRequestsPerSecond(),
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                region,
                                dbClusters));
                    }

                    //Initializing content
                    secsSleptDBCluster.put(region, 0);

                    //Initializing content
                    allDeadDBCluster.put(region, false);

                    //Initializing content
                    timeLeftOverDBCluster.put(region, 0);

                    DBClusterSubThreads_.put(region, threads);
                }

                //AND instance is OFF
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBInstanceNoTime.get(region).isEmpty()) {
                        continue;
                    }

                    if (localDBInstanceNoTimeList_.get(region) == null || localDBInstanceNoTimeList_.get(region).isEmpty()) {
                        continue;
                    }
                    Threads.threadExecutorFixedPool(DBInstanceSubThreads_.get(region), splitFactor_.get(region), runTimeInterval_, TimeUnit.SECONDS);
                }
                //AND cluster is OFF
                for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBClusterNoTime.get(region).isEmpty()) {
                        continue;
                    }

                    if (localDBClusterNoTimeList_.get(region) == null || localDBClusterNoTimeList_.get(region).isEmpty()) {
                        continue;
                    }
                    Threads.threadExecutorFixedPool(DBClusterSubThreads_.get(region), splitFactor_.get(region), runTimeInterval_, TimeUnit.SECONDS);
                }

                //*********************
                //LETS SEE IF THEY'RE DEAD
                Boolean ejection = false;
                Boolean theyreDeadDBInstance = false;
                Boolean theyreDeadDBCluster = false;
                while (true) {
                    if (!theyreDeadDBInstance) {
                        for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                            Region region = entry.getKey();
                            if (localDBInstanceNoTime.get(region).isEmpty()) {
                                continue;
                            }
                            if (areAllThreadsDead(DBInstanceSubThreads_.get(region))) {
                                allDeadDBInstance.put(region, true);
                            } else {
                                secsSleptDBInstance.replace(region, secsSleptDBInstance.get(region), secsSleptDBInstance.get(region) + 1);
                                if (secsSleptDBInstance.get(region) > runTimeInterval_) {
                                    splitFactor_.replace(region, splitFactor_.get(region), splitFactor_.get(region) + 1);
                                    logger.info("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=\"increasing_splitFactor\", Monitor=\"RDSSnapshotNoTime\", splitFactor=\""
                                            + Integer.toString(splitFactor_.get(region)) + "\", RDSSnapshotNoTimeSize=\"" + Integer.toString(localDBInstanceNoTime.get(region).size()) + "\"");
                                    ejection = true;
                                    break;
                                }

                            }

                        }
                    }
                    
                    if (!theyreDeadDBCluster) {
                        for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterNoTime.entrySet()) {
                            Region region = entry.getKey();
                            if (localDBClusterNoTime.get(region).isEmpty()) {
                                continue;
                            }
                            if (areAllThreadsDead(DBClusterSubThreads_.get(region))) {
                                allDeadDBInstance.put(region, true);
                            } else {
                                secsSleptDBCluster.replace(region, secsSleptDBCluster.get(region), secsSleptDBCluster.get(region) + 1);
                                if (secsSleptDBInstance.get(region) > runTimeInterval_) {
                                    splitFactor_.replace(region, splitFactor_.get(region), splitFactor_.get(region) + 1);
                                    logger.info("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=\"increasing_splitFactor\", Monitor=\"RDSSnapshotNoTime\", splitFactor=\""
                                            + Integer.toString(splitFactor_.get(region)) + "\", RDSSnapshotNoTimeSize=\"" + Integer.toString(localDBClusterNoTime.get(region).size()) + "\"");
                                    ejection = true;
                                    break;
                                }

                            }

                        }
                    }

                    //I dont like this
                    theyreDeadDBInstance = true;
                    for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                        Region region = entry.getKey();
                        if (localDBInstanceNoTime.get(region).isEmpty()) {
                            continue;
                        }
                        //If any of them have false
                        if (!allDeadDBInstance.get(region)) {
                            theyreDeadDBInstance = false;
                        }
                    }
                    
                    theyreDeadDBCluster = true;
                    for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterNoTime.entrySet()) {
                        Region region = entry.getKey();
                        if (localDBClusterNoTime.get(region).isEmpty()) {
                            continue;
                        }
                        //If any of them have false
                        if (!allDeadDBCluster.get(region)) {
                            theyreDeadDBCluster = false;
                        }
                    }

                    if (ejection || (theyreDeadDBInstance && theyreDeadDBCluster)) {
                        break;
                    }

                    Threads.sleepSeconds(1);
                }
                //*********************
                //*********************

                //See if decrease splitfactor
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBInstanceNoTime.get(region).isEmpty()) {
                        continue;
                    }

                    //Left over sleep time
                    int timeRemaining = runTimeInterval_ - secsSleptDBInstance.get(region);
                    if (timeRemaining > 0) {
                        timeLeftOverDBInstance.put(region, timeRemaining);
                    }

                    if ((splitFactor_.get(region) > 1) & (timeRemaining > 60)) {
                        splitFactor_.replace(region, splitFactor_.get(region), splitFactor_.get(region) - 1);
                        logger.info("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=\"decreasing_splitFactor\", Monitor=\"RDSSnapshotNoTime\", splitFactor=\""
                                + Integer.toString(splitFactor_.get(region)) + "\", RDSSnapshotDBIstanceNoTimeSize=\"" + Integer.toString(localDBInstanceNoTime.get(region).size()) + "\"");
                    }
                }

                //*********************
                //Sleep our remaining time
                Map.Entry<Region, Integer> maxEntry = null;
                for (Map.Entry<Region, Integer> entry : timeLeftOverDBInstance.entrySet()) {
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

                localDBInstanceNoTimeList_.clear();
                localDBClusterNoTimeList_.clear();
                DBInstanceSubThreads_.clear();
                DBClusterSubThreads_.clear();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotNoTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }

    }

}
