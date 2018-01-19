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
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
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
import com.pearson.eidetic.driver.MonitorRDS;
import com.pearson.eidetic.driver.MonitorRDSMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class MonitorRDSTagPropagator extends MonitorRDSMethods implements Runnable, MonitorRDS {

    private static final Logger logger = LoggerFactory.getLogger(MonitorRDSTagPropagator.class.getName());

    private final AwsAccount awsAccount_;

    public MonitorRDSTagPropagator(AwsAccount awsAccount) {
        if (awsAccount == null) {
            throw new IllegalArgumentException("null is not valid for AWSAccount");
        }

        this.awsAccount_ = awsAccount;
    }

    @Override
    public void run() {

        while (true) {
            try {
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : awsAccount_.getDBInstanceNoTime_Copy().entrySet()) {
                    com.amazonaws.regions.Region region = entry.getKey();
                    AmazonRDSClient rdsClient;
                    String endpoint = "rds." + region.getName() + ".amazonaws.com";

                    AWSCredentials credentials = new BasicAWSCredentials(awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());
                    ClientConfiguration clientConfig = new ClientConfiguration();
                    clientConfig.setProtocol(Protocol.HTTPS);

                    rdsClient = new AmazonRDSClient(credentials, clientConfig);
                    rdsClient.setRegion(region);
                    rdsClient.setEndpoint(endpoint);

                    DescribeDBInstancesRequest describeDBInstancesRequest
                            = new DescribeDBInstancesRequest();

                    DescribeDBInstancesResult describeDBInstancesResult
                            = RDSClientMethods.describeDBInstances(region,
                                    rdsClient,
                                    describeDBInstancesRequest,
                                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                                    awsAccount_.getMaxApiRequestsPerSecond(),
                                    awsAccount_.getUniqueAwsAccountIdentifier());

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

                    for (DBInstance dbInstance : dbInstances) {

                        if (!(dbInstance.getDBClusterIdentifier() == null)) {
                            Threads.sleepMilliseconds(80);
                            String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccount_.getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
                            ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                            List<com.amazonaws.services.rds.model.Tag> tags = RDSClientMethods.getTags(region,
                                    rdsClient,
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
                                continue;
                            }

                            DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest().withDBClusterIdentifier(dbInstance.getDBClusterIdentifier());
                            DescribeDBClustersResult describeDBClustersResult = RDSClientMethods.describeDBClusters(region,
                                    rdsClient,
                                    describeDBClustersRequest,
                                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                                    awsAccount_.getMaxApiRequestsPerSecond(),
                                    awsAccount_.getUniqueAwsAccountIdentifier());
                            List<DBCluster> dbClusters = describeDBClustersResult.getDBClusters();

                            marker = describeDBClustersResult.getMarker();
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

                            for (DBCluster dbCluster : dbClusters) {
                                Threads.sleepMilliseconds(80);
                                String clusterarn = String.format("arn:aws:rds:%s:%s:cluster:%s", region.getName(), awsAccount_.getAwsAccountId(), dbCluster.getDBClusterIdentifier());

                                ListTagsForResourceRequest listTagsForResourceRequest2 = new ListTagsForResourceRequest().withResourceName(clusterarn);
                                List<com.amazonaws.services.rds.model.Tag> tags2 = RDSClientMethods.getTags(region, 
                                        rdsClient,
                                        listTagsForResourceRequest2,
                                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                                        awsAccount_.getMaxApiRequestsPerSecond(),
                                        awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();
                                JSONObject createSnapshot2 = new JSONObject();
                                for (com.amazonaws.services.rds.model.Tag tag : tags2) {
                                    if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                                        createSnapshot2.put("Interval", tag.getValue());
                                    } else if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                                        createSnapshot2.put("RunAt", tag.getValue());
                                    } else if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                                        createSnapshot2.put("Retain", tag.getValue());
                                    }
                                }

                                Boolean change = false;
                                if (createSnapshot2.isEmpty()) {
                                    tags2.add(new Tag().withKey("Eidetic_Interval").withValue(createSnapshot.get("Interval").toString()));
                                    tags2.add(new Tag().withKey("Eidetic_Retain").withValue(createSnapshot.get("Retain").toString()));
                                    if (createSnapshot.containsKey("RunAt")) {
                                        tags2.add(new Tag().withKey("Eidetic_RunAt").withValue(createSnapshot.get("RunAt").toString()));
                                    }
                                    change = true;
                                } else {
                                    if (createSnapshot2.containsKey("Interval") && !createSnapshot2.get("Interval").equals(createSnapshot.get("Interval"))) {
                                        for (com.amazonaws.services.rds.model.Tag tag : tags2) {
                                            if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                                                tag.setValue(createSnapshot.get("Eidetic").toString());
                                                break;
                                            }
                                        }
                                        change = true;
                                    }
                                    if (createSnapshot2.containsKey("Retain") && !createSnapshot2.get("Retain").equals(createSnapshot.get("Retain"))) {
                                        for (com.amazonaws.services.rds.model.Tag tag : tags2) {
                                            if (tag.getKey().equalsIgnoreCase("Eidetic_Retain")) {
                                                tag.setValue(createSnapshot.get("Retain").toString());
                                                break;
                                            }
                                        }
                                        change = true;
                                    }
                                    if (createSnapshot2.containsKey("RunAt") && !createSnapshot.containsKey("RunAt")) {
                                        com.amazonaws.services.rds.model.Tag mytag = null;
                                        for (com.amazonaws.services.rds.model.Tag tag : tags2) {
                                            if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                                                mytag = tag;
                                                break;
                                            }
                                        }
                                        if (mytag != null) {
                                            tags2.remove(mytag);
                                        }
                                        change = true;
                                    }
                                    if (createSnapshot2.containsKey("RunAt") && createSnapshot.containsKey("RunAt") && !createSnapshot2.get("RunAt").equals(createSnapshot.get("RunAt"))) {
                                        for (com.amazonaws.services.rds.model.Tag tag : tags2) {
                                            if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                                                tag.setValue(createSnapshot.get("RunAt").toString());
                                                break;
                                            }
                                        }
                                        change = true;
                                    }

                                }

                                if (change) {
                                    ArrayList<Tag> removeTags = new ArrayList();
                                    for (Tag tag : tags2) {
                                        if (tag.getKey().startsWith("aws:")) {
                                            removeTags.add(tag);
                                        }
                                    }
                                    for (Tag tag : removeTags) {
                                        try {
                                            tags2.remove(tag);
                                        } catch (Exception e) {
                                            logger.error("awsAccountNickname=\"" + awsAccount_.getAwsAccountId() + "\", could not remove tag from tags: " + dbInstance.getDBInstanceIdentifier() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                                        }
                                    }

                                    setResourceTags(region, rdsClient, clusterarn, tags2, ApplicationConfiguration.getAwsCallRetryAttempts(),
                                            awsAccount_.getMaxApiRequestsPerSecond(),
                                            awsAccount_.getUniqueAwsAccountIdentifier());
                                }
                            }

                        }

                    }
                }

                Threads.sleepMinutes(10);

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorErrorCheckerFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                Threads.sleepSeconds(10);
            }
        }

    }

    public void setResourceTags(Region region, AmazonRDSClient rdsClient, String arn, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        AddTagsToResourceRequest addTagsToResourceRequest = new AddTagsToResourceRequest().withResourceName(arn);
        addTagsToResourceRequest.setTags(tags);
        RDSClientMethods.createTags(region,
                rdsClient,
                addTagsToResourceRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
    }

}
