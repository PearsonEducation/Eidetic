package com.pearson.eidetic.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;

import com.pearson.eidetic.globals.ApplicationConfiguration;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.HashSet;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * @author Judah Walker
 */
public class AwsAccount {

    private static final Logger logger = LoggerFactory.getLogger(AwsAccount.class.getName());

    private final int index_;
    private final String awsNickname_;
    private final String awsAccountId_;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Boolean prohibitRDSCalls_;

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeNoTime_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeTime_ = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSync_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSyncValidate_ = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> CopyVolumeSnapshots_ = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceNoTime_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceTime_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> DBClusterNoTime_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> DBClusterTime_ = new ConcurrentHashMap<>();

    public AwsAccount(int index, String awsNickname,
            String awsAccessKeyId, String awsSecretKey, Integer maxApiRequestsPerSecond, Boolean prohibitRDSCalls) {

        List<com.amazonaws.regions.Region> regions = com.amazonaws.regions.RegionUtils.getRegions();
        try {
            for (com.amazonaws.regions.Region region : regions) {

                if (Regions.GovCloud.getName().equals(region.getName()) || Regions.CN_NORTH_1.getName().equals(region.getName())) {
                    continue;
                }

                VolumeNoTime_.put(region, new ArrayList<Volume>());
                VolumeTime_.put(region, new ArrayList<Volume>());
                CopyVolumeSnapshots_.put(region, new ArrayList<Volume>());
                VolumeSyncValidate_.put(region, new ArrayList<Volume>());
                VolumeSync_.put(region, new ArrayList<Volume>());

                DBInstanceNoTime_.put(region, new ArrayList<DBInstance>());
                DBInstanceTime_.put(region, new ArrayList<DBInstance>());
                DBClusterNoTime_.put(region, new ArrayList<DBCluster>());
                DBClusterTime_.put(region, new ArrayList<DBCluster>());

            }
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        this.index_ = index;
        this.awsNickname_ = awsNickname;
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        //Removing awsAccessKeyId from logs.
        //this.uniqueAwsAccountIdentifier_ = awsNickname_ + "~" + awsAccessKeyId_ + "~" + index_;
        this.uniqueAwsAccountIdentifier_ = awsNickname_ + index_;
        this.prohibitRDSCalls_ = prohibitRDSCalls;

        this.awsAccountId_ = retrieveAWSAccountID();

    }

    private String retrieveAWSAccountID() {
        try {
            AWSCredentials creds = new BasicAWSCredentials(awsAccessKeyId_, awsSecretKey_);
            AmazonIdentityManagementClient iam = new AmazonIdentityManagementClient(creds);
            return iam.getUser().getUser().getArn().split(":")[4];
        } catch (AmazonClientException e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Could not get Aws Account ID\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            throw new RuntimeException("Failed to get AWS account id", e);
        }
    }

    public int getIndex() {
        return index_;
    }

    public String getAwsNickname() {
        return awsNickname_;
    }

    public String getAwsAccountId() {
        return awsAccountId_;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId_;
    }

    public String getAwsSecretKey() {
        return awsSecretKey_;
    }

    public String getUniqueAwsAccountIdentifier() {
        return uniqueAwsAccountIdentifier_;
    }

    public Integer getMaxApiRequestsPerSecond() {
        return maxApiRequestsPerSecond_;
    }

    public void initializeEC2Snapshots() {
        JSONParser parser = new JSONParser();

        for (Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : VolumeNoTime_.entrySet()) {
            com.amazonaws.regions.Region region = entry.getKey();
            AmazonEC2Client ec2Client;
            String endpoint = "ec2." + region.getName() + ".amazonaws.com";

            AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId_, awsSecretKey_);
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProtocol(Protocol.HTTPS);

            ec2Client = new AmazonEC2Client(credentials, clientConfig);
            ec2Client.setRegion(region);
            ec2Client.setEndpoint(endpoint);

            Filter[] filters = new Filter[1];
            filters[0] = new Filter().withName("tag-key").withValues("Eidetic");
            //filters[0] = new Filter().withName("tag:Eidetic");

            DescribeVolumesRequest describeVolumesRequest
                    = new DescribeVolumesRequest().withFilters(filters);
            DescribeVolumesResult describeVolumeResult
                    = EC2ClientMethods.describeVolumes(ec2Client,
                            describeVolumesRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            maxApiRequestsPerSecond_,
                            uniqueAwsAccountIdentifier_);

            List<Volume> volumes = describeVolumeResult.getVolumes();

            for (Volume volume : volumes) {
                for (Tag tag : volume.getTags()) {
                    String tagValue = null;
                    if (tag.getKey().equalsIgnoreCase("Eidetic")) {
                        tagValue = tag.getValue();
                    }
                    if (tagValue == null) {
                        continue;
                    }

                    JSONObject eideticParameters;
                    try {
                        Object obj = parser.parse(tagValue);
                        eideticParameters = (JSONObject) obj;
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                        continue;
                    }

                    if (eideticParameters.containsKey("SyncSnapshot")) {
                        JSONObject syncSnapshot;
                        try {
                            syncSnapshot = (JSONObject) eideticParameters.get("SyncSnapshot");
                            if (syncSnapshot.containsKey("Validate")) {
                                JSONObject validate = (JSONObject) syncSnapshot.get("Validate");
                                validate.get("Cluster");
                                validate.get("CreateAfter");
                                VolumeSyncValidate_.get(region).add(volume);
                            }
                            VolumeSync_.get(region).add(volume);
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                            continue;
                        }
                    }

                    JSONObject createSnapshot;
                    if (eideticParameters.containsKey("CreateSnapshot")) {
                        try {
                            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
                        } catch (Exception e) {
                            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                            continue;
                        }

                        if (createSnapshot != null && createSnapshot.containsKey("RunAt")) {
                            VolumeTime_.get(region).add(volume);
                        } else {
                            VolumeNoTime_.get(region).add(volume);
                        }

                    }

                    if (eideticParameters.containsKey("CopySnapshot")) {
                        CopyVolumeSnapshots_.get(region).add(volume);
                    }
                    break;
                }
            }
            ec2Client.shutdown();

        }
    }

    public void initializeRDSSnapshots() {
        for (Entry<com.amazonaws.regions.Region, ArrayList<DBInstance>> entry : DBInstanceNoTime_.entrySet()) {
            com.amazonaws.regions.Region region = entry.getKey();
            AmazonRDS amazonRDSClient;
            String endpoint = "rds." + region.getName() + ".amazonaws.com";

            AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId_, awsSecretKey_);
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProtocol(Protocol.HTTPS);

            amazonRDSClient = new AmazonRDSClient(credentials, clientConfig);
            amazonRDSClient.setRegion(region);
            amazonRDSClient.setEndpoint(endpoint);

            DescribeDBInstancesRequest describeDBInstancesRequest
                    = new DescribeDBInstancesRequest();
            DescribeDBInstancesResult describeDBInstancesResult
                    = RDSClientMethods.describeDBInstances(amazonRDSClient,
                            describeDBInstancesRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            maxApiRequestsPerSecond_,
                            uniqueAwsAccountIdentifier_);

            List<DBInstance> dbInstances = describeDBInstancesResult.getDBInstances();
            String marker = describeDBInstancesResult.getMarker();
            while (marker != null) {
                describeDBInstancesRequest.setMarker(marker);

                describeDBInstancesResult
                        = RDSClientMethods.describeDBInstances(amazonRDSClient,
                                describeDBInstancesRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                maxApiRequestsPerSecond_,
                                uniqueAwsAccountIdentifier_);

                dbInstances.addAll(describeDBInstancesResult.getDBInstances());

                marker = describeDBInstancesResult.getMarker();
            }
            List<DBInstance> deleteList = new ArrayList();
            for (DBInstance dbInstance : dbInstances) {

                if (!(dbInstance.getDBClusterIdentifier() == null)) {
                    Threads.sleepMilliseconds(50);
                    List<com.amazonaws.services.rds.model.Tag> tags;
                    try {
                        String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
                        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                        tags = RDSClientMethods.getTags(amazonRDSClient,
                                listTagsForResourceRequest,
                                ApplicationConfiguration.getAwsCallRetryAttempts(),
                                getMaxApiRequestsPerSecond(),
                                getUniqueAwsAccountIdentifier()).getTagList();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow, can not get tags\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
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

            HashSet<DBCluster> seenDBClusters = new HashSet();

            for (DBInstance dbInstance : dbInstances) {
                Threads.sleepMilliseconds(50);

                List<com.amazonaws.services.rds.model.Tag> tags;
                try {
                    String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccountId_, dbInstance.getDBInstanceIdentifier());
                    ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                    tags = RDSClientMethods.getTags(amazonRDSClient,
                            listTagsForResourceRequest,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            maxApiRequestsPerSecond_,
                            uniqueAwsAccountIdentifier_).getTagList();
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Error in workflow, can not get tags\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }
                for (com.amazonaws.services.rds.model.Tag tag : tags) {

                    String tagValue = null;
                    if (tag.getKey().equalsIgnoreCase("Eidetic_Interval")) {
                        tagValue = tag.getValue();
                    }

                    if (tagValue == null) {
                        continue;
                    }

                    //DBInstanceNoTime_
                    //DBInstanceTime_
                    //DBClusterNoTime_
                    //DBClusterTime_
                    Boolean contains_runat = false;
                    for (com.amazonaws.services.rds.model.Tag tag2 : tags) {
                        if (tag2.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                            contains_runat = true;
                            break;
                        }
                    }

                    if (contains_runat) {
                        if (dbInstance.getDBClusterIdentifier() != null) {
                            //DescribeDBClustersResult	describeDBClusters(DescribeDBClustersRequest request)
                            DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest().withDBClusterIdentifier(dbInstance.getDBClusterIdentifier());
                            DescribeDBClustersResult describeDBClustersResult = RDSClientMethods.describeDBClusters(amazonRDSClient,
                                    describeDBClustersRequest,
                                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                                    maxApiRequestsPerSecond_,
                                    uniqueAwsAccountIdentifier_);
                            List<DBCluster> dbClusters = describeDBClustersResult.getDBClusters();
                            for (DBCluster dbCluster : dbClusters) {
                                if (!seenDBClusters.contains(dbCluster)) {
                                    DBClusterTime_.get(region).add(dbCluster);
                                    seenDBClusters.add(dbCluster);
                                }
                            }
                        } else {
                            DBInstanceTime_.get(region).add(dbInstance);
                        }
                    } else {
                        if (dbInstance.getDBClusterIdentifier() != null) {
                            //DescribeDBClustersResult	describeDBClusters(DescribeDBClustersRequest request)
                            DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest().withDBClusterIdentifier(dbInstance.getDBClusterIdentifier());
                            DescribeDBClustersResult describeDBClustersResult = RDSClientMethods.describeDBClusters(amazonRDSClient,
                                    describeDBClustersRequest,
                                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                                    maxApiRequestsPerSecond_,
                                    uniqueAwsAccountIdentifier_);
                            List<DBCluster> dbClusters = describeDBClustersResult.getDBClusters();
                            for (DBCluster dbCluster : dbClusters) {
                                if (!seenDBClusters.contains(dbCluster)) {
                                    DBClusterNoTime_.get(region).add(dbCluster);
                                    seenDBClusters.add(dbCluster);
                                }
                            }
                        } else {
                            DBInstanceNoTime_.get(region).add(dbInstance);
                        }
                    }

                    break;
                }
            }
            amazonRDSClient.shutdown();

        }
    }

    public void replaceVolumeNoTime(ConcurrentHashMap<Region, ArrayList<Volume>> newVolumeNoTime) {
        synchronized (VolumeNoTime_) {
            VolumeNoTime_.clear();
            VolumeNoTime_.putAll(newVolumeNoTime);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> getVolumeNoTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> volumeNoTime_Copy = new ConcurrentHashMap<>();

        synchronized (VolumeNoTime_) {
            for (Region region : VolumeNoTime_.keySet()) {
                ArrayList<Volume> volumes = VolumeNoTime_.get(region);
                ArrayList volumeCopy = new ArrayList<>(volumes);
                volumeNoTime_Copy.put(region, volumeCopy);
            }
        }

        return volumeNoTime_Copy;
    }

    public void replaceVolumeTime(ConcurrentHashMap<Region, ArrayList<Volume>> newVolumeTime) {
        synchronized (VolumeTime_) {
            VolumeTime_.clear();
            VolumeTime_.putAll(newVolumeTime);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> getVolumeTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> volumeTime_Copy = new ConcurrentHashMap<>();

        synchronized (VolumeTime_) {
            for (Region region : VolumeTime_.keySet()) {
                ArrayList<Volume> volumes = VolumeTime_.get(region);
                ArrayList volumeCopy = new ArrayList<>(volumes);
                volumeTime_Copy.put(region, volumeCopy);
            }
        }

        return volumeTime_Copy;
    }

    public void replaceVolumeSync(ConcurrentHashMap<Region, ArrayList<Volume>> newVolumeSync) {
        synchronized (VolumeSync_) {
            VolumeSync_.clear();
            VolumeSync_.putAll(newVolumeSync);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> getVolumeSync_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSync_Copy = new ConcurrentHashMap<>();

        synchronized (VolumeSync_) {
            for (Region region : VolumeSync_.keySet()) {
                ArrayList<Volume> volumes = VolumeSync_.get(region);
                ArrayList volumeCopy = new ArrayList<>(volumes);
                VolumeSync_Copy.put(region, volumeCopy);
            }
        }

        return VolumeSync_Copy;
    }

    public void replaceVolumeSyncValidate(ConcurrentHashMap<Region, ArrayList<Volume>> newVolumeSyncValidate) {
        synchronized (VolumeSyncValidate_) {
            VolumeSyncValidate_.clear();
            VolumeSyncValidate_.putAll(newVolumeSyncValidate);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> getVolumeSyncValidate_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSyncValidate_Copy = new ConcurrentHashMap<>();

        synchronized (VolumeSyncValidate_) {
            for (Region region : VolumeSyncValidate_.keySet()) {
                ArrayList<Volume> volumes = VolumeSyncValidate_.get(region);
                ArrayList volumeCopy = new ArrayList<>(volumes);
                VolumeSyncValidate_Copy.put(region, volumeCopy);
            }
        }

        return VolumeSyncValidate_Copy;
    }

    public void replaceCopyVolumeSnapshots(ConcurrentHashMap<Region, ArrayList<Volume>> newCopyVolumeSnapshots) {
        synchronized (CopyVolumeSnapshots_) {
            CopyVolumeSnapshots_.clear();
            CopyVolumeSnapshots_.putAll(newCopyVolumeSnapshots);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> getCopyVolumeSnapshots_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> CopyVolumeSnapshots_Copy = new ConcurrentHashMap<>();

        synchronized (CopyVolumeSnapshots_) {
            for (Region region : CopyVolumeSnapshots_.keySet()) {
                ArrayList<Volume> volumes = CopyVolumeSnapshots_.get(region);
                ArrayList volumeCopy = new ArrayList<>(volumes);
                CopyVolumeSnapshots_Copy.put(region, volumeCopy);
            }
        }

        return CopyVolumeSnapshots_Copy;
    }

    //private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceNoTime_ = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceTime_ = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> RDSClusterNoTime_ = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> RDSClusterTime_ = new ConcurrentHashMap<>();
    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> getDBInstanceNoTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceNoTime_Copy = new ConcurrentHashMap<>();

        synchronized (DBInstanceNoTime_) {
            for (Region region : DBInstanceNoTime_.keySet()) {
                ArrayList<DBInstance> dbInstance = DBInstanceNoTime_.get(region);
                ArrayList dbInstances = new ArrayList<>(dbInstance);
                DBInstanceNoTime_Copy.put(region, dbInstances);
            }
        }

        return DBInstanceNoTime_Copy;
    }

    public void replaceDBInstanceNoTime(ConcurrentHashMap<Region, ArrayList<DBInstance>> newDBInstanceNoTime) {
        synchronized (DBInstanceNoTime_) {
            DBInstanceNoTime_.clear();
            DBInstanceNoTime_.putAll(newDBInstanceNoTime);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> getDBInstanceTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBInstance>> DBInstanceTime_Copy = new ConcurrentHashMap<>();

        synchronized (DBInstanceTime_) {
            for (Region region : DBInstanceTime_.keySet()) {
                ArrayList<DBInstance> dbInstance = DBInstanceTime_.get(region);
                ArrayList dbInstances = new ArrayList<>(dbInstance);
                DBInstanceTime_Copy.put(region, dbInstances);
            }
        }

        return DBInstanceTime_Copy;
    }

    public void replaceDBInstanceTime(ConcurrentHashMap<Region, ArrayList<DBInstance>> newDBInstanceTime) {
        synchronized (DBInstanceTime_) {
            DBInstanceTime_.clear();
            DBInstanceTime_.putAll(newDBInstanceTime);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> getDBClusterNoTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> DBClusterNoTime_Copy = new ConcurrentHashMap<>();

        synchronized (DBClusterNoTime_) {
            for (Region region : DBClusterNoTime_.keySet()) {
                ArrayList<DBCluster> dbCluster = DBClusterNoTime_.get(region);
                ArrayList dbClusters = new ArrayList<>(dbCluster);
                DBClusterNoTime_Copy.put(region, dbClusters);
            }
        }

        return DBClusterNoTime_Copy;
    }

    public void replaceDBClusterNoTime(ConcurrentHashMap<Region, ArrayList<DBCluster>> newDBClusterNoTime) {
        synchronized (DBClusterNoTime_) {
            DBClusterNoTime_.clear();
            DBClusterNoTime_.putAll(newDBClusterNoTime);
        }
    }

    public ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> getDBClusterTime_Copy() {

        ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<DBCluster>> DBClusterTime_Copy = new ConcurrentHashMap<>();

        synchronized (DBClusterTime_) {
            for (Region region : DBClusterTime_.keySet()) {
                ArrayList<DBCluster> dbCluster = DBClusterTime_.get(region);
                ArrayList dbClusters = new ArrayList<>(dbCluster);
                DBClusterTime_Copy.put(region, dbClusters);
            }
        }

        return DBClusterTime_Copy;
    }

    public void replaceDBClusterTime(ConcurrentHashMap<Region, ArrayList<DBCluster>> newDBClusterTime) {
        synchronized (DBClusterTime_) {
            DBClusterTime_.clear();
            DBClusterTime_.putAll(newDBClusterTime);
        }
    }

    public Boolean prohibitRDSCalls() {
        return prohibitRDSCalls_;
    }

}
