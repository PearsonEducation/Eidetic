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
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import com.pearson.eidetic.driver.threads.EideticSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class SnapshotCleaner extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = null;
    private AwsAccount awsAccount_ = null;
    private Integer eideticCleanKeepDays_ = null;
    private Integer allSnapshotCleanKeepDays_ = null;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;

    public SnapshotCleaner(AwsAccount awsAccount, Integer eideticCleanDays, Integer allSnapshotCleanDays,
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

        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : awsAccount_.getVolumeNoTime_Copy().entrySet()) {
            try {
                com.amazonaws.regions.Region region = entry.getKey();
                //kill thread if wrong creds \/ \/ \/ \/
                AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

                Boolean success;

                success = eideticClean(ec2Client);
                if (!success) {
                    continue;
                }

                allSnapshotsClean(ec2Client);

                ec2Client.shutdown();
            } catch (Exception e) {
                logger.error("Event=\"Error\", Error=\"error in SnapshotCleaner workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

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

    public Boolean eideticClean(AmazonEC2Client ec2Client) {
        if (ec2Client == null) {
            return false;
        }
        //See if tag has changed on vol. If 7 days old, delete with all snapshots with old tag.
        Filter[] filters = new Filter[1];
        filters[0] = new Filter().withName("tag-key").withValues("Eidetic");

        DescribeSnapshotsRequest describeSnapshotRequest
                = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters);
        DescribeSnapshotsResult describeSnapshotsResult
                = EC2ClientMethods.describeSnapshots(ec2Client,
                        describeSnapshotRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        List<Snapshot> snapshots = describeSnapshotsResult.getSnapshots();
        for (Snapshot snapshot : snapshots) {
            int timeSinceCreation = getDaysBetweenNowAndSnapshot(snapshot);

            if (timeSinceCreation <= eideticCleanKeepDays_) {
                continue;
            }

            String volId = snapshot.getVolumeId();
            Filter[] volfilters = new Filter[1];
            volfilters[0] = new Filter().withName("volume-id").withValues(volId);
            DescribeVolumesRequest describeVolumesRequest
                    = new DescribeVolumesRequest().withFilters(volfilters);
            DescribeVolumesResult describeVolumeResult
                    = EC2ClientMethods.describeVolumes(ec2Client,
                            describeVolumesRequest,
                            numRetries_,
                            maxApiRequestsPerSecond_,
                            uniqueAwsAccountIdentifier_);

            List<Volume> volumes = describeVolumeResult.getVolumes();

            if (!volumes.isEmpty()) {

                Collection<Tag> voltags;
                try {
                    voltags = getResourceTags(volumes.get(0));
                } catch (Exception e) {
                    logger.info("Event=\"Error\", Error=\"error getting vol in Amnesia\", Volumes_toString=\"" + volumes.toString() + "\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }

                Tag voltag = null;
                for (Tag tag : voltags) {
                    if ("Eidetic".equals(tag.getKey())) {
                        voltag = tag;
                        break;
                    }
                }
                if (voltag == null) {
                    continue;
                }

                Collection<Tag> snapshottags = getResourceTags(snapshot);
                Tag snaptag = null;
                for (Tag tag : snapshottags) {
                    if ("Eidetic".equals(tag.getKey())) {
                        snaptag = tag;
                        break;
                    }
                }
                if (snaptag == null) {
                    continue;
                }

                try {
                    if (snaptag.getValue().equals(voltag.getValue())) {
                        continue;
                    }
                } catch (Exception e) {
                    logger.info("Event=\"Error\", Error=\"error comparing vol and snapshot tag values\", Volume_id=\"" + volumes.get(0).getVolumeId() + ", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }

                try {
                    deleteSnapshot(ec2Client, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }

            } else {
                //Volume doesn't exist
                if (timeSinceCreation > 90) {
                    //See if old vol still exists. If not, if snap is 90 days old, delete.
                    try {
                        deleteSnapshot(ec2Client, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    } catch (Exception e) {
                        logger.error("Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                }
            }
        }
        return true;
    }

    public Boolean allSnapshotsClean(AmazonEC2Client ec2Client) {
        if (ec2Client == null) {
            return false;
        }

        Filter[] filters2 = new Filter[1];
        filters2[0] = new Filter().withName("status").withValues("completed");

        DescribeSnapshotsRequest describeSnapshotRequest2
                = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters2);
        DescribeSnapshotsResult describeSnapshotsResult2
                = EC2ClientMethods.describeSnapshots(ec2Client,
                        describeSnapshotRequest2,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);
        //Delete all snaps allSnapshotCleanDays_ and older.
        List<Snapshot> snapshots2 = describeSnapshotsResult2.getSnapshots();
        for (Snapshot snapshot : snapshots2) {
            Integer timeSinceCreation = getDaysBetweenNowAndSnapshot(snapshot);
            if (timeSinceCreation > allSnapshotCleanKeepDays_) {
                try {
                    deleteSnapshot(ec2Client, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        }

        return true;
    }

}
