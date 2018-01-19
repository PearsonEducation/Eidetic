/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.MonitorRDS;
import com.pearson.eidetic.driver.MonitorRDSMethods;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker;
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
 * @author Judah Walker
 */
public class MonitorRDSSnapshotChecker extends MonitorRDSMethods implements Runnable, MonitorRDS {

    private static final Logger logger = LoggerFactory.getLogger(MonitorRDSSnapshotChecker.class.getName());

    private final AwsAccount awsAccount_;

    private final HashMap<Region, ArrayList<RDSSnapshotChecker>> EideticSubThreads_ = new HashMap<>();

    public MonitorRDSSnapshotChecker(AwsAccount awsAccount) {
        if (awsAccount == null) {
            throw new IllegalArgumentException("null is not valid for AWSAccount");
        }

        this.awsAccount_ = awsAccount;
    }

    @Override
    public void run() {
        //Threads.sleepMinutes(3); //Let regular Eidetic process run first @ start up so we do not alert.
        ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime;
        ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime;
        ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime;
        ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime;

        while (true) {
            try {
                try {
                    localDBInstanceNoTime = awsAccount_.getDBInstanceNoTime_Copy();
                    localDBInstanceTime = awsAccount_.getDBInstanceTime_Copy();
                    localDBClusterNoTime = awsAccount_.getDBClusterNoTime_Copy();
                    localDBClusterTime = awsAccount_.getDBClusterTime_Copy();
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"awsAccount pull failure.\" " + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    Threads.sleepSeconds(5);
                    continue;
                }

                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBInstanceNoTime.get(region).isEmpty()
                            && localDBInstanceTime.get(region).isEmpty()
                            && localDBClusterNoTime.get(region).isEmpty()
                            && localDBClusterTime.get(region).isEmpty()) {
                        continue;
                    }

                    ArrayList<RDSSnapshotChecker> threads = new ArrayList<>();

                    threads.add(new RDSSnapshotChecker(
                            awsAccount_.getAwsAccessKeyId(),
                            awsAccount_.getAwsSecretKey(),
                            awsAccount_.getAwsAccountId(),
                            awsAccount_.getUniqueAwsAccountIdentifier(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            region,
                            localDBInstanceNoTime.get(region),
                            localDBInstanceTime.get(region),
                            localDBClusterNoTime.get(region),
                            localDBClusterTime.get(region)
                    ));

                    EideticSubThreads_.put(region, threads);
                }

                //AND THEY'RE OFF
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    Region region = entry.getKey();
                    if (localDBInstanceNoTime.get(region).isEmpty()
                            && localDBInstanceTime.get(region).isEmpty()
                            && localDBClusterNoTime.get(region).isEmpty()
                            && localDBClusterTime.get(region).isEmpty()) {
                        continue;
                    }

                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), 1, 3600, TimeUnit.SECONDS);
                }

                Threads.sleepMinutes(20);

                localDBInstanceNoTime.clear();
                localDBInstanceTime.clear();
                localDBClusterNoTime.clear();
                localDBClusterTime.clear();

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotCheckerFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }
    }
}
