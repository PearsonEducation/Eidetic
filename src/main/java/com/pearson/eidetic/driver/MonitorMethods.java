/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;

/**
 *
 * @author Judah Walker
 */
public class MonitorMethods implements Monitor {

    @Override
    public boolean areAllThreadsDead(List<? extends EideticSubThread> threads) {

        if ((threads == null) || threads.isEmpty()) {
            return true;
        }

        boolean areAllThreadsDead = true;

        for (EideticSubThread thread : threads) {
            if (!thread.isFinished()) {
                areAllThreadsDead = false;
            }
        }

        return areAllThreadsDead;
    }

    @Override
    //NEED TEST CASES!!!!
    public ArrayList<ArrayList<Volume>> splitArrayList(ArrayList<Volume> volumes, Integer splitFactor) {
        if ((volumes == null) | (splitFactor == null)) {
            return null;
        }

        ArrayList<ArrayList<Volume>> returnVols = new ArrayList<>();
        if (splitFactor <= 1) {
            returnVols.add(volumes);
            return returnVols;
        }
        
        if (volumes.size() <= splitFactor) {
            returnVols.add(volumes);
            return returnVols;
        }
        
        
        
        final int chunkLength = volumes.size() / splitFactor;
        final int totalLength = volumes.size();
        
        for (int i = 0; i < totalLength; i += chunkLength) {
            returnVols.add(
                    new ArrayList<>(volumes.subList(i,
                                    Math.min(totalLength, i + chunkLength)))
            );
        }

        return returnVols;
    }
    
    public static ArrayList<ArrayList<Volume>> listsToArrayLists(List<List<Volume>> input) {
        ArrayList<ArrayList<Volume>> returnList = new ArrayList();
        for (List<Volume> list : input) {
            ArrayList<Volume> newOne = new ArrayList();
            newOne.addAll(list);
            returnList.add(newOne);
        }
        return returnList;
        
    }
    
    public List<Snapshot> getAllSnapshotsOfVolume(Region region, AmazonEC2Client ec2Client, Volume vol,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {

        if (ec2Client == null || vol == null) {
            return new ArrayList<>();
        }

        String volumeID = vol.getVolumeId();

        Filter filter = new Filter().withName("volume-id").withValues(volumeID);

        DescribeSnapshotsRequest describeSnapshotsRequest
                = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filter);
        DescribeSnapshotsResult describeSnapshotsResult
                = EC2ClientMethods.describeSnapshots(region,
                        ec2Client,
                        describeSnapshotsRequest,
                        numRetries,
                        maxApiRequestsPerSecond,
                        uniqueAwsAccountIdentifier);

        List<Snapshot> snapshots = describeSnapshotsResult.getSnapshots();

        return snapshots;
    }

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

    public int getHoursBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getStartTime());
        return Hours.hoursBetween(dt, now).getHours();
    }

    public int getDaysBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList) {
        if (sortedCompareList == null || sortedCompareList.isEmpty()) {
            return -1;
        }

        DateTime now = new DateTime();
        DateTime dt = new DateTime(sortedCompareList.get(sortedCompareList.size() - 1).getStartTime());
        return Days.daysBetween(dt, now).getDays();
    }

}
