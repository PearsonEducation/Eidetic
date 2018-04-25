/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.MonitorRDS;
import com.pearson.eidetic.driver.MonitorRDSMethods;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotCleaner;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class MonitorRDSSnapshotCleaner extends MonitorRDSMethods implements Runnable, MonitorRDS {

    private static final Logger logger = LoggerFactory.getLogger(MonitorRDSSnapshotCleaner.class.getName());

    private final AwsAccount awsAccount_;

    public MonitorRDSSnapshotCleaner(AwsAccount awsAccount) {
        if (awsAccount == null) {
            throw new IllegalArgumentException("null is not valid for AWSAccount");
        }

        this.awsAccount_ = awsAccount;
    }

    @Override
    public void run() {
        while (true) {
            try {

                ArrayList<RDSSnapshotCleaner> threads = new ArrayList<>();

                threads.add(new RDSSnapshotCleaner(awsAccount_,
                        ApplicationConfiguration.geteideticCleanKeepDays(),
                        ApplicationConfiguration.getallSnapshotCleanKeepDays(),
                        awsAccount_.getUniqueAwsAccountIdentifier(),
                        awsAccount_.getMaxApiRequestsPerSecond(),
                        ApplicationConfiguration.getAwsCallRetryAttempts()));

                Threads.threadExecutorFixedPool(threads, 1, 80, TimeUnit.MINUTES);

                Threads.sleepMinutes(160);

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotCleanerFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }

    }

}
