/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DeleteDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.DeleteDBSnapshotRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.utilities.StackTrace;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
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
public class RDSSubThreadMethods implements RDSSubThread {

    private static final Logger logger = LoggerFactory.getLogger(RDSSubThreadMethods.class.getName());

    @Override
    public boolean isFinished() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<DBSnapshot> getAllDBSnapshotsOfDBInstance(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (rdsClient == null || dbInstance == null) {
            return new ArrayList<>();
        }

        String instanceID = dbInstance.getDBInstanceIdentifier();

        List<DBSnapshot> snapshots = null;
        try {

            DescribeDBSnapshotsRequest describeDBSnapshotsRequest
                    = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(instanceID);
            DescribeDBSnapshotsResult describeSnapshotsResult
                    = RDSClientMethods.describeDBSnapshots(region,
                            rdsClient,
                            describeDBSnapshotsRequest,
                            numRetries,
                            maxApiRequestsPerSecond,
                            uniqueAwsAccountIdentifier);

            snapshots = describeSnapshotsResult.getDBSnapshots();

            String marker = describeSnapshotsResult.getMarker();
            while (marker != null) {
                describeDBSnapshotsRequest.setMarker(marker);

                describeSnapshotsResult
                        = RDSClientMethods.describeDBSnapshots(region,
                                rdsClient,
                                describeDBSnapshotsRequest,
                                numRetries,
                                maxApiRequestsPerSecond,
                                uniqueAwsAccountIdentifier);

                snapshots.addAll(describeSnapshotsResult.getDBSnapshots());

                marker = describeSnapshotsResult.getMarker();
            }

            List<DBSnapshot> toRemove = new ArrayList();

            for (DBSnapshot dbSnapshot : snapshots) {
                if (!dbSnapshot.getStatus().equalsIgnoreCase("available")) {
                    toRemove.add(dbSnapshot);
                }
            }

            snapshots.removeAll(toRemove);
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + dbInstance.getDBInstanceIdentifier() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshots;
    }

    @Override
    public List<DBClusterSnapshot> getAllDBClusterSnapshotsOfDBCluster(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (rdsClient == null || dbCluster == null) {
            return new ArrayList<>();
        }

        String dbClusterID = dbCluster.getDBClusterIdentifier();

        List<DBClusterSnapshot> snapshots = null;
        try {

            DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest
                    = new DescribeDBClusterSnapshotsRequest().withDBClusterIdentifier(dbClusterID);
            DescribeDBClusterSnapshotsResult describeClusterSnapshotsResult
                    = RDSClientMethods.describeDBClusterSnapshots(region,
                            rdsClient,
                            describeDBClusterSnapshotsRequest,
                            numRetries,
                            maxApiRequestsPerSecond,
                            uniqueAwsAccountIdentifier);

            snapshots = describeClusterSnapshotsResult.getDBClusterSnapshots();

            String marker = describeClusterSnapshotsResult.getMarker();
            while (marker != null) {
                describeDBClusterSnapshotsRequest.setMarker(marker);

                describeClusterSnapshotsResult
                        = RDSClientMethods.describeDBClusterSnapshots(region,
                                rdsClient,
                                describeDBClusterSnapshotsRequest,
                                numRetries,
                                maxApiRequestsPerSecond,
                                uniqueAwsAccountIdentifier);

                snapshots.addAll(describeClusterSnapshotsResult.getDBClusterSnapshots());

                marker = describeClusterSnapshotsResult.getMarker();
            }

            List<DBClusterSnapshot> toRemove = new ArrayList();

            for (DBClusterSnapshot dbClusterSnapshot : snapshots) {
                if (!dbClusterSnapshot.getStatus().equalsIgnoreCase("available")) {
                    toRemove.add(dbClusterSnapshot);
                }
            }

            snapshots.removeAll(toRemove);
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + dbCluster.getDBClusterIdentifier() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshots;
    }

    @Override
    public Collection<Tag> getResourceTags(Region region, AmazonRDSClient rdsClient, String arn, Integer numRetries,
            Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        return RDSClientMethods.getTags(region,
                rdsClient,
                listTagsForResourceRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier).getTagList();
    }

    @Override
    public void setResourceTags(Region region, AmazonRDSClient rdsClient, String arn, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        AddTagsToResourceRequest addTagsToResourceRequest = new AddTagsToResourceRequest().withResourceName(arn);
        HashSet<String> tempset = new HashSet<>();
        HashSet<Tag> temptagset = new HashSet<>();
        for (Tag tag : tags) {
            if (!tempset.contains(tag.getKey())) {
                tempset.add(tag.getKey());
                temptagset.add(tag);
            }
        }
        tags.clear();
        tags.addAll(temptagset);
        addTagsToResourceRequest.setTags(tags);
        RDSClientMethods.createTags(region,
                rdsClient,
                addTagsToResourceRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
    }

    public int dateCompare(DBSnapshot snap1, DBSnapshot snap2) {
        if (snap1.getSnapshotCreateTime().before(snap2.getSnapshotCreateTime())) {
            return -1;
        } else if (snap1.getSnapshotCreateTime().equals(snap2.getSnapshotCreateTime())) {
            return 0;
        }
        return 1;
    }

    public int dateCompare(DBClusterSnapshot snap1, DBClusterSnapshot snap2) {
        if (snap1.getSnapshotCreateTime().before(snap2.getSnapshotCreateTime())) {
            return -1;
        } else if (snap1.getSnapshotCreateTime().equals(snap2.getSnapshotCreateTime())) {
            return 0;
        }
        return 1;
    }

    @Override
    public void sortDBSnapshotsByDate(List<DBSnapshot> comparelist) {

        if (comparelist == null) {
            return;
        }

        /**
         * Here we are looking at if there are more than one current record, we
         * convert to datetime and compare with now. Given the period, we decide
         * to take a snapshot or move to new object. Also, comparelist[0] is
         * oldest, comparelist[len(comparelist) - 1] is newest.
         */
        if (comparelist.size() > 1) {
            Collections.sort(comparelist, new Comparator<DBSnapshot>() {
                @Override
                public int compare(DBSnapshot s1, DBSnapshot s2) {
                    return s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime());
                }
            });
        }
    }

    @Override
    public void sortDBClusterSnapshotsByDate(List<DBClusterSnapshot> comparelist) {

        if (comparelist == null) {
            return;
        }

        /**
         * Here we are looking at if there are more than one current record, we
         * convert to datetime and compare with now. Given the period, we decide
         * to take a snapshot or move to new object. Also, comparelist[0] is
         * oldest, comparelist[len(comparelist) - 1] is newest.
         */
        if (comparelist.size() > 1) {
            Collections.sort(comparelist, new Comparator<DBClusterSnapshot>() {
                @Override
                public int compare(DBClusterSnapshot s1, DBClusterSnapshot s2) {
                    return s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime());
                }
            });
        }
    }

    @Override
    public int getHoursBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    @Override
    public int getHoursBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    @Override
    public int getDaysBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }

    @Override
    public int getDaysBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }

    @Override
    public DBSnapshot createDBSnapshotOfDBInstance(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        String dbInstanceId = dbInstance.getDBInstanceIdentifier();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        CreateDBSnapshotRequest createDBSnapshotRequest = new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceId).withDBSnapshotIdentifier("Eidetic-" + dbInstanceId + "-" + dateFormat.format(date));

        DBSnapshot snapshot = null;
        try {
            snapshot = RDSClientMethods.createDBSnapshot(region,
                    rdsClient,
                    createDBSnapshotRequest,
                    numRetries,
                    maxApiRequestsPerSecond,
                    uniqueAwsAccountIdentifier);
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshot;
    }

    @Override
    public DBClusterSnapshot createDBClusterSnapshotOfDBCluster(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier
    ) {
        String dbClusterId = dbCluster.getDBClusterIdentifier();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        CreateDBClusterSnapshotRequest createDBClusterSnapshotRequest = new CreateDBClusterSnapshotRequest().withDBClusterIdentifier(dbClusterId).withDBClusterSnapshotIdentifier("Eidetic-" + dbClusterId + "-" + dateFormat.format(date));
        DBClusterSnapshot snapshot = null;
        try {
            snapshot = RDSClientMethods.createDBClusterSnapshot(region,
                    rdsClient,
                    createDBClusterSnapshotRequest,
                    numRetries,
                    maxApiRequestsPerSecond,
                    uniqueAwsAccountIdentifier);
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier + "\"," + createDBClusterSnapshotRequest.toString() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }

        return snapshot;
    }

    public void deleteDBSnapshot(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, DBSnapshot dbSnapshot,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        String dbSnapshotId = dbSnapshot.getDBSnapshotIdentifier();
        DeleteDBSnapshotRequest deleteDBSnapshotRequest
                = new DeleteDBSnapshotRequest().withDBSnapshotIdentifier(dbSnapshotId);
        RDSClientMethods.deleteDBSnapshot(region,
                rdsClient,
                dbInstance,
                deleteDBSnapshotRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
    }

    public void deleteDBClusterSnapshot(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster, DBClusterSnapshot dbClusterSnapshot,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        String dbClusterSnapshotId = dbClusterSnapshot.getDBClusterSnapshotIdentifier();
        DeleteDBClusterSnapshotRequest deleteDBClusterSnapshotRequest
                = new DeleteDBClusterSnapshotRequest().withDBClusterSnapshotIdentifier(dbClusterSnapshotId);
        RDSClientMethods.deleteDBClusterSnapshot(region,
                rdsClient,
                dbCluster,
                deleteDBClusterSnapshotRequest,
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

    public int getMinutesBetweenNowAndDBSnapshot(DBSnapshot dbSnapshot) {
        if (dbSnapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(dbSnapshot.getSnapshotCreateTime());
        return Minutes.minutesBetween(dt, now).getMinutes();
    }

    public int getDaysBetweenNowAndDBSnapshot(DBSnapshot dbSnapshot) {
        if (dbSnapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(dbSnapshot.getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }

    public int getMinutesBetweenNowAndDBClusterSnapshot(DBClusterSnapshot dbClusterSnapshot) {
        if (dbClusterSnapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(dbClusterSnapshot.getSnapshotCreateTime());
        return Minutes.minutesBetween(dt, now).getMinutes();
    }

    public int getDaysBetweenNowAndDBClusterSnapshot(DBClusterSnapshot dbClusterSnapshot) {
        if (dbClusterSnapshot == null) {
            throw new NullPointerException("Parameter Snapshot cannot be null");
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(dbClusterSnapshot.getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }

}
