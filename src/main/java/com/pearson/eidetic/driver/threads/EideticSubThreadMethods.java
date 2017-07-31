/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateSnapshotResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class EideticSubThreadMethods implements EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    @Override
    public boolean isFinished() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Snapshot> getAllSnapshotsOfVolume(AmazonEC2Client ec2Client, Volume vol,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (ec2Client == null || vol == null || numRetries == null || maxApiRequestsPerSecond == null || uniqueAwsAccountIdentifier == null) {
            return new ArrayList<>();
        }

        String volumeID = vol.getVolumeId();

        Filter filter = new Filter().withName("volume-id").withValues(volumeID);

        List<Snapshot> snapshots = null;
        try {

            DescribeSnapshotsRequest describeSnapshotsRequest
                    = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filter);
            DescribeSnapshotsResult describeSnapshotsResult
                    = EC2ClientMethods.describeSnapshots(ec2Client,
                            describeSnapshotsRequest,
                            numRetries,
                            maxApiRequestsPerSecond,
                            uniqueAwsAccountIdentifier);

            snapshots = describeSnapshotsResult.getSnapshots();
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + vol.getVolumeId() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshots;
    }

    @Override
    public Collection<Tag> getResourceTags(Volume vol) {
        List<Tag> tagsList = vol.getTags();
        Collection<Tag> tags = tagsList;
        return tags;
    }

    @Override
    public Collection<Tag> getResourceTags(Instance instance) {
        List<Tag> tagsList = instance.getTags();
        Collection<Tag> tags = tagsList;
        return tags;
    }

    @Override
    public Collection<Tag> getResourceTags(Snapshot snapshot) {
        List<Tag> tagsList = snapshot.getTags();
        Collection<Tag> tags = tagsList;
        return tags;
    }

    @Override
    public void setResourceTags(AmazonEC2Client ec2Client, Volume vol, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(vol.getVolumeId()).withTags(tags);
        EC2ClientMethods.createTags(ec2Client,
                createTagsRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
        vol.setTags(tags);
    }

    @Override
    public void setResourceTags(AmazonEC2Client ec2Client, Instance instance, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(instance.getInstanceId()).withTags(tags);
        EC2ClientMethods.createTags(ec2Client,
                createTagsRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
        instance.setTags(tags);
    }

    @Override
    public void setResourceTags(AmazonEC2Client ec2Client, Snapshot snapshot, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(snapshot.getSnapshotId()).withTags(tags);
        EC2ClientMethods.createTags(ec2Client,
                createTagsRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
        snapshot.setTags(tags);
    }

    public int dateCompare(Snapshot snap1, Snapshot snap2) {
        if (snap1.getStartTime().before(snap2.getStartTime())) {
            return -1;
        } else if (snap1.getStartTime().equals(snap2.getStartTime())) {
            return 0;
        }
        return 1;
    }

    @Override
    public void sortSnapshotsByDate(List<Snapshot> comparelist) {

        if (comparelist == null) {
            return;
        }

        /**
         * Here we are looking at if there are more than one current record, we
         * convert to datetime and compare with now. Given the period, we decide
         * to take a snapshot or move to new volume. Also, comparelist[0] is
         * oldest, comparelist[len(comparelist) - 1] is newest.
         */
        if (comparelist.size() > 1) {
            Collections.sort(comparelist, new Comparator<Snapshot>() {
                @Override
                public int compare(Snapshot s1, Snapshot s2) {
                    return s1.getStartTime().compareTo(s2.getStartTime());
                }
            });
        }
    }

    @Override
    public int getHoursBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getStartTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    @Override
    public int getDaysBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getStartTime());
        return Days.daysBetween(dt, now).getDays();
    }

    @Override
    public Snapshot createSnapshotOfVolume(AmazonEC2Client ec2Client, Volume vol, String description,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        String volumeId = vol.getVolumeId();
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest(
                volumeId, description);
        Snapshot snapshot = null;
        try {
            CreateSnapshotResult result = EC2ClientMethods.createSnapshot(ec2Client,
                    snapshotRequest,
                    numRetries,
                    maxApiRequestsPerSecond,
                    uniqueAwsAccountIdentifier);
            snapshot = result.getSnapshot();
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + snapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshot;
    }

    @Override
    public Snapshot createSnapshotOfVolume(AmazonEC2Client ec2Client, Volume vol,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier
    ) {
        String volumeId = vol.getVolumeId();
        String description = "";
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest(
                volumeId, description);

        Snapshot snapshot = null;
        try {
            CreateSnapshotResult result = EC2ClientMethods.createSnapshot(ec2Client,
                    snapshotRequest,
                    numRetries,
                    maxApiRequestsPerSecond,
                    uniqueAwsAccountIdentifier);
            snapshot = result.getSnapshot();
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + snapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshot;
    }

    public void deleteSnapshot(AmazonEC2Client ec2Client, Snapshot snapshot,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        String snapshotId = snapshot.getSnapshotId();
        DeleteSnapshotRequest deleteSnapshotRequest
                = new DeleteSnapshotRequest().withSnapshotId(snapshotId);
        EC2ClientMethods.deleteSnapshot(ec2Client,
                deleteSnapshotRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
    }

    public static int[] range(Integer start, Integer length) {
        if (start == null || length == null) {
            throw new NullPointerException("Parameter start/length cannot be null");
        }
        
        if (length < 0) {
            int[] range = {};
            return range;
        }
        int[] range = new int[length - start + 1];
        for (int i = start; i <= length; i++) {
            range[i - start] = i;
        }
        return range;
    }

    public int getMinutesBetweenNowAndSnapshot(Snapshot snapshot) {
        if (snapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }
        
        DateTime now = new DateTime();
        DateTime dt = new DateTime(snapshot.getStartTime());
        return Minutes.minutesBetween(dt, now).getMinutes();
    }

    public int getDaysBetweenNowAndSnapshot(Snapshot snapshot) {
        if (snapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }
        
        DateTime now = new DateTime();
        DateTime dt = new DateTime(snapshot.getStartTime());
        return Days.daysBetween(dt, now).getDays();
    }

}
