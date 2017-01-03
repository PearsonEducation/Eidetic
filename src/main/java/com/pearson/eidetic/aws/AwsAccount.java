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

import com.pearson.eidetic.globals.ApplicationConfiguration;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pearson.eidetic.utilities.StackTrace;
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

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeNoTime_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeTime_ = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSync_ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> VolumeSyncValidate_ = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<com.amazonaws.regions.Region, ArrayList<Volume>> CopyVolumeSnapshots_ = new ConcurrentHashMap<>();

    public AwsAccount(int index, String awsNickname,
            String awsAccessKeyId, String awsSecretKey, Integer maxApiRequestsPerSecond) {

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
                VolumeSync_ .put(region, new ArrayList<Volume>());       

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

    public void initializeSnapshots() {
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

}
