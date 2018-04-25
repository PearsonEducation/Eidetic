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
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
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
 * @author Judah Walker
 */
public class SnapshotCreationTime extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotCreationTime.class.getName());

    private Boolean isFinished_ = false;
    private AwsAccount awsAccount_ = null;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;

    public SnapshotCreationTime(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
        this.uniqueAwsAccountIdentifier_ = awsAccount.getUniqueAwsAccountIdentifier();
        this.maxApiRequestsPerSecond_ = awsAccount.getMaxApiRequestsPerSecond();
        this.numRetries_ = ApplicationConfiguration.getAwsCallRetryAttempts();
    }

    @Override
    public void run() {
        isFinished_ = false;
        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : awsAccount_.getCopyVolumeSnapshots_Copy().entrySet()) {
            try {
            //Get all pending!
            com.amazonaws.regions.Region region = entry.getKey();
            AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

            //Add CreationPendings
            addCreationPending(region, ec2Client);

            //Get all snapshots with creation pending
            updateCreationPending(region, ec2Client);

            //Get all snapshots with creation pending
            addCreationComplete(region, ec2Client);

            ec2Client.shutdown();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in SnapshotCreationTime workflow\", stacktrace=\""
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

    public void addCreationPending(Region region, AmazonEC2Client ec2Client) {
        try {
            Filter[] filters = new Filter[1];
            filters[0] = new Filter().withName("status").withValues("pending");

            DescribeSnapshotsRequest describeSnapshotRequest
                    = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters);
            DescribeSnapshotsResult describeSnapshotsResult
                    = EC2ClientMethods.describeSnapshots(region, 
                            ec2Client, 
                            describeSnapshotRequest, 
                            numRetries_, 
                            maxApiRequestsPerSecond_, 
                            uniqueAwsAccountIdentifier_);

            List<Snapshot> snapshots = describeSnapshotsResult.getSnapshots();
            for (Snapshot snapshot : snapshots) {
                try {
                    Collection<Tag> tags = getResourceTags(snapshot);
                    Tag tag = new Tag("CreationPending", "0");
                    tags.add(tag);
                    setResourceTags(ec2Client, snapshot, tags, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        } catch (Exception e) {
            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in addCreationPending\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
    }

    public void updateCreationPending(Region region, AmazonEC2Client ec2Client) {
        try {
            Filter[] filters2 = new Filter[1];
            filters2[0] = new Filter().withName("tag-key").withValues("CreationPending");

            DescribeSnapshotsRequest describeSnapshotRequest2
                    = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters2);
            DescribeSnapshotsResult describeSnapshotsResult2
                    = EC2ClientMethods.describeSnapshots(region,
                            ec2Client, 
                            describeSnapshotRequest2, 
                            numRetries_, 
                            maxApiRequestsPerSecond_, 
                            uniqueAwsAccountIdentifier_);

            List<Snapshot> snapshots2 = describeSnapshotsResult2.getSnapshots();
            for (Snapshot snapshot : snapshots2) {
                try {
                    Collection<Tag> tags = getResourceTags(snapshot);
                    for (Tag tag : tags) {
                        if ("CreationPending".equals(tag.getKey())) {
                            tags.remove(tag);
                            break;
                        }
                    }
                    String time = Integer.toString(getMinutesBetweenNowAndSnapshot(snapshot));
                    Tag tag = new Tag("CreationPending", time);
                    tags.add(tag);
                    setResourceTags(ec2Client, snapshot, tags, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        } catch (Exception e) {
            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in updateCreationPending\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
    }

    public void addCreationComplete(Region region, AmazonEC2Client ec2Client) {
        try {
            Filter[] filters3 = new Filter[2];
            filters3[0] = new Filter().withName("tag-key").withValues("CreationPending");
            filters3[1] = new Filter().withName("status").withValues("completed");
            DescribeSnapshotsRequest describeSnapshotRequest3
                    = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters3);
            DescribeSnapshotsResult describeSnapshotsResult3
                    = EC2ClientMethods.describeSnapshots(region, 
                            ec2Client, 
                            describeSnapshotRequest3, 
                            numRetries_, 
                            maxApiRequestsPerSecond_, 
                            uniqueAwsAccountIdentifier_);
;

            List<Snapshot> snapshots3 = describeSnapshotsResult3.getSnapshots();
            for (Snapshot snapshot : snapshots3) {
                try {
                    Collection<Tag> tags = getResourceTags(snapshot);
                    Tag deleteTag = null;
                    for (Tag tag : tags) {
                        if ("CreationPending".equals(tag.getKey())) {
                            deleteTag = tag;
                            tags.remove(tag);
                            break;
                        }
                    }
                    if (deleteTag == null) {
                        continue;
                    }
                    DeleteTagsRequest deleteTagsRequest = new DeleteTagsRequest().withResources(snapshot.getSnapshotId()).withTags(deleteTag);
                    EC2ClientMethods.deleteTags(ec2Client, deleteTagsRequest, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    
                    String time = Integer.toString(getMinutesBetweenNowAndSnapshot(snapshot));
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Snapshot_Creation_Time\", Volume_id=\"" + snapshot.getVolumeId() + "\", Snapshot_id=\"" + snapshot.getSnapshotId() +
                            "\", TotalCreationTime=\"" + time.toString() + "\"");
                    Tag tag = new Tag("CreationComplete", time);
                    tags.add(tag);
                    setResourceTags(ec2Client, snapshot, tags, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }

        } catch (Exception e) {
            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in addCreationComplete\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
    }
    
    

}
