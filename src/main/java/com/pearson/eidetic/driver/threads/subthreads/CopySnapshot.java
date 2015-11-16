/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.subthreads;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CopySnapshotRequest;
import com.amazonaws.services.ec2.model.CopySnapshotResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import com.pearson.eidetic.driver.threads.EideticSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class CopySnapshot extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final com.amazonaws.regions.Region region_;
    private final ArrayList<Volume> CopyVolumeSnapshots_;

    public CopySnapshot(String awsAccessKeyId, String awsSecretKey, String uniqueAwsAccountIdentifier,
            Integer maxApiRequestsPerSecond, Integer numRetries, com.amazonaws.regions.Region region,
            ArrayList<Volume> CopyVolumeSnapshots) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
        this.region_ = region;
        this.CopyVolumeSnapshots_ = CopyVolumeSnapshots;
    }

    @Override
    public void run() {
        isFinished_ = false;
        AmazonEC2Client ec2Client = connect(region_, awsAccessKeyId_, awsSecretKey_);

        for (Volume vol : CopyVolumeSnapshots_) {
            try {
                Date date = new java.util.Date();
                JSONParser parser = new JSONParser();

                String inttagvalue = getIntTagValue(vol);
                if (inttagvalue == null) {
                    continue;
                }

                JSONObject eideticParameters = getParameters(vol, inttagvalue, parser);
                if (eideticParameters == null) {
                    continue;
                }

                String period = getPeriod(eideticParameters, vol);
                if (period == null) {
                    continue;
                }

                Integer keep = getKeep(eideticParameters, vol);
                if (keep == null) {
                    continue;
                }

                String date_suffix = establishDate_Suffix(vol, period, date);
                if (date_suffix == null) {
                    continue;
                }

                String volId = vol.getVolumeId();

                String intCopytagvalue = getCopytagvalue(vol, eideticParameters);
                if (intCopytagvalue == null) {
                    continue;
                }

                String gotoendpoint = establishEndpoint(vol, intCopytagvalue);
                if (gotoendpoint == null) {
                    continue;
                }

                Region gotoregion = establishGotoRegion(intCopytagvalue);
                if (gotoregion == null) {
                    continue;
                }

                AmazonEC2Client gotoec2Client = connect(gotoregion, awsAccessKeyId_, awsSecretKey_);

                List<Snapshot> copy_snapshots = getCopySnapshots(vol, gotoec2Client,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
                if (!copy_snapshots.isEmpty()) {

                    List<Snapshot> copycomparelist = populateCopyCompareList(copy_snapshots, period);
                    if (copycomparelist.isEmpty()) {
                        continue;
                    }

                    List<Snapshot> sortedcopyCompareList = new ArrayList<>(copycomparelist);
                    sortSnapshotsByDate(sortedcopyCompareList);

                    Integer hours = getHoursBetweenNowAndNewestSnapshot(sortedcopyCompareList);
                    Integer days = getDaysBetweenNowAndNewestSnapshot(sortedcopyCompareList);

                    //This looks ok to me
                    if (("week".equals(period) && days < 0) || ("week".equals(period) && days >= 7)) {
                    } else if (("day".equals(period) && days < 0) || ("day".equals(period) && days >= 1)) {
                    } else if (("hour".equals(period) && hours < 0) || ("hour".equals(period) && hours >= 1)) {
                    } else if (("month".equals(period) && days < 0) || ("month".equals(period) && days >= 30)) {
                    } else {
                        continue;
                    }

                }
                //End snapshot dec

                List<Snapshot> int_snapshots = getIntSnapshots(ec2Client,
                        vol,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
                //No snapshots to copy over to goto region.
                if (int_snapshots.isEmpty()) {
                    continue;
                }

                List<Snapshot> comparelist = populateCompareList(int_snapshots, period);
                if (comparelist.isEmpty()) {
                    continue;
                }

                List<Snapshot> sortedCompareList = new ArrayList<>(comparelist);
                sortSnapshotsByDate(sortedCompareList);

                //get newest snapshot
                Snapshot snapshot = sortedCompareList.get(sortedCompareList.size() - 1);
                if (snapshot == null) {
                    continue;
                }

                //**************
                //Need to validate that snapshot we are copying doesn't exist already in the copied region 
                //************
                String newSnapshotId = copySnapshotAction(gotoec2Client,
                        vol,
                        snapshot,
                        period,
                        date_suffix,
                        date,
                        intCopytagvalue,
                        region_,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
                
                if (newSnapshotId == null) {
                    continue;
                }

                //Need to get the snapshot object instead of id.
                addTagsToNewCopySnapshot(gotoec2Client,
                        newSnapshotId,
                        vol,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

                //End create new snapshot
                //Start copypot snapshot clean up
                List<Snapshot> del_snapshots = getDeleteSnapshots(gotoec2Client,
                        vol,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

                if (del_snapshots.isEmpty()) {
                    //we should have one. This means something is wrong.
                    continue;
                }

                List<Snapshot> deletelist = populateDeleteList(del_snapshots, period);

                List<Snapshot> sortedDeleteList = new ArrayList<>(deletelist);
                sortSnapshotsByDate(sortedDeleteList);

                deleteSnapshots(gotoec2Client,
                        sortedDeleteList,
                        keep,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

                gotoec2Client.shutdown();
            } catch (Exception e) {
                logger.error("Event=\"Error\", Error=\"error in CopySnapshot workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        ec2Client.shutdown();
        isFinished_ = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished_;
    }

    public AmazonEC2Client connect(Region region, String awsAccessKey, String awsSecretKey) {
        AmazonEC2Client ec2Client;
        String endpoint = "ec2." + region.getName() + ".amazonaws.com";

        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTPS);

        ec2Client = new AmazonEC2Client(credentials, clientConfig);
        ec2Client.setRegion(region);
        ec2Client.setEndpoint(endpoint);
        return ec2Client;
    }

    public String getIntTagValue(Volume vol) {
        if (vol == null) {
            return null;
        }

        String inttagvalue = null;
        for (Tag tag : vol.getTags()) {
            if ("Eidetic".equalsIgnoreCase(tag.getKey())) {
                inttagvalue = tag.getValue();
                break;
            }
        }

        return inttagvalue;

    }

    private JSONObject getParameters(Volume vol, String inttagvalue, JSONParser parser) {
        JSONObject eideticParameters = null;
        try {
            Object obj = parser.parse(inttagvalue);
            eideticParameters = (JSONObject) obj;
        } catch (Exception e) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
        return eideticParameters;
    }

    public String getPeriod(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
            return null;
        }

        String period = null;
        if (createSnapshot.containsKey("Interval")) {
            try {
                period = createSnapshot.get("Interval").toString();
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return period;
    }

    public Integer getKeep(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null) || (vol == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
            return null;
        }

        Integer keep = null;
        if (createSnapshot.containsKey("Retain")) {
            try {
                keep = Integer.parseInt(createSnapshot.get("Retain").toString());
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return keep;
    }

    private String establishEndpoint(Volume vol, String intCopytagvalue) {
        String gotoname;
        String gotoendpoint = null;
        if ("us-east-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("us-west-2".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("us-west-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("eu-west-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("eu-west-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("ap-southeast-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("ap-southeast-2".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("ap-northeast-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else if ("sa-east-1".equals(intCopytagvalue)) {
            gotoname = intCopytagvalue;
            gotoendpoint = "ec2." + gotoname + ".amazonaws.com";
        } else {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
        }
        return gotoendpoint;
    }

    private String establishDate_Suffix(Volume vol, String period, Date date) {
        String date_suffix = null;
        if ("day".equals(period)) {
            date_suffix = date.toString().split(" ")[0];
        } else if ("hour".equals(period)) {
            date_suffix = date.toString().split(" ")[3].split(":")[0];
        } else if ("week".equals(period)) {
            date_suffix = date.toString().split(" ")[2];
        } else if ("month".equals(period)) {
            date_suffix = date.toString().split(" ")[1];
        } else {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
        }
        return date_suffix;
    }

    private String getCopytagvalue(Volume vol, JSONObject eideticParameters) {
        if ((eideticParameters == null)) {
            return null;
        }
        String copySnapshot = null;
        if (eideticParameters.containsKey("CopySnapshot")) {
            copySnapshot = eideticParameters.get("CopySnapshot").toString();
        }
        if (copySnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
            return null;
        }

        return copySnapshot;
    }

    private Region establishGotoRegion(String intCopytagvalue) {
        Region gotoregion = null;
        try {
            List<com.amazonaws.regions.Region> regions = com.amazonaws.regions.RegionUtils.getRegions();
            for (com.amazonaws.regions.Region region : regions) {

                if (Regions.GovCloud.getName().equals(region.getName()) || Regions.CN_NORTH_1.getName().equals(region.getName())) {
                    continue;
                }

                if (region.getName().equals(intCopytagvalue)) {
                    gotoregion = region;
                    break;
                }

            }
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
        return gotoregion;
    }

    private List<Snapshot> populateCopyCompareList(List<Snapshot> copy_snapshots, String period) {
        List<Snapshot> copycomparelist = new ArrayList();
        for (Snapshot snapshot : copy_snapshots) {
            String sndesc = snapshot.getDescription();
            if ("week".equals(period) && sndesc.startsWith("week_snapshot")) {
                copycomparelist.add(snapshot);
            } else if ("day".equals(period) && sndesc.startsWith("day_snapshot")) {
                copycomparelist.add(snapshot);
            } else if ("hour".equals(period) && sndesc.startsWith("hour_snapshot")) {
                copycomparelist.add(snapshot);
            } else if ("month".equals(period) && sndesc.startsWith("month_snapshot")) {
                copycomparelist.add(snapshot);
            }
        }
        return copycomparelist;
    }

    private List<Snapshot> getCopySnapshots(Volume vol, AmazonEC2Client gotoec2Client, Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        String temptag = "tag:Eidetic-CopySnapshot";
        Filter[] copyfilters = new Filter[1];
        copyfilters[0] = new Filter().withName(temptag).withValues(vol.getVolumeId());

        DescribeSnapshotsRequest describecopySnapshotsRequest
                = new DescribeSnapshotsRequest().withFilters(copyfilters);
        DescribeSnapshotsResult describecopySnapshotsResult
                = EC2ClientMethods.describeSnapshots(gotoec2Client,
                        describecopySnapshotsRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        return describecopySnapshotsResult.getSnapshots();
    }

    private List<Snapshot> getIntSnapshots(AmazonEC2Client ec2Client, Volume vol, Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        Filter[] filters = new Filter[2];
        filters[0] = new Filter().withName("volume-id").withValues(vol.getVolumeId());
        filters[1] = new Filter().withName("status").withValues("completed");

        DescribeSnapshotsRequest describeSnapshotsRequest
                = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters);
        DescribeSnapshotsResult describeSnapshotsResult
                = EC2ClientMethods.describeSnapshots(ec2Client,
                        describeSnapshotsRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
        return describeSnapshotsResult.getSnapshots();
    }

    private List<Snapshot> populateCompareList(List<Snapshot> int_snapshots, String period) {
        List<Snapshot> comparelist = new ArrayList();
        for (Snapshot snapshot : int_snapshots) {
            String sndesc = snapshot.getDescription();
            if ("week".equals(period) && sndesc.startsWith("week_snapshot")) {
                comparelist.add(snapshot);
            } else if ("day".equals(period) && sndesc.startsWith("day_snapshot")) {
                comparelist.add(snapshot);
            } else if ("hour".equals(period) && sndesc.startsWith("hour_snapshot")) {
                comparelist.add(snapshot);
            } else if ("month".equals(period) && sndesc.startsWith("month_snapshot")) {
                comparelist.add(snapshot);
            }
        }
        return comparelist;
    }

    private String copySnapshotAction(AmazonEC2Client gotoec2Client, Volume vol, Snapshot snapshot,
            String period, String date_suffix, Date date, String intCopytagvalue, Region region_,
            Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        String volumeAttachmentInstance = "none";
        try {
                volumeAttachmentInstance = vol.getAttachments().get(0).getInstanceId();
        } catch (Exception e) {
                logger.debug("Volume not attached to instance: " + vol.getVolumeId());
        }
        
        String description = period + "_snapshot " + vol.getVolumeId() + " by Eidetic CopySnapshot at " + date.toString()
                + ". Volume attached to " + volumeAttachmentInstance;
        CopySnapshotRequest copySnapshotRequest = new CopySnapshotRequest();
        copySnapshotRequest.setSourceRegion(region_.toString());
        copySnapshotRequest.setSourceSnapshotId(snapshot.getSnapshotId());
        copySnapshotRequest.setDescription(description);
        copySnapshotRequest.setDestinationRegion(intCopytagvalue);
        if (snapshot.isEncrypted()) {
            copySnapshotRequest.setEncrypted(snapshot.isEncrypted());
        //  copySnapshotRequest.setPresignedUrl(copySnapshotRequest.getPresignedUrl());
        }
        CopySnapshotResult copySnapshotResult = EC2ClientMethods.copySnapshot(gotoec2Client,
                copySnapshotRequest,
                numRetries_,
                maxApiRequestsPerSecond_,
                uniqueAwsAccountIdentifier_);
        
        if (copySnapshotResult == null) {
            return null;
        }
        
        return copySnapshotResult.getSnapshotId();
    }

    private void addTagsToNewCopySnapshot(AmazonEC2Client gotoec2Client, String newSnapshotId, Volume vol, Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        Filter[] newfilters = new Filter[1];
        newfilters[0] = new Filter().withName("snapshot-id").withValues(newSnapshotId);

        DescribeSnapshotsRequest describeNewSnapshotsRequest
                = new DescribeSnapshotsRequest().withFilters(newfilters);
        DescribeSnapshotsResult describeNewSnapshotsResult
                = EC2ClientMethods.describeSnapshots(gotoec2Client,
                        describeNewSnapshotsRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        List<Snapshot> newsnapshots = describeNewSnapshotsResult.getSnapshots();

        Collection<Tag> tags_volume = getResourceTags(vol);

        for (Tag tag : tags_volume) {
            if (tag.getKey().equalsIgnoreCase("Eidetic")) {
                tags_volume.remove(tag);
                break;
            }
        }

        tags_volume.add(new Tag("Eidetic-CopySnapshot", vol.getVolumeId()));

        try {
            setResourceTags(gotoec2Client, newsnapshots.get(0), tags_volume,
                    numRetries_,
                    maxApiRequestsPerSecond_,
                    uniqueAwsAccountIdentifier_);
        } catch (Exception e) {
            logger.info("Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + newSnapshotId + "\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
    }

    private List<Snapshot> getDeleteSnapshots(AmazonEC2Client gotoec2Client, Volume vol, Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        String temptag = "tag:Eidetic-CopySnapshot";
        Filter[] copyfilters = new Filter[1];
        copyfilters[0] = new Filter().withName(temptag).withValues(vol.getVolumeId());

        DescribeSnapshotsRequest describecopySnapshotsRequest = new DescribeSnapshotsRequest().withFilters(copyfilters);
        DescribeSnapshotsResult describecopySnapshotsResult = EC2ClientMethods.describeSnapshots(gotoec2Client,
                describecopySnapshotsRequest,
                numRetries_,
                maxApiRequestsPerSecond_,
                uniqueAwsAccountIdentifier_);

        return describecopySnapshotsResult.getSnapshots();
    }

    private List<Snapshot> populateDeleteList(List<Snapshot> del_snapshots, String period) {
        List<Snapshot> deletelist = new ArrayList();

        for (Snapshot dsnapshot : del_snapshots) {
            String sndesc = dsnapshot.getDescription();
            if ("week".equals(period) && sndesc.startsWith("week_snapshot")) {
                deletelist.add(dsnapshot);
            } else if ("day".equals(period) && sndesc.startsWith("day_snapshot")) {
                deletelist.add(dsnapshot);
            } else if ("hour".equals(period) && sndesc.startsWith("hour_snapshot")) {
                deletelist.add(dsnapshot);
            } else if ("month".equals(period) && sndesc.startsWith("month_snapshot")) {
                deletelist.add(dsnapshot);
            }
        }

        return deletelist;
    }

    private void deleteSnapshots(AmazonEC2Client gotoec2Client, List<Snapshot> sortedDeleteList, Integer keep, Integer numRetries_, Integer maxApiRequestsPerSecond_, String uniqueAwsAccountIdentifier_) {
        int delta = sortedDeleteList.size() - keep;

        for (int i : range(0, delta - 1)) {
            try {
                deleteSnapshot(gotoec2Client, sortedDeleteList.get(i), numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + sortedDeleteList.get(i).getSnapshotId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
    }
}
