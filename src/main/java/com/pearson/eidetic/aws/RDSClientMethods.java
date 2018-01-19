/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
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
import com.pearson.eidetic.globals.ApplicationConfiguration;
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

    private static final Logger logger = LoggerFactory.getLogger(RDSClientMethods.class.getName());

    public static DescribeDBInstancesResult describeDBInstances(Region region, AmazonRDS amazonRDSClient, DescribeDBInstancesRequest describeDBInstancesRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBInstancesResult describeDBInstancesResult = null;
        int randomNum;
        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeDBInstancesResult = awsAccount.getDescribeDBInstancesCache(region, describeDBInstancesRequest);
                    if (describeDBInstancesResult != null) {
                        return describeDBInstancesResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000) ms
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + "describeDBInstances()," + describeDBInstancesRequest.toString() + "\"," + e.toString());
                } else if (e.toString().contains("DBInstanceNotFound")) {
                    return null;
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBInstancesRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            if (describeDBInstancesResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeDBInstancesCache(region, describeDBInstancesRequest, describeDBInstancesResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeDBInstancesResult;

    }

    public static ListTagsForResourceResult getTags(Region region, AmazonRDS amazonRDSClient, ListTagsForResourceRequest listTagsForResourceRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        ListTagsForResourceResult listTagsForResourceResult = null;
        int randomNum;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    listTagsForResourceResult = awsAccount.getListTagsForResourceCache(region, listTagsForResourceRequest);
                    if (listTagsForResourceResult != null) {
                        return listTagsForResourceResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + "getTags()," + listTagsForResourceRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + listTagsForResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            if (listTagsForResourceResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putListTagsForResourceCache(region, listTagsForResourceRequest, listTagsForResourceResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return listTagsForResourceResult;

    }

    public static DescribeDBClustersResult describeDBClusters(Region region, AmazonRDS amazonRDSClient, DescribeDBClustersRequest describeDBClustersRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBClustersResult describeDBClustersResult = null;
        int randomNum;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeDBClustersResult = awsAccount.getDescribeDBClustersCache(region, describeDBClustersRequest);
                    if (describeDBClustersResult != null) {
                        return describeDBClustersResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeDBClusters()," + describeDBClustersRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBClustersRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            if (describeDBClustersResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeDBClustersCache(region, describeDBClustersRequest, describeDBClustersResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeDBClustersResult;
    }

    public static DescribeDBSnapshotsResult describeDBSnapshots(Region region, AmazonRDS amazonRDSClient, DescribeDBSnapshotsRequest describeDBSnapshotsRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBSnapshotsResult describeDBSnapshotsResult = null;
        int randomNum;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeDBSnapshotsResult = awsAccount.getDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest);
                    if (describeDBSnapshotsResult != null) {
                        return describeDBSnapshotsResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeDBSnapshots()," + describeDBSnapshotsRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBSnapshotsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            if (describeDBSnapshotsResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest, describeDBSnapshotsResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeDBSnapshotsResult;
    }

    public static DescribeDBClusterSnapshotsResult describeDBClusterSnapshots(Region region, AmazonRDS amazonRDSClient, DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult = null;
        int randomNum;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeDBClusterSnapshotsResult = awsAccount.getDescribeDBClusterSnapshotsCache(region, describeDBClusterSnapshotsRequest);
                    if (describeDBClusterSnapshotsResult != null) {
                        return describeDBClusterSnapshotsResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeDBClusterSnapshots()," + describeDBClusterSnapshotsRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeDBClusterSnapshotsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            if (describeDBClusterSnapshotsResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeDBClusterSnapshotsCache(region, describeDBClusterSnapshotsRequest, describeDBClusterSnapshotsResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeDBClusterSnapshotsResult;
    }

    public static void deleteDBSnapshot(Region region, AmazonRDS amazonRDSClient, DBInstance dbInstance, DeleteDBSnapshotRequest deleteDBSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum;
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                        return;//no need to update cache
                    }
                    if (e.toString().contains("DBSnapshotNotFound")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                        break;//need to update cache
                    }

                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",deleteDBSnapshot()," + deleteDBSnapshotRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    DescribeDBSnapshotsRequest describeDBSnapshotsRequest_DBSnapshotID = new DescribeDBSnapshotsRequest().withDBSnapshotIdentifier(deleteDBSnapshotRequest.getDBSnapshotIdentifier());
                    awsAccount.removeDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest_DBSnapshotID);
                    //Delete DBInstancefrom Snapshot Cache
                    if (dbInstance != null) {
                        DescribeDBSnapshotsRequest describeDBSnapshotsRequest_DBInstanceIdentifier = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(dbInstance.getDBInstanceIdentifier());
                        awsAccount.removeDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest_DBInstanceIdentifier);
                        DescribeDBSnapshotsRequest describeDBSnapshotsRequest_DBClusterIdentifier = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(dbInstance.getDBClusterIdentifier());
                        awsAccount.removeDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest_DBClusterIdentifier);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

    }

    public static void deleteDBClusterSnapshot(Region region, AmazonRDS amazonRDSClient, DBCluster dbCluster, DeleteDBClusterSnapshotRequest deleteDBClusterSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum;
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                    if (e.toString().contains("DBClusterSnapshotNotFoundFault")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                        break;
                    }
                    if (e.toString().contains("Throttling")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",deleteDBClusterSnapshot()," + deleteDBClusterSnapshotRequest.toString() + "\"," + e.toString());
                    } else {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                }

            } catch (Exception e) {
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",deleteDBClusterSnapshot()," + deleteDBClusterSnapshotRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest_DBSnapshotID = new DescribeDBClusterSnapshotsRequest().withDBClusterSnapshotIdentifier(deleteDBClusterSnapshotRequest.getDBClusterSnapshotIdentifier());
                    awsAccount.removeDescribeDBClusterSnapshotsCache(region, describeDBClusterSnapshotsRequest_DBSnapshotID);
                    //Delete DBInstancefrom Snapshot Cache
                    if (dbCluster != null) {
                        DescribeDBClusterSnapshotsRequest describeDBSnapshotsRequest_DBClusterIdentifier = new DescribeDBClusterSnapshotsRequest().withDBClusterIdentifier(dbCluster.getDBClusterIdentifier());
                        awsAccount.removeDescribeDBClusterSnapshotsCache(region, describeDBSnapshotsRequest_DBClusterIdentifier);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

    }

    public static DBSnapshot createDBSnapshot(Region region, AmazonRDS amazonRDSClient, CreateDBSnapshotRequest createDBSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DBSnapshot dbSnapshot = null;
        int randomNum;
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",createDBSnapshot()," + createDBSnapshotRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    if (dbSnapshot != null) {
                        DescribeDBSnapshotsRequest describeDBSnapshotsRequest_DBSnapshotIdentifier = new DescribeDBSnapshotsRequest().withDBSnapshotIdentifier(dbSnapshot.getDBSnapshotIdentifier());
                        awsAccount.removeDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest_DBSnapshotIdentifier);
                        DescribeDBSnapshotsRequest describeDBSnapshotsRequest_DBInstanceIdentifier = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(dbSnapshot.getDBInstanceIdentifier());
                        awsAccount.removeDescribeDBSnapshotsCache(region, describeDBSnapshotsRequest_DBInstanceIdentifier);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return dbSnapshot;
    }

    public static DBClusterSnapshot createDBClusterSnapshot(Region region, AmazonRDS amazonRDSClient, CreateDBClusterSnapshotRequest createDBClusterSnapshotRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DBClusterSnapshot dbClusterSnapshot = null;
        int randomNum;
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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

                dbClusterSnapshot = amazonRDSClient.createDBClusterSnapshot(createDBClusterSnapshotRequest);
                elapsedTime = (new Date()).getTime() - startTime;

                if (dbClusterSnapshot != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    logger.debug("************** I finished this snapshot and it took " + elapsedTime.toString() + " ms *******************");
                    break;
                }

            } catch (Exception e) {
                if (e.toString().contains("is not currently in the available state")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBClusterSnapshotRequest.toString() + ", \"error=\"failure to create snapshot, db not in the available state, will try again later\"");
                    break;
                }
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",createDBClusterSnapshot()," + createDBClusterSnapshotRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    if (dbClusterSnapshot != null) {
                        DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest_DBSnapshotIdentifier = new DescribeDBClusterSnapshotsRequest().withDBClusterSnapshotIdentifier(dbClusterSnapshot.getDBClusterSnapshotIdentifier());
                        awsAccount.removeDescribeDBClusterSnapshotsCache(region, describeDBClusterSnapshotsRequest_DBSnapshotIdentifier);
                        DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest_DBInstanceIdentifier = new DescribeDBClusterSnapshotsRequest().withDBClusterIdentifier(dbClusterSnapshot.getDBClusterIdentifier());
                        awsAccount.removeDescribeDBClusterSnapshotsCache(region, describeDBClusterSnapshotsRequest_DBInstanceIdentifier);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return dbClusterSnapshot;
    }

    public static void createTags(Region region, AmazonRDSClient rdsClient, AddTagsToResourceRequest addTagsToResourceRequest, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
        Threads.sleepMilliseconds(50 + (10 * randomNum));
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + avg(2000)ms 
                if (i > 0) {
                    randomNum = ThreadLocalRandom.current().nextInt(1, 10);
                    long sleepTimeInMilliseconds = 2000 * i + (randomNum * 100);
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
                if (e.toString().contains("Throttling")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",createTags()," + addTagsToResourceRequest.toString() + "\"," + e.toString());
                } else {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + addTagsToResourceRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }
            }
        }

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(addTagsToResourceRequest.getResourceName());
                    awsAccount.removeListTagsForResourceCache(region, listTagsForResourceRequest);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

    }

}
