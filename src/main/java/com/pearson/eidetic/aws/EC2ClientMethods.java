/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CopySnapshotRequest;
import com.amazonaws.services.ec2.model.CopySnapshotResult;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.util.AwsHostNameUtils;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.globals.GlobalVariables;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class EC2ClientMethods {

    private static final Logger logger = LoggerFactory.getLogger(EC2ClientMethods.class.getName());

    public static CreateSnapshotResult createSnapshot(Region region, AmazonEC2Client ec2Client, CreateSnapshotRequest snapshotRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        CreateSnapshotResult createSnapshotResult = null;
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                    logger.debug(snapshotRequest.toString() + System.lineSeparator() + "**********I am sleeping because something is blocking on AWS****************");

                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                    logger.debug(snapshotRequest.toString() + System.lineSeparator() + "****** I am waiting because the request rate is being throttled by Eidetic");
                }
                Long elapsedTime;
                Long startTime = System.currentTimeMillis();

                createSnapshotResult = ec2Client.createSnapshot(snapshotRequest);

                elapsedTime = (new Date()).getTime() - startTime;

                if (createSnapshotResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    logger.debug("************** I finished this snapshot and it took " + elapsedTime.toString() + " ms *******************");
                    break;
                }
            } catch (Exception e) {
                if (e.toString().contains("retired")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + snapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    return createSnapshotResult;
                }
                if (e.toString().contains("RequestLimitExceeded")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",createSnapshot()," + snapshotRequest.toString() + e.toString());
                    continue;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + snapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",Event=\"Snapshot Created\", Volume_id=\"" + snapshotRequest.getVolumeId() + "\"");
        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    if (createSnapshotResult != null) {
                        DescribeSnapshotsRequest describeSnapshotsRequest_SnapshotID = new DescribeSnapshotsRequest().withSnapshotIds(createSnapshotResult.getSnapshot().getSnapshotId());
                        awsAccount.removeDescribeSnapshotsCache(region, describeSnapshotsRequest_SnapshotID);
                    }
                    //Delete Volume ID from Snapshot Cache
                    Filter filter = new Filter().withName("volume-id").withValues(snapshotRequest.getVolumeId());
                    DescribeSnapshotsRequest describeSnapshotsRequest_VolumeID = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filter);
                    awsAccount.removeDescribeSnapshotsCache(region, describeSnapshotsRequest_VolumeID);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
        return createSnapshotResult;

    }

    public static CopySnapshotResult copySnapshot(AmazonEC2Client ec2Client, CopySnapshotRequest snapshotRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        CopySnapshotResult copySnapshotResult = null;

        if (numRetries >= 3) {
            numRetries = 3;
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                copySnapshotResult = ec2Client.copySnapshot(snapshotRequest);

                if (copySnapshotResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                if (e.toString().contains("20109") || e.toString().contains("Too many snapshot") || e.toString().contains("retired")) {
                    return copySnapshotResult;
                }
                if (e.toString().contains("RequestLimitExceeded")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",copySnapshot()," + snapshotRequest.toString() + e.toString());
                    continue;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + snapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",Event=\"Snapshot Copied\", source_region=\"" + snapshotRequest.getSourceRegion()
                + "\", destination_region=\"" + snapshotRequest.getDestinationRegion() + "\", source_snapshot_id=\"" + snapshotRequest.getSourceSnapshotId() + "\"");
        return copySnapshotResult;

    }

    public static void createTags(AmazonEC2Client ec2Client, CreateTagsRequest createTagsRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    ec2Client.createTags(createTagsRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("TagLimitExceeded")) {
                        break;
                    }
                    if (e.toString().contains("RequestLimitExceeded")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",createTags()," + createTagsRequest.toString() + e.toString());
                        continue;
                    }
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

    }

    public static void deleteTags(AmazonEC2Client ec2Client, DeleteTagsRequest deleteTagsRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    ec2Client.deleteTags(deleteTagsRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("RequestLimitExceeded")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",deleteTags()," + deleteTagsRequest.toString() + e.toString());
                        continue;
                    }
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deleteTagsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

    }

    public static void deleteSnapshot(Region region, AmazonEC2Client ec2Client, Volume vol, DeleteSnapshotRequest deletesnapshotRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                try {
                    ec2Client.deleteSnapshot(deletesnapshotRequest);
                    try {
                        GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deletesnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    }
                    break;
                } catch (Exception e) {
                    if (e.toString().contains("InvalidSnapshot.InUse")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deletesnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                        return;
                    }
                    if (e.toString().contains("RequestLimitExceeded")) {
                        logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",deleteSnapshot()," + deletesnapshotRequest.toString() + e.toString());
                        continue;
                    }
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deletesnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + deletesnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }
        logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",Event=\"Snapshot Deleted\", Snapshot_id=\"" + deletesnapshotRequest.getSnapshotId() + "\"");

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    //Delete Snapshot ID from Snapshot Cache
                    DescribeSnapshotsRequest describeSnapshotsRequest_SnapshotID = new DescribeSnapshotsRequest().withSnapshotIds(deletesnapshotRequest.getSnapshotId());
                    awsAccount.removeDescribeSnapshotsCache(region, describeSnapshotsRequest_SnapshotID);
                    //Delete Volume ID from Snapshot Cache
                    if (vol != null) {
                        Filter filter = new Filter().withName("volume-id").withValues(vol.getVolumeId());
                        DescribeSnapshotsRequest describeSnapshotsRequest_VolumeID = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filter);
                        awsAccount.removeDescribeSnapshotsCache(region, describeSnapshotsRequest_VolumeID);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem clearing caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
    }

    public static DescribeVolumesResult describeVolumes(Region region, AmazonEC2Client ec2Client, DescribeVolumesRequest describeVolumesRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeVolumesResult describeVolumesResult = null;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeVolumesResult = awsAccount.getDescribeVolumesCache(region, describeVolumesRequest);
                    if (describeVolumesResult != null) {
                        return describeVolumesResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);

                if (describeVolumesResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                if (e.toString().contains("RequestLimitExceeded")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeVolumes()," + describeVolumesRequest.toString() + e.toString());
                    continue;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeVolumesRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        try {
            if (describeVolumesResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeVolumesCache(region, describeVolumesRequest, describeVolumesResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeVolumesResult;

    }

    public static DescribeSnapshotsResult describeSnapshots(Region region, AmazonEC2Client ec2Client, DescribeSnapshotsRequest describeSnapshotsRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeSnapshotsResult describeSnapshotsResult = null;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeSnapshotsResult = awsAccount.getDescribeSnapshotsCache(region, describeSnapshotsRequest);
                    if (describeSnapshotsResult != null) {
                        return describeSnapshotsResult;                      
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);
                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeSnapshotsResult = ec2Client.describeSnapshots(describeSnapshotsRequest);

                if (describeSnapshotsResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                if (e.toString().contains("RequestLimitExceeded")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeSnapshots()," + describeSnapshotsRequest.toString() + e.toString());
                    continue;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeSnapshotsRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        try {
            if (describeSnapshotsResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeSnapshotsCache(region, describeSnapshotsRequest, describeSnapshotsResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeSnapshotsResult;

    }

    public static DescribeInstancesResult describeInstances(Region region, AmazonEC2Client ec2Client, DescribeInstancesRequest describeInstancesRequest,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        DescribeInstancesResult describeInstancesResult = null;

        try {
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                    describeInstancesResult = awsAccount.getDescribeInstancesCache(region, describeInstancesRequest);
                    if (describeInstancesResult != null) {
                        return describeInstancesResult;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        for (int i = 0; i <= numRetries; i++) {
            try {
                // if the initial download attempt failed, wait for i * 2000 + random(1,10) * 100ms 
                if (i > 0) {
                    long sleepTimeInMilliseconds = 2000 * i + (ThreadLocalRandom.current().nextInt(1, 10) * 100);
                    Threads.sleepMilliseconds(sleepTimeInMilliseconds);
                }

                AtomicLong requestAttemptCounter = GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(uniqueAwsAccountIdentifier);

                long currentRequestCount = requestAttemptCounter.incrementAndGet();

                while (currentRequestCount > maxApiRequestsPerSecond) {
                    Threads.sleepMilliseconds(50);
                    currentRequestCount = requestAttemptCounter.incrementAndGet();
                }

                describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);

                if (describeInstancesResult != null) {
                    GlobalVariables.apiRequestCountersByAwsAccount.get(uniqueAwsAccountIdentifier).incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                if (e.toString().contains("RequestLimitExceeded")) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",describeInstances()," + describeInstancesRequest.toString() + e.toString());
                    continue;
                }
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + describeInstancesRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            }
        }

        try {
            if (describeInstancesResult != null) {
                for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                    if (awsAccount.getUniqueAwsAccountIdentifier().equals(uniqueAwsAccountIdentifier) && awsAccount.enableCaching()) {
                        awsAccount.putDescribeInstancesCache(region, describeInstancesRequest, describeInstancesResult);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\",error=\"problem with caches\"," + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return describeInstancesResult;

    }

}
