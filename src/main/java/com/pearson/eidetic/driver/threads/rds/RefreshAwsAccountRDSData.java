/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.util.Pair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class RefreshAwsAccountRDSData implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RefreshAwsAccountRDSData.class.getName());

    private final AwsAccount awsAccount_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final HashMap<DBInstance, Boolean> DBInstanceNoTimeHasTag_ = new HashMap();
    private final HashMap<DBInstance, Boolean> DBInstanceTimeHasTag_ = new HashMap();

    private final HashMap<DBCluster, Boolean> DBClusterNoTimeHasTag_ = new HashMap();
    private final HashMap<DBCluster, Boolean> DBClusterTimeHasTag_ = new HashMap();

    public RefreshAwsAccountRDSData(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
        this.uniqueAwsAccountIdentifier_ = awsAccount.getUniqueAwsAccountIdentifier();
        this.maxApiRequestsPerSecond_ = awsAccount.getMaxApiRequestsPerSecond();
        this.numRetries_ = ApplicationConfiguration.getAwsCallRetryAttempts();

    }

    @Override
    public void run() {

        while (true) {
            try {
                ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime;

                localDBInstanceNoTime = awsAccount_.getDBInstanceNoTime_Copy();

                ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime;

                localDBInstanceTime = awsAccount_.getDBInstanceTime_Copy();

                ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime;

                localDBClusterNoTime = awsAccount_.getDBClusterNoTime_Copy();

                ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime;

                localDBClusterTime = awsAccount_.getDBClusterTime_Copy();

                JSONParser parser = new JSONParser();

                for (Map.Entry<com.amazonaws.regions.Region, ArrayList<DBInstance>> entry : localDBInstanceNoTime.entrySet()) {
                    com.amazonaws.regions.Region region = entry.getKey();
                    AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

                    List<DBInstance> dbInstances = initialization(rdsClient,
                            localDBInstanceNoTime,
                            localDBInstanceTime,
                            region);

                    Pair<List<DBInstance>, List<DBCluster>> pair = processDBInstances(dbInstances, rdsClient, localDBClusterNoTime, localDBClusterTime, region);

                    dbInstances.clear();
                    dbInstances = pair.getKey();
                    List<DBCluster> dbClusters = pair.getValue();

                    for (DBInstance dbInstance : dbInstances) {

                        JSONObject eideticParameters;
                        eideticParameters = getEideticParameters(rdsClient, dbInstance, region, parser);
                        if (eideticParameters == null) {
                            continue;
                        }

                        JSONObject createSnapshot;
                        createSnapshot = getCreateSnapshot(dbInstance, eideticParameters);
                        if (createSnapshot == null) {
                            continue;
                        }

                        HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBInstance>>> resultSet;
                        resultSet = refreshCreateSnapshotDBInstances(dbInstance, createSnapshot, localDBInstanceTime, localDBInstanceNoTime, region);
                        if (resultSet == null) {
                            continue;
                        }
                        if (resultSet.isEmpty()) {
                            continue;
                        }

                        try {
                            localDBInstanceTime = resultSet.get(0);
                            localDBInstanceNoTime = resultSet.get(1);
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting from resultSet\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        }

                    }

                    for (DBCluster dbCluster : dbClusters) {

                        JSONObject eideticParameters;
                        eideticParameters = getEideticParameters(rdsClient, dbCluster, region, parser);
                        if (eideticParameters == null) {
                            continue;
                        }

                        JSONObject createSnapshot;
                        createSnapshot = getCreateSnapshot(dbCluster, eideticParameters);
                        if (createSnapshot == null) {
                            continue;
                        }

                        HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBCluster>>> resultSet;
                        resultSet = refreshCreateSnapshotDBClusters(dbCluster, createSnapshot, localDBClusterTime, localDBClusterNoTime, region);
                        if (resultSet == null) {
                            continue;
                        }
                        if (resultSet.isEmpty()) {
                            continue;
                        }

                        try {
                            localDBClusterTime = resultSet.get(0);
                            localDBClusterNoTime = resultSet.get(1);
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting from resultSet\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        }

                    }

                    localDBInstanceTime = processLocalDBInstanceTime(localDBInstanceTime, region);
                    localDBInstanceNoTime = processLocalDBInstanceNoTime(localDBInstanceNoTime, region);
                    localDBClusterTime = processLocalDBClusterTime(localDBClusterTime, region);
                    localDBClusterNoTime = processLocalDBClusterNoTime(localDBClusterNoTime, region);

                    rdsClient.shutdown();

                }

                awsAccount_.replaceDBInstanceTime(localDBInstanceTime);
                awsAccount_.replaceDBInstanceNoTime(localDBInstanceNoTime);
                awsAccount_.replaceDBClusterTime(localDBClusterTime);
                awsAccount_.replaceDBClusterNoTime(localDBClusterNoTime);
                Threads.sleepMinutes(30);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"exception in RefreshAwsAccountRDSData workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

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

    public List<DBInstance> initialization(AmazonRDSClient rdsClient,
            ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime,
            ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime,
            Region region) {

        DescribeDBInstancesRequest describeDBInstancesRequest
                = new DescribeDBInstancesRequest();
        DescribeDBInstancesResult describeDBInstancesResult
                = RDSClientMethods.describeDBInstances(region,
                        rdsClient,
                        describeDBInstancesRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        for (DBInstance dbInstance : localDBInstanceNoTime.get(region)) {
            DBInstanceNoTimeHasTag_.put(dbInstance, false);
        }
        for (DBInstance dbInstance : localDBInstanceTime.get(region)) {
            DBInstanceTimeHasTag_.put(dbInstance, false);
        }

        List<DBInstance> dbInstances = describeDBInstancesResult.getDBInstances();

        String marker = describeDBInstancesResult.getMarker();
        while (marker != null) {
            describeDBInstancesRequest.setMarker(marker);

            describeDBInstancesResult
                    = RDSClientMethods.describeDBInstances(region,
                            rdsClient,
                            describeDBInstancesRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            awsAccount_.getUniqueAwsAccountIdentifier());

            dbInstances.addAll(describeDBInstancesResult.getDBInstances());

            marker = describeDBInstancesResult.getMarker();
        }

        List<DBInstance> deleteList = new ArrayList();
        for (DBInstance dbInstance : dbInstances) {

            if (!(dbInstance.getDBClusterIdentifier() == null)) {
                Threads.sleepMilliseconds(180);
                List<com.amazonaws.services.rds.model.Tag> tags;
                try {
                    String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccount_.getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
                    ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                    tags = RDSClientMethods.getTags(region,
                            rdsClient,
                            listTagsForResourceRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }

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
                    deleteList.add(dbInstance);
                }
            }

        }

        for (DBInstance dbInstance : deleteList) {
            try {
                dbInstances.remove(dbInstance);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return dbInstances;
    }

    public Pair<List<DBInstance>, List<DBCluster>> processDBInstances(List<DBInstance> dbInstances,
            AmazonRDSClient rdsClient,
            ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime,
            ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime,
            Region region) {

        for (DBCluster dbCluster : localDBClusterNoTime.get(region)) {
            DBClusterNoTimeHasTag_.put(dbCluster, false);
        }
        for (DBCluster dbCluster : localDBClusterTime.get(region)) {
            DBClusterTimeHasTag_.put(dbCluster, false);
        }

        List<DBInstance> dbInstances2 = new ArrayList();
        for (DBInstance dbInstance : dbInstances) {
            DBInstance temp = dbInstance.clone();
            dbInstances2.add(temp);
        }
        List<DBCluster> dbClusters2 = new ArrayList();

        for (DBInstance dbInstance : dbInstances) {
            if (dbInstance.getDBClusterIdentifier() != null) {
                DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest().withDBClusterIdentifier(dbInstance.getDBClusterIdentifier());
                DescribeDBClustersResult describeDBClustersResult = RDSClientMethods.describeDBClusters(region,
                        rdsClient,
                        describeDBClustersRequest,
                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
                List<DBCluster> dbClusters = describeDBClustersResult.getDBClusters();

                String marker = describeDBClustersResult.getMarker();
                while (marker != null) {
                    describeDBClustersRequest.setMarker(marker);

                    describeDBClustersResult = RDSClientMethods.describeDBClusters(region,
                            rdsClient,
                            describeDBClustersRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            awsAccount_.getMaxApiRequestsPerSecond(),
                            awsAccount_.getUniqueAwsAccountIdentifier());

                    dbClusters.addAll(describeDBClustersResult.getDBClusters());

                    marker = describeDBClustersResult.getMarker();
                }
                for (DBCluster dbCluster : dbClusters) {
                    if (!dbClusters2.contains(dbCluster)) {
                        dbClusters2.add(dbCluster);
                    }
                }
                dbInstances2.remove(dbInstance);
            }
        }
        Pair<List<DBInstance>, List<DBCluster>> pair = new Pair(dbInstances2, dbClusters2);
        return pair;
    }

    private JSONObject getEideticParameters(AmazonRDSClient rdsClient, DBInstance dbInstance, Region region, JSONParser parser) {
        List<Tag> tags;
        Threads.sleepMilliseconds(80);
        String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccount_.getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        tags = RDSClientMethods.getTags(region,
                rdsClient,
                listTagsForResourceRequest,
                ApplicationConfiguration.getAwsCallRetryAttempts(),
                awsAccount_.getMaxApiRequestsPerSecond(),
                awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();

        if (tags == null) {
            return null;
        }
        JSONObject eideticParameters = new JSONObject();
        JSONObject createSnapshot = new JSONObject();

        for (Tag tag : tags) {
            String tagValue;
            if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                tagValue = tag.getValue();
                createSnapshot.put("Interval", tagValue);
            } else if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                tagValue = tag.getValue();
                createSnapshot.put("RunAt", tagValue);
            } else if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                tagValue = tag.getValue();
                createSnapshot.put("Retain", tagValue);
            }
        }
        eideticParameters.put("CreateSnapshot", createSnapshot);
        return eideticParameters;
    }

    private JSONObject getEideticParameters(AmazonRDSClient rdsClient, DBCluster dbCluster, Region region, JSONParser parser) {
        List<Tag> tags;
        Threads.sleepMilliseconds(80);

        String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region.getName(), awsAccount_.getAwsAccountId(), dbCluster.getDBClusterIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        tags = RDSClientMethods.getTags(region,
                rdsClient,
                listTagsForResourceRequest,
                ApplicationConfiguration.getAwsCallRetryAttempts(),
                awsAccount_.getMaxApiRequestsPerSecond(),
                awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();
        if (tags == null) {
            return null;
        }
        JSONObject eideticParameters = new JSONObject();
        JSONObject createSnapshot = new JSONObject();

        for (Tag tag : tags) {
            String tagValue = null;
            if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                tagValue = tag.getValue();
                createSnapshot.put("Interval", tagValue);
            } else if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                tagValue = tag.getValue();
                createSnapshot.put("RunAt", tagValue);
            } else if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                tagValue = tag.getValue();
                createSnapshot.put("Retain", tagValue);
            }
        }
        eideticParameters.put("CreateSnapshot", createSnapshot);
        return eideticParameters;
    }

    private JSONObject getCreateSnapshot(DBInstance dbInstance, JSONObject eideticParameters) {
        JSONObject createSnapshot = null;
        try {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
        return createSnapshot;
    }

    private JSONObject getCreateSnapshot(DBCluster dbCluster, JSONObject eideticParameters) {
        JSONObject createSnapshot = null;
        try {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
        return createSnapshot;
    }

    private HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBInstance>>> refreshCreateSnapshotDBInstances(DBInstance dbInstance, JSONObject createSnapshot, ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime, ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime, Region region) {
        HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBInstance>>> resultSet = new HashMap();
        //New dbInstance detected! Return value continue     
        if (!localDBInstanceNoTime.get(region).contains(dbInstance) && !localDBInstanceTime.get(region).contains(dbInstance)) {
            if (createSnapshot.containsKey("RunAt")) {//Add tries
                try {
                    localDBInstanceTime.get(region).add(dbInstance);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBInstance to DBInstanceTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                DBInstanceTimeHasTag_.put(dbInstance, true);
            } else {
                try {
                    localDBInstanceNoTime.get(region).add(dbInstance);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBInstance to DBInstanceNoTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                DBInstanceNoTimeHasTag_.put(dbInstance, true);
            }
            resultSet.put(0, localDBInstanceTime);
            resultSet.put(1, localDBInstanceNoTime);
            return resultSet;
        }

        //Old dbInstance Tag is left unchanged
        if ((createSnapshot.containsKey("RunAt")) && localDBInstanceTime.get(region).contains(dbInstance)) {
            DBInstanceTimeHasTag_.replace(dbInstance, true);
        } else if (!(createSnapshot.containsKey("RunAt")) && localDBInstanceNoTime.get(region).contains(dbInstance)) {
            DBInstanceNoTimeHasTag_.replace(dbInstance, true);

            //If we change the value of the tag.
        } else if ((createSnapshot.containsKey("RunAt")) && localDBInstanceNoTime.get(region).contains(dbInstance)) {
            DBInstanceTimeHasTag_.put(dbInstance, true);
            DBInstanceNoTimeHasTag_.replace(dbInstance, false);
            try {
                localDBInstanceTime.get(region).add(dbInstance);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBInstance to DBInstanceTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else if (!(createSnapshot.containsKey("RunAt")) && localDBInstanceTime.get(region).contains(dbInstance)) {
            DBInstanceNoTimeHasTag_.put(dbInstance, true);
            DBInstanceTimeHasTag_.replace(dbInstance, false);
            try {
                localDBInstanceNoTime.get(region).add(dbInstance);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBInstance to DBInstanceNoTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\"");
        }
        resultSet.put(0, localDBInstanceTime);
        resultSet.put(1, localDBInstanceNoTime);
        return resultSet;
    }

    //Flip flop 
    private HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBCluster>>> refreshCreateSnapshotDBClusters(DBCluster dbCluster, JSONObject createSnapshot, ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime, ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime, Region region) {
        HashMap<Integer, ConcurrentHashMap<Region, ArrayList<DBCluster>>> resultSet = new HashMap();
        //New dbCluster detected! Return value continue     
        if (!localDBClusterNoTime.get(region).contains(dbCluster) && !localDBClusterTime.get(region).contains(dbCluster)) {
            if (createSnapshot.containsKey("RunAt")) {//Add tries
                try {
                    localDBClusterTime.get(region).add(dbCluster);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBCluster to DBClusterTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                DBClusterTimeHasTag_.put(dbCluster, true);
            } else {
                try {
                    localDBClusterNoTime.get(region).add(dbCluster);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBCluster to DBClusterTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                DBClusterNoTimeHasTag_.put(dbCluster, true);
            }
            resultSet.put(0, localDBClusterTime);
            resultSet.put(1, localDBClusterNoTime);
            return resultSet;
        }

        //Old dbCluster Tag is left unchanged
        if ((createSnapshot.containsKey("RunAt")) && localDBClusterTime.get(region).contains(dbCluster)) {
            DBClusterTimeHasTag_.replace(dbCluster, true);
        } else if (!(createSnapshot.containsKey("RunAt")) && localDBClusterNoTime.get(region).contains(dbCluster)) {
            DBClusterNoTimeHasTag_.replace(dbCluster, true);

            //If we change the value of the tag.
        } else if ((createSnapshot.containsKey("RunAt")) && localDBClusterNoTime.get(region).contains(dbCluster)) {
            DBClusterTimeHasTag_.put(dbCluster, true);
            DBClusterNoTimeHasTag_.replace(dbCluster, false);
            try {
                localDBClusterTime.get(region).add(dbCluster);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBCluster to DBClusterNoTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else if (!(createSnapshot.containsKey("RunAt")) && localDBClusterTime.get(region).contains(dbCluster)) {
            DBClusterNoTimeHasTag_.put(dbCluster, true);
            DBClusterTimeHasTag_.replace(dbCluster, false);
            try {
                localDBClusterNoTime.get(region).add(dbCluster);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding DBCluster to DBClusterNoTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\"");
        }
        resultSet.put(0, localDBClusterTime);
        resultSet.put(1, localDBClusterNoTime);
        return resultSet;
    }

    private ConcurrentHashMap<Region, ArrayList<DBInstance>> processLocalDBInstanceTime(ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime, Region region) {
        for (Map.Entry pair : DBInstanceTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            DBInstance dbInstance = (DBInstance) pair.getKey();
            try {
                localDBInstanceTime.get(region).remove(dbInstance);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing dbInstance from DBInstanceTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        DBInstanceTimeHasTag_.clear();
        return localDBInstanceTime;
    }

    private ConcurrentHashMap<Region, ArrayList<DBInstance>> processLocalDBInstanceNoTime(ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceNoTime, Region region) {
        for (Map.Entry pair : DBInstanceNoTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            DBInstance dbInstance = (DBInstance) pair.getKey();
            try {
                localDBInstanceNoTime.get(region).remove(dbInstance);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing dbInstance from DBInstanceNoTime_\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        DBInstanceNoTimeHasTag_.clear();
        return localDBInstanceNoTime;
    }

    private ConcurrentHashMap<Region, ArrayList<DBCluster>> processLocalDBClusterNoTime(ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterNoTime, Region region) {
        for (Map.Entry pair : DBClusterNoTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            DBCluster dbCluster = (DBCluster) pair.getKey();
            try {
                localDBClusterNoTime.get(region).remove(dbCluster);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing dbCluster from DBClusterNoTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        DBClusterNoTimeHasTag_.clear();
        return localDBClusterNoTime;
    }

    private ConcurrentHashMap<Region, ArrayList<DBCluster>> processLocalDBClusterTime(ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime, Region region) {
        for (Map.Entry pair : DBClusterTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            DBCluster dbCluster = (DBCluster) pair.getKey();
            try {
                localDBClusterTime.get(region).remove(dbCluster);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing dbCluster from DBClusterTime_\", DBCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        DBClusterTimeHasTag_.clear();
        return localDBClusterTime;
    }

}
