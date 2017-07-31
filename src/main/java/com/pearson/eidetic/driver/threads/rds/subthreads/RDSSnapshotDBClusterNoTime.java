/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds.subthreads;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.driver.threads.rds.RDSSubThread;
import com.pearson.eidetic.driver.threads.rds.RDSSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class RDSSnapshotDBClusterNoTime extends RDSSubThreadMethods implements Runnable, RDSSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = false;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String awsAccountId_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final com.amazonaws.regions.Region region_;
    private final ArrayList<DBCluster> DBClusterNoTime_;
    private final HashMap<DBCluster, Collection<Tag>> DBClusterTags_;
    private final HashMap<DBClusterSnapshot, Collection<Tag>> DBClusterSnapshotTags_;

    public RDSSnapshotDBClusterNoTime(String awsAccessKeyId, String awsSecretKey, String awsAccountId, String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond,
            Integer numRetries, com.amazonaws.regions.Region region, ArrayList<DBCluster> DBClusterNoTime) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.awsAccountId_ = awsAccountId;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
        this.region_ = region;
        this.DBClusterNoTime_ = DBClusterNoTime;
        this.DBClusterTags_ = new HashMap();
        this.DBClusterSnapshotTags_ = new HashMap();
    }

    @Override
    public void run() {
        isFinished_ = false;
        //kill thread if wrong creds \/ \/ \/ \/
        AmazonRDSClient rdsClient = connect(region_, awsAccessKeyId_, awsSecretKey_);

        for (DBCluster dbCluster : DBClusterNoTime_) {
            try {
                Date date = new java.util.Date();

                JSONObject eideticParameters = getIntTagValue(rdsClient, dbCluster);
                if (eideticParameters == null) {
                    continue;
                }

                String period = getPeriod(eideticParameters, dbCluster);
                if (period == null) {
                    continue;
                }

                Integer keep = getKeep(eideticParameters, dbCluster);
                if (keep == null) {
                    continue;
                }

                Boolean success;
                success = snapshotDecision(rdsClient, dbCluster, period);
                if (!success) {
                    continue;
                }

                success = snapshotCreation(rdsClient, dbCluster, period, date);
                if (!success) {
                    continue;
                }

                snapshotDeletion(rdsClient, dbCluster, period, keep);

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in RDSSnapshotDBClusterNoTime workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }

        }
        rdsClient.shutdown();
        isFinished_ = true;

    }

    @Override
    public boolean isFinished() {
        return isFinished_;
    }

    public AmazonRDSClient connect(Region region, String awsAccessKey, String awsSecretKey) {
        AmazonRDSClient rdsClient;
        String endpoint = "rds." + region.getName() + ".amazonaws.com";

        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTPS);

        rdsClient = new AmazonRDSClient(credentials, clientConfig);
        rdsClient.setRegion(region);
        rdsClient.setEndpoint(endpoint);
        return rdsClient;
    }

    public JSONObject getIntTagValue(AmazonRDSClient amazonRDSClient, DBCluster dbCluster) {
        if (dbCluster == null) {
            return null;
        }

        String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region_.getName(), awsAccountId_, dbCluster.getDBClusterIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        List<Tag> tags = RDSClientMethods.getTags(amazonRDSClient,
                listTagsForResourceRequest,
                ApplicationConfiguration.getAwsCallRetryAttempts(),
                maxApiRequestsPerSecond_,
                uniqueAwsAccountIdentifier_).getTagList();

        JSONObject eideticParameters = new JSONObject();
        JSONObject createSnapshot = new JSONObject();

        for (Tag tag : tags) {
            if ("Eidetic_Interval".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Interval", tag.getValue());
            } else if ("Eidetic_RunAt".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("RunAt", tag.getValue());
            } else if ("Eidetic_Retain".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Retain", tag.getValue());
            }
        }

        DBClusterTags_.put(dbCluster, tags);

        eideticParameters.put("CreateSnapshot", createSnapshot);
        return eideticParameters;
    }

    public String getPeriod(JSONObject eideticParameters, DBCluster dbCluster) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\"");
            return null;
        }

        String period = null;
        if (createSnapshot.containsKey("Interval")) {
            try {
                period = createSnapshot.get("Interval").toString();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return period;
    }

    public Integer getKeep(JSONObject eideticParameters, DBCluster dbCluster) {
        if ((eideticParameters == null) | (dbCluster == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\"");
            return null;
        }

        Integer keep = null;
        if (createSnapshot.containsKey("Retain")) {
            try {
                keep = Integer.parseInt(createSnapshot.get("Retain").toString());
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return keep;
    }

    public boolean snapshotDecision(AmazonRDSClient rdsClient, DBCluster dbCluster, String period) {
        if ((rdsClient == null) || (dbCluster == null) || (period == null)) {
            return false;
        }
        try {

            List<DBClusterSnapshot> int_snapshots = getAllDBClusterSnapshotsOfDBCluster(rdsClient, dbCluster, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<DBClusterSnapshot> comparelist = new ArrayList();

            for (DBClusterSnapshot snapshot : int_snapshots) {
                if (!snapshot.getDBClusterSnapshotIdentifier().startsWith("eidetic")){
                    continue;
                }
                //getResourceTags(AmazonRDSClient rdsClient, String arn, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier)
                String arn = String.format("arn:aws:rds:%s:%s:cluster-snapshot:%s", region_.getName(), awsAccountId_, snapshot.getDBClusterSnapshotIdentifier());
                Collection<Tag> tags_dbClusterSnapshot = getResourceTags(rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                DBClusterSnapshotTags_.put(snapshot, tags_dbClusterSnapshot);
                String sndesc = null;
                for (Tag tag : tags_dbClusterSnapshot) {
                    if ("description".equalsIgnoreCase(tag.getKey())) {
                        sndesc = tag.getValue();
                        break;
                    }
                }
                if (sndesc == null) {
                    //   logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbsnapshot has not description tag\",\"DBClusterSnapshot_Identifier=\""
                    //         + snapshot.getDBClusterSnapshotIdentifier() + "\"");
                    continue;
                }

                if ("week".equalsIgnoreCase(period) && sndesc.startsWith("week_snapshot")) {
                    comparelist.add(snapshot);
                } else if ("day".equalsIgnoreCase(period) && sndesc.startsWith("day_snapshot")) {
                    comparelist.add(snapshot);
                } else if ("hour".equalsIgnoreCase(period) && sndesc.startsWith("hour_snapshot")) {
                    comparelist.add(snapshot);
                } else if ("month".equalsIgnoreCase(period) && sndesc.startsWith("month_snapshot")) {
                    comparelist.add(snapshot);
                }
            }

            List<DBClusterSnapshot> sortedCompareList = new ArrayList<>(comparelist);
            sortDBClusterSnapshotsByDate(sortedCompareList);

            int hours = getHoursBetweenNowAndNewestDBClusterSnapshot(sortedCompareList);
            int days = getDaysBetweenNowAndNewestDBClusterSnapshot(sortedCompareList);

            if (("week".equalsIgnoreCase(period) && days < 0) || ("week".equalsIgnoreCase(period) && days >= 7)) {
            } else if (("hour".equalsIgnoreCase(period) && hours < 0) || ("hour".equalsIgnoreCase(period) && hours >= 1)) {
            } else if (("day".equalsIgnoreCase(period) && days < 0) || ("day".equalsIgnoreCase(period) && days >= 1)) {
            } else if (("month".equalsIgnoreCase(period) && days < 0) || ("month".equalsIgnoreCase(period) && days >= 30)) {
            } else {
                return false;
            }

        } catch (Exception e) {
            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

    public boolean snapshotCreation(AmazonRDSClient rdsClient, DBCluster dbCluster, String period, Date date) {
        if ((date == null) || (rdsClient == null) || (dbCluster == null) || (period == null)) {
            return false;
        }

        try {

            if ("day".equalsIgnoreCase(period)) {
            } else if ("hour".equalsIgnoreCase(period)) {
            } else if ("week".equalsIgnoreCase(period)) {
            } else if ("month".equalsIgnoreCase(period)) {
            } else {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\"");
                return false;
            }

            Collection<Tag> tags;

            if (DBClusterTags_.containsKey(dbCluster)) {
                tags = DBClusterTags_.get(dbCluster);
            } else {
                String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region_.getName(), awsAccountId_, dbCluster.getDBClusterIdentifier());
                tags = getResourceTags(rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                DBClusterTags_.put(dbCluster, tags);
            }

            String dbClusterIdentifier = "none";
            try {
                dbClusterIdentifier = dbCluster.getDBClusterIdentifier();
            } catch (Exception e) {
                logger.debug("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\", dbCluster: " + dbCluster.getDBClusterIdentifier());
            }

            String description = period + "_snapshot " + dbCluster.getDBClusterIdentifier() + " by Eidetic NoTime at " + date.toString();

            tags.add(new Tag().withKey("description").withValue(description));

            DBClusterSnapshot current_snap;
            try {
                //createDBClusterSnapshotOfDBCluster(AmazonRDSClient rdsClient, DBCluster dbCluster, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier
                current_snap = createDBClusterSnapshotOfDBCluster(rdsClient, dbCluster, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Creating snapshot from dbCluster\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", \"snapshot\"=\"" + current_snap.getDBClusterSnapshotIdentifier() + "\"");
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error creating snapshot from dbCluster\", DBCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return false;
            }
            
            ArrayList<Tag> removeTags = new ArrayList();
            for (Tag tag : tags) {
                if (tag.getKey().startsWith("aws:")) {
                    removeTags.add(tag);
                }
            }
            for (Tag tag : removeTags) {
                try {
                    tags.remove(tag);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\", could not remove tag from tags: " + dbCluster.getDBClusterIdentifier() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }

            try {
                String arn = String.format("arn:aws:rds:%s:%s:cluster-snapshot:%s", region_.getName(), awsAccountId_, current_snap.getDBClusterSnapshotIdentifier());
                setResourceTags(rdsClient, arn, tags, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + current_snap.getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return false;
            }

            DBClusterSnapshotTags_.put(current_snap, tags);

        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error, Error=\"error in snapshotCreation\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

    public boolean snapshotDeletion(AmazonRDSClient rdsClient, DBCluster dbCluster, String period, Integer keep) {
        if ((keep == null) || (rdsClient == null) || (dbCluster == null) || (period == null)) {
            return false;
        }

        try {
            List<DBClusterSnapshot> del_snapshots = getAllDBClusterSnapshotsOfDBCluster(rdsClient, dbCluster, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<DBClusterSnapshot> deletelist = new ArrayList();

            for (DBClusterSnapshot snapshot : del_snapshots) {
                if (!snapshot.getDBClusterSnapshotIdentifier().startsWith("eidetic")){
                    continue;
                }
                String desc = null;
                Collection<Tag> tags;
                if (DBClusterSnapshotTags_.containsKey(snapshot)) {
                    tags = DBClusterSnapshotTags_.get(snapshot);
                } else {
                    String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region_.getName(), awsAccountId_, snapshot.getDBClusterSnapshotIdentifier());
                    tags = getResourceTags(rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    DBClusterSnapshotTags_.put(snapshot, tags);
                }

                if (tags == null) {
                    continue;
                }
                for (Tag tag : tags) {
                    if ("description".equalsIgnoreCase(tag.getKey())) {
                        desc = tag.getValue();
                        break;
                    }
                }
                if (desc == null) {
                    //    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbsnapshot has not description tag\",\"DBClusterSnapshot_Identifier=\""
                    //          + snapshot.getDBClusterSnapshotIdentifier() + "\"");
                    continue;
                }
                if ("week".equals(period) && desc.startsWith("week_snapshot")) {
                    deletelist.add(snapshot);
                } else if ("day".equals(period) && desc.startsWith("day_snapshot")) {
                    deletelist.add(snapshot);
                } else if ("hour".equals(period) && desc.startsWith("hour_snapshot")) {
                    deletelist.add(snapshot);
                } else if ("month".equals(period) && desc.startsWith("month_snapshot")) {
                    deletelist.add(snapshot);
                }
            }

            List<DBClusterSnapshot> sortedDeleteList = new ArrayList<>(deletelist);
            sortDBClusterSnapshotsByDate(sortedDeleteList);

            int delta = sortedDeleteList.size() - (keep - 1);

            for (int i : range(0, delta - 1)) {
                try {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Deleting snapshot from dbCluster\", DBCluster_Identifier=\"" + sortedDeleteList.get(i).getDBClusterIdentifier() + "\", \"dbClusterSnapshot\"=\"" + sortedDeleteList.get(i).getDBClusterSnapshotIdentifier() + "\"");
                    deleteDBClusterSnapshot(rdsClient, sortedDeleteList.get(i), numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + sortedDeleteList.get(i).getDBClusterSnapshotIdentifier() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDeletion\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }

        return true;
    }
}
