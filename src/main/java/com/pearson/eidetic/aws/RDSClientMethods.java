/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.aws;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DeleteDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.DeleteDBSnapshotRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import java.util.concurrent.ThreadLocalRandom;

import com.pearson.eidetic.globals.GlobalVariables;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class RDSClientMethods {

    private static final Logger logger = LoggerFactory.getLogger(AwsAccount.class.getName());

    public static DescribeDBInstancesResult describeDBInstances(AmazonRDS amazonRDSClient, DescribeDBInstancesRequest describeDBInstancesRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBInstancesResult describeDBInstancesResult = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500) ms
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeDBInstancesResult = amazonRDSClient.describeDBInstances(describeDBInstancesRequest);

                if (describeDBInstancesResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBInstancesRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        return describeDBInstancesResult;

    }

    public static ListTagsForResourceResult getTags(AmazonRDS amazonRDSClient, ListTagsForResourceRequest listTagsForResourceRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        ListTagsForResourceResult listTagsForResourceResult = null;
        int randomNum = ThreadLocalRandom.current().nextInt(5, 70);
        Threads.sleepMilliseconds(40 * randomNum);
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                listTagsForResourceResult = amazonRDSClient.listTagsForResource(listTagsForResourceRequest);

                if (listTagsForResourceResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + listTagsForResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        return listTagsForResourceResult;

    }

    public static DescribeDBClustersResult describeDBClusters(AmazonRDS amazonRDSClient, DescribeDBClustersRequest describeDBClustersRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBClustersResult describeDBClustersResult = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeDBClustersResult = amazonRDSClient.describeDBClusters(describeDBClustersRequest);

                if (describeDBClustersResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBClustersRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        return describeDBClustersResult;
    }

    public static DescribeDBSnapshotsResult describeDBSnapshots(AmazonRDS amazonRDSClient, DescribeDBSnapshotsRequest describeDBSnapshotsRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBSnapshotsResult describeDBSnapshotsResult = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeDBSnapshotsResult = amazonRDSClient.describeDBSnapshots(describeDBSnapshotsRequest);

                if (describeDBSnapshotsResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBSnapshotsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        return describeDBSnapshotsResult;
    }

    public static DescribeDBClusterSnapshotsResult describeDBClusterSnapshots(AmazonRDS amazonRDSClient, DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeDBClusterSnapshotsResult = amazonRDSClient.describeDBClusterSnapshots(describeDBClusterSnapshotsRequest);

                if (describeDBClusterSnapshotsResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBClusterSnapshotsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        return describeDBClusterSnapshotsResult;
    }

    public static void deleteDBSnapshot(AmazonRDS amazonRDSClient, DeleteDBSnapshotRequest deleteDBSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    amazonRDSClient.deleteDBSnapshot(deleteDBSnapshotRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("InvalidSnapshot.InUse")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                        return;
                    }

                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
    }

    public static void deleteDBClusterSnapshot(AmazonRDS amazonRDSClient, DeleteDBClusterSnapshotRequest deleteDBClusterSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    amazonRDSClient.deleteDBClusterSnapshot(deleteDBClusterSnapshotRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("InvalidSnapshot.InUse")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                        return;
                    }
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
    }

    public static DBSnapshot createDBSnapshot(AmazonRDS amazonRDSClient, CreateDBSnapshotRequest createDBSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DBSnapshot dbSnapshot = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                Long elapsedTime;
                Long startTime = System.currentTimeMillis();

                dbSnapshot = amazonRDSClient.createDBSnapshot(createDBSnapshotRequest);
                elapsedTime = (new Date()).getTime() - startTime;

                if (dbSnapshot != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    logger.debug("************** I finished this snapshot and it took " + elapsedTime.toString() + " ms *******************");
                    break;
                }

            } catch (Exception e) {
                if (e.toString().contains("is not currently in the available state")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\", snapshotRequest=" + createDBSnapshotRequest.toString() + ", \"error=\"failure to create snapshot, targeted DB not in the available state, will try again later\"");
                    break;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
        return dbSnapshot;
    }

    public static DBClusterSnapshot createDBClusterSnapshot(AmazonRDS amazonRDSClient, CreateDBClusterSnapshotRequest createDBClusterSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DBClusterSnapshot dbSnapshot = null;
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                Long elapsedTime;
                Long startTime = System.currentTimeMillis();

                dbSnapshot = amazonRDSClient.createDBClusterSnapshot(createDBClusterSnapshotRequest);
                elapsedTime = (new Date()).getTime() - startTime;

                if (dbSnapshot != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    logger.debug("************** I finished this snapshot and it took " + elapsedTime.toString() + " ms *******************");
                    break;
                }

            } catch (Exception e) {
                if (e.toString().contains("is not currently in the available state")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBClusterSnapshotRequest.toString() + ", \"error=\"failure to create snapshot, db not in the available state, will try again later\"");
                    break;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
        return dbSnapshot;
    }

    public static void createTags(AmazonRDSClient rdsClient, AddTagsToResourceRequest addTagsToResourceRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 500 + avg(500)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 500 * i + (randomNum * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    rdsClient.addTagsToResource(addTagsToResourceRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + addTagsToResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("TagLimitExceeded")) {
                        break;
                    }
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + addTagsToResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + addTagsToResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
    }

}
