/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.google.common.collect.Lists;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.driver.threads.rds.RDSSubThread;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;

/**
 *
 * @author Judah Walker
 */
public class MonitorRDSMethods implements MonitorRDS {

    @Override
    public boolean areAllDBInstanceThreadsDead(List<? extends RDSSubThread> threads) {

        if ((threads == null) || threads.isEmpty()) {
            return true;
        }

        boolean areAllThreadsDead = true;

        for (RDSSubThread thread : threads) {
            if (!thread.isFinished()) {
                areAllThreadsDead = false;
            }
        }

        return areAllThreadsDead;
    }

    @Override
    public boolean areAllDBClusterThreadsDead(List<? extends RDSSubThread> threads) {

        if ((threads == null) || threads.isEmpty()) {
            return true;
        }

        boolean areAllThreadsDead = true;

        for (RDSSubThread thread : threads) {
            if (!thread.isFinished()) {
                areAllThreadsDead = false;
            }
        }

        return areAllThreadsDead;
    }

    @Override
    public boolean areAllThreadsDead(List<? extends RDSSubThread> threads) {

        if ((threads == null) || threads.isEmpty()) {
            return true;
        }

        boolean areAllThreadsDead = true;

        for (RDSSubThread thread : threads) {
            if (!thread.isFinished()) {
                areAllThreadsDead = false;
            }
        }

        return areAllThreadsDead;
    }

    public ArrayList<ArrayList<DBInstance>> splitDBInstanceArrayList(ArrayList<DBInstance> dbInstances, Integer splitFactor) {
        if ((dbInstances == null) | (splitFactor == null)) {
            return null;
        }

        ArrayList<ArrayList<DBInstance>> returnDBInstances = new ArrayList<>();
        if (splitFactor <= 1) {
            returnDBInstances.add(dbInstances);
            return returnDBInstances;
        }

        if (dbInstances.size() <= splitFactor) {
            returnDBInstances.add(dbInstances);
            return returnDBInstances;
        }

        final int chunkLength = dbInstances.size() / splitFactor;
        final int totalLength = dbInstances.size();

        for (int i = 0; i < totalLength; i += chunkLength) {
            returnDBInstances.add(
                    new ArrayList<>(dbInstances.subList(i,
                            Math.min(totalLength, i + chunkLength)))
            );
        }

        return returnDBInstances;
    }

    public ArrayList<ArrayList<DBCluster>> splitDBClusterArrayList(ArrayList<DBCluster> dbClusters, Integer splitFactor) {
        if ((dbClusters == null) | (splitFactor == null)) {
            return null;
        }

        ArrayList<ArrayList<DBCluster>> returnDBClusters = new ArrayList<>();
        if (splitFactor <= 1) {
            returnDBClusters.add(dbClusters);
            return returnDBClusters;
        }

        if (dbClusters.size() <= splitFactor) {
            returnDBClusters.add(dbClusters);
            return returnDBClusters;
        }

        final int chunkLength = dbClusters.size() / splitFactor;
        final int totalLength = dbClusters.size();

        for (int i = 0; i < totalLength; i += chunkLength) {
            returnDBClusters.add(
                    new ArrayList<>(dbClusters.subList(i,
                            Math.min(totalLength, i + chunkLength)))
            );
        }

        return returnDBClusters;
    }

    public static ArrayList<ArrayList<DBInstance>> listsOfDBInstancesToArrayLists(List<List<DBInstance>> input) {
        ArrayList<ArrayList<DBInstance>> returnList = new ArrayList();
        for (List<DBInstance> list : input) {
            ArrayList<DBInstance> newOne = new ArrayList();
            newOne.addAll(list);
            returnList.add(newOne);
        }
        return returnList;
    }

    public static ArrayList<ArrayList<DBCluster>> listsOfDBClustersToArrayLists(List<List<DBCluster>> input) {
        ArrayList<ArrayList<DBCluster>> returnList = new ArrayList();
        for (List<DBCluster> list : input) {
            ArrayList<DBCluster> newOne = new ArrayList();
            newOne.addAll(list);
            returnList.add(newOne);
        }
        return returnList;
    }

    public List<DBSnapshot> getAllDBSnapshotsOfDBInstance(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (rdsClient == null || dbInstance == null) {
            return new ArrayList<>();
        }

        String instanceID = dbInstance.getDBInstanceIdentifier();

        DescribeDBSnapshotsRequest describeDBSnapshotsRequest
                = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(instanceID);
        DescribeDBSnapshotsResult describeDBSnapshotsResult
                = RDSClientMethods.describeDBSnapshots(region,
                        rdsClient,
                        describeDBSnapshotsRequest,
                        numRetries,
                        maxApiRequestsPerSecond,
                        uniqueAwsAccountIdentifier);

        List<DBSnapshot> snapshots = describeDBSnapshotsResult.getDBSnapshots();

        List<DBSnapshot> toRemove = new ArrayList();

        for (DBSnapshot dbSnapshot : snapshots) {
            if (!dbSnapshot.getStatus().equalsIgnoreCase("available")) {
                toRemove.add(dbSnapshot);
            }
        }

        snapshots.removeAll(toRemove);

        return snapshots;
    }

    public List<DBClusterSnapshot> getAllDBClusterSnapshotsOfDBCluster(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (rdsClient == null || dbCluster == null) {
            return new ArrayList<>();
        }

        String clusterID = dbCluster.getDBClusterIdentifier();

        DescribeDBClusterSnapshotsRequest describeDBClusterSnapshotsRequest
                = new DescribeDBClusterSnapshotsRequest().withDBClusterIdentifier(clusterID);
        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult
                = RDSClientMethods.describeDBClusterSnapshots(region,
                        rdsClient,
                        describeDBClusterSnapshotsRequest,
                        numRetries,
                        maxApiRequestsPerSecond,
                        uniqueAwsAccountIdentifier);

        List<DBClusterSnapshot> snapshots = describeDBClusterSnapshotsResult.getDBClusterSnapshots();

        List<DBClusterSnapshot> toRemove = new ArrayList();

        for (DBClusterSnapshot dbClusterSnapshot : snapshots) {
            if (!dbClusterSnapshot.getStatus().equalsIgnoreCase("available")) {
                toRemove.add(dbClusterSnapshot);
            }
        }

        snapshots.removeAll(toRemove);

        return snapshots;
    }

    public void sortDBSnapshotsByDate(List<DBSnapshot> comparelist) {

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
            Collections.sort(comparelist, new Comparator<DBSnapshot>() {
                @Override
                public int compare(DBSnapshot s1, DBSnapshot s2) {
                    return s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime());
                }
            });
        }
    }

    public void sortDBClusterSnapshotsByDate(List<DBClusterSnapshot> comparelist) {

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
            Collections.sort(comparelist, new Comparator<DBClusterSnapshot>() {
                @Override
                public int compare(DBClusterSnapshot s1, DBClusterSnapshot s2) {
                    return s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime());
                }
            });
        }
    }

    public int getHoursBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    public int getDaysBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }

    public int getHoursBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    public int getDaysBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getSnapshotCreateTime());
        return Days.daysBetween(dt, now).getDays();
    }
}
