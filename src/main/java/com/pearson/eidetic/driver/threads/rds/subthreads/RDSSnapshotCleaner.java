/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds.subthreads;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.driver.threads.rds.RDSSubThread;
import com.pearson.eidetic.driver.threads.rds.RDSSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class RDSSnapshotCleaner extends RDSSubThreadMethods implements Runnable, RDSSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = false;
    private AwsAccount awsAccount_ = null;
    private Integer eideticCleanKeepDays_ = null;
    private Integer allSnapshotCleanKeepDays_ = null;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;

    public RDSSnapshotCleaner(AwsAccount awsAccount, Integer eideticCleanDays, Integer allSnapshotCleanDays,
            String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond, Integer numRetries) {
        this.awsAccount_ = awsAccount;
        this.eideticCleanKeepDays_ = eideticCleanDays;
        this.allSnapshotCleanKeepDays_ = allSnapshotCleanDays;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
    }

    @Override
    public void run() {
        isFinished_ = false;

        //Just to get the regions
        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<DBInstance>> entry : awsAccount_.getDBInstanceNoTime_Copy().entrySet()) {
            try {
                com.amazonaws.regions.Region region = entry.getKey();
                //kill thread if wrong creds \/ \/ \/ \/
                AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

                Boolean success;

                allSnapshotsClean(rdsClient);

                success = eideticClean(rdsClient, region, awsAccount_.getAwsAccountId());
                if (!success) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in RDSSnapshotCleaner workflow\"");
                }

                rdsClient.shutdown();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in RDSSnapshotCleaner workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        isFinished_ = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished_;
    }

    public AmazonRDSClient connect(Region region, String awsAccessKey, String awsSecretKey) {
        AmazonRDSClient rdsClient;
        String endpoint = "rds." + region.getName() + ".amazonaws.com";

        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTPS);

        rdsClient = new AmazonRDSClient(credentials, clientConfig);
        rdsClient.setRegion(region);
        rdsClient.setEndpoint(endpoint);
        return rdsClient;
    }

    public Boolean eideticClean(AmazonRDSClient rdsClient, Region region, String awsAccountId) {
        if (rdsClient == null) {
            return false;
        }
        DescribeDBSnapshotsRequest describeDBSnapshotRequest
                = new DescribeDBSnapshotsRequest();
        DescribeDBSnapshotsResult describeDBSnapshotsResult
                = RDSClientMethods.describeDBSnapshots(rdsClient,
                        describeDBSnapshotRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        List<DBSnapshot> dbSnapshots = describeDBSnapshotsResult.getDBSnapshots();

        List<DBSnapshot> deleteList = new ArrayList();
        for (DBSnapshot dbSnapshot : dbSnapshots) {

            String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region.getName(), awsAccount_.getAwsAccountId(), dbSnapshot.getDBSnapshotIdentifier());
            ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
            List<com.amazonaws.services.rds.model.Tag> tags = RDSClientMethods.getTags(rdsClient,
                    listTagsForResourceRequest,
                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                    awsAccount_.getMaxApiRequestsPerSecond(),
                    awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();

            JSONObject createSnapshot = new JSONObject();
            for (com.amazonaws.services.rds.model.Tag tag : tags) {
                if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                    createSnapshot.put("Interval", tag.getValue());
                } else if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                    createSnapshot.put("RunAt", tag.getValue());
                } else if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                    createSnapshot.put("Retain", tag.getValue());
                }
            }
            if (createSnapshot.isEmpty()) {
                deleteList.add(dbSnapshot);
            }

        }

        for (DBSnapshot dbSnapshot : deleteList) {
            try {
                dbSnapshots.remove(dbSnapshot);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow\", DBSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        for (DBSnapshot dbSnapshot : dbSnapshots) {
            try {
                Threads.sleepMilliseconds(500);
                int timeSinceCreation = getDaysBetweenNowAndDBSnapshot(dbSnapshot);

                if (timeSinceCreation <= eideticCleanKeepDays_) {
                    continue;
                }

                String instanceId = dbSnapshot.getDBInstanceIdentifier();

                DescribeDBInstancesRequest describeDBInstancesRequest
                        = new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId);
                DescribeDBInstancesResult describeDBInstancesResult
                        = RDSClientMethods.describeDBInstances(rdsClient,
                                describeDBInstancesRequest,
                                numRetries_,
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_);

                List<DBInstance> dbInstances = describeDBInstancesResult.getDBInstances();

                if (!dbInstances.isEmpty()) {

                    List<Tag> tags;
                    try {
                        String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccountId, dbInstances.get(0).getDBInstanceIdentifier());
                        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                        tags = RDSClientMethods.getTags(rdsClient,
                                listTagsForResourceRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_).getTagList();
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting dbInstance in RDSSnapshotCleaner\", DBInstances_toString=\"" + dbInstances.toString() + "\", bSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }
                    if (tags == null) {
                        continue;
                    }
                    Tag instanceTag = null;
                    for (Tag tag : tags) {
                        if ("Eidetic_Interval".equals(tag.getKey())) {
                            instanceTag = tag;
                            break;
                        }
                    }
                    if (instanceTag == null) {
                        continue;
                    }

                    List<Tag> snapshottags;
                    try {
                        String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region.getName(), awsAccountId, dbSnapshot.getDBSnapshotIdentifier());
                        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                        snapshottags = RDSClientMethods.getTags(rdsClient,
                                listTagsForResourceRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_).getTagList();
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting dbInstance in RDSSnapshotCleaner\", DBInstances_toString=\"" + dbInstances.toString() + "\", dbSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }
                    if (snapshottags == null) {
                        continue;
                    }
                    Tag snaptag = null;
                    for (Tag tag : snapshottags) {
                        if ("Eidetic_Interval".equals(tag.getKey())) {
                            snaptag = tag;
                            break;
                        }
                    }
                    if (snaptag == null) {
                        continue;
                    }

                    try { //Checks to see if we modified tag, if it is the same tag the tag retain date m
                        if (snaptag.getValue().equals(instanceTag.getValue())) {
                            continue;
                        }
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error comparing dbInstance and snapshot tag values\", DBInstance_id=\"" + dbInstances.get(0).getDBInstanceIdentifier() + ", Snapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }

                    try {
                        deleteDBSnapshot(rdsClient, dbSnapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", dbSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }

                } else {
                    //DBInstance doesn't exist
                    if (timeSinceCreation > eideticCleanKeepDays_) {
                        //See if old dbInstance still exists. If not, if snap is $eideticCleanKeepDays_ days old, delete.
                        try {
                            deleteDBSnapshot(rdsClient, dbSnapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", dbSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        }
                    }
                }

            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"exeception in workflow\", dbSnapshot_id=\"" + dbSnapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotRequest
                = new DescribeDBClusterSnapshotsRequest();

        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult
                = RDSClientMethods.describeDBClusterSnapshots(rdsClient,
                        describeDBClusterSnapshotRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        List<DBClusterSnapshot> dbClusterSnapshots = describeDBClusterSnapshotsResult.getDBClusterSnapshots();

        List<DBClusterSnapshot> deleteList2 = new ArrayList();
        for (DBClusterSnapshot dbClusterSnapshot : dbClusterSnapshots) {
            Threads.sleepMilliseconds(500);
            String arn = String.format("arn:aws:rds:%s:%s:cluster-snapshot:%s", region.getName(), awsAccount_.getAwsAccountId(), dbClusterSnapshot.getDBClusterSnapshotIdentifier());
            ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
            List<com.amazonaws.services.rds.model.Tag> tags = RDSClientMethods.getTags(rdsClient,
                    listTagsForResourceRequest,
                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                    awsAccount_.getMaxApiRequestsPerSecond(),
                    awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();

            JSONObject createSnapshot = new JSONObject();
            for (com.amazonaws.services.rds.model.Tag tag : tags) {
                if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                    createSnapshot.put("Interval", tag.getValue());
                } else if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                    createSnapshot.put("RunAt", tag.getValue());
                } else if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                    createSnapshot.put("Retain", tag.getValue());
                }
            }
            if (createSnapshot.isEmpty()) {
                deleteList2.add(dbClusterSnapshot);
            }

        }

        for (DBClusterSnapshot dbClusterSnapshot : deleteList2) {
            try {
                dbClusterSnapshots.remove(dbClusterSnapshot);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow\", DBSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        for (DBClusterSnapshot dbClusterSnapshot : dbClusterSnapshots) {
            try {
                int timeSinceCreation = getDaysBetweenNowAndDBClusterSnapshot(dbClusterSnapshot);

                if (timeSinceCreation <= eideticCleanKeepDays_) {
                    continue;
                }

                String clusterId = dbClusterSnapshot.getDBClusterIdentifier();

                DescribeDBClustersRequest describeDBClustersRequest
                        = new DescribeDBClustersRequest().withDBClusterIdentifier(clusterId);
                DescribeDBClustersResult describeDBClustersResult
                        = RDSClientMethods.describeDBClusters(rdsClient,
                                describeDBClustersRequest,
                                numRetries_,
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_);

                List<DBCluster> dbClusters = describeDBClustersResult.getDBClusters();
                
                String marker = describeDBClustersResult.getMarker();
                while (marker != null) {
                    describeDBClustersRequest.setMarker(marker);

                    describeDBClustersResult = RDSClientMethods.describeDBClusters(rdsClient,
                            describeDBClustersRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            awsAccount_.getUniqueAwsAccountIdentifier());

                    dbClusters.addAll(describeDBClustersResult.getDBClusters());

                    marker = describeDBClustersResult.getMarker();
                }

                if (!dbClusters.isEmpty()) {

                    List<Tag> tags;
                    try {
                        Threads.sleepMilliseconds(500);
                        String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region.getName(), awsAccountId, dbClusters.get(0).getDBClusterIdentifier());
                        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                        tags = RDSClientMethods.getTags(rdsClient,
                                listTagsForResourceRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_).getTagList();
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting dbCluster in RDSSnapshotCleaner\", DBClusters_toString=\"" + dbClusters.toString() + "\", bSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }
                    if (tags == null) {
                        continue;
                    }
                    Tag clusterTag = null;
                    for (Tag tag : tags) {
                        if ("Eidetic_Interval".equals(tag.getKey())) {
                            clusterTag = tag;
                            break;
                        }
                    }
                    if (clusterTag == null) {
                        continue;
                    }

                    List<Tag> snapshottags;
                    try {
                        Threads.sleepMilliseconds(500);
                        String arn = String.format("arn:aws:rds:%s:%s:cluster-snapshot:%s", region.getName(), awsAccountId, dbClusterSnapshot.getDBClusterSnapshotIdentifier());
                        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                        snapshottags = RDSClientMethods.getTags(rdsClient,
                                listTagsForResourceRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_).getTagList();
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting dbCluster in RDSSnapshotCleaner\", DBClusters_toString=\"" + dbClusters.toString() + "\", dbClusterSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }
                    if (snapshottags == null) {
                        continue;
                    }
                    Tag snaptag = null;
                    for (Tag tag : snapshottags) {
                        if ("Eidetic_Interval".equals(tag.getKey())) {
                            snaptag = tag;
                            break;
                        }
                    }
                    if (snaptag == null) {
                        continue;
                    }

                    try { //Checks to see if we modified tag, if it is the same tag the tag retain date m
                        if (snaptag.getValue().equals(clusterTag.getValue())) {
                            continue;
                        }
                    } catch (Exception e) {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error comparing dbCluster and snapshot tag values\", DBCluster_id=\"" + dbClusters.get(0).getDBClusterIdentifier() + ", Snapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }

                    try {
                        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Deleting dbClusterSnapshot\", \"dbClusterSnapshot\"=" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\"");

                        deleteDBClusterSnapshot(rdsClient, dbClusterSnapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", dbClusterSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }

                } else {
                    //DBCluster doesn't exist
                    if (timeSinceCreation > eideticCleanKeepDays_) {
                        //See if old dbCluster still exists. If not, if snap is $eideticCleanKeepDays_ days old, delete.
                        try {
                            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Deleting dbClusterSnapshot\", \"dbClusterSnapshot\"=" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\"");

                            deleteDBClusterSnapshot(rdsClient, dbClusterSnapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", dbClusterSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        }
                    }
                }

            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"exeception in workflow\", dbClusterSnapshot_id=\"" + dbClusterSnapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return true;
    }

    public Boolean allSnapshotsClean(AmazonRDSClient rdsClient) {
        if (rdsClient == null) {
            return false;
        }

        DescribeDBSnapshotsRequest describeDBSnapshotRequest2
                = new DescribeDBSnapshotsRequest();
        DescribeDBSnapshotsResult describeDBSnapshotsResult2
                = RDSClientMethods.describeDBSnapshots(rdsClient,
                        describeDBSnapshotRequest2,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        //Delete all snaps allSnapshotCleanDays_ and older.
        List<DBSnapshot> snapshots2 = describeDBSnapshotsResult2.getDBSnapshots();

        for (DBSnapshot snapshot : snapshots2) {
            Integer timeSinceCreation = getDaysBetweenNowAndDBSnapshot(snapshot);
            if (timeSinceCreation > allSnapshotCleanKeepDays_) {
                try {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Deleting dbsnapshot\", \"dbSnapshot\"=" + snapshot.getDBSnapshotIdentifier() + "\"");

                    deleteDBSnapshot(rdsClient, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting dbSnapshot\", dbSnapshot_id=\"" + snapshot.getDBSnapshotIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        }

        DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotRequest2
                = new DescribeDBClusterSnapshotsRequest();
        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult2
                = RDSClientMethods.describeDBClusterSnapshots(rdsClient,
                        describeDBClusterSnapshotRequest2,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
        //Delete all snaps allSnapshotCleanDays_ and older.
        List<DBClusterSnapshot> snapshotsCluster2 = describeDBClusterSnapshotsResult2.getDBClusterSnapshots();
        for (DBClusterSnapshot snapshot : snapshotsCluster2) {
            Integer timeSinceCreation = getDaysBetweenNowAndDBClusterSnapshot(snapshot);
            if (timeSinceCreation > allSnapshotCleanKeepDays_) {
                try {
                    deleteDBClusterSnapshot(rdsClient, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting dbClusterSnapshot\", dbClusterSnapshot_id=\"" + snapshot.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        }

        return true;
    }
}
