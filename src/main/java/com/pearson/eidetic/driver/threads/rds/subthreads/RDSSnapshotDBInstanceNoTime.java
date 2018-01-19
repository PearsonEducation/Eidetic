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
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
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
public class RDSSnapshotDBInstanceNoTime extends RDSSubThreadMethods implements Runnable, RDSSubThread {

    private static final Logger logger = LoggerFactory.getLogger(RDSSnapshotDBInstanceNoTime.class.getName());

    private Boolean isFinished_ = false;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String awsAccountId_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final com.amazonaws.regions.Region region_;
    private final ArrayList<DBInstance> DBInstanceNoTime_;
    private final HashMap<DBInstance, Collection<Tag>> DBInstanceTags_;
    private final HashMap<DBSnapshot, Collection<Tag>> DBSnapshotTags_;

    public RDSSnapshotDBInstanceNoTime(String awsAccessKeyId, String awsSecretKey, String awsAccountId, String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond,
            Integer numRetries, com.amazonaws.regions.Region region, ArrayList<DBInstance> DBInstanceNoTime) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.awsAccountId_ = awsAccountId;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
        this.region_ = region;
        this.DBInstanceNoTime_ = DBInstanceNoTime;
        this.DBInstanceTags_ = new HashMap();
        this.DBSnapshotTags_ = new HashMap();
    }

    @Override
    public void run() {
        isFinished_ = false;
        //kill thread if wrong creds \/ \/ \/ \/
        AmazonRDSClient rdsClient = connect(region_, awsAccessKeyId_, awsSecretKey_);

        for (DBInstance dbInstance : DBInstanceNoTime_) {

            try {
                Date date = new java.util.Date();

                JSONObject eideticParameters = getIntTagValue(region_, rdsClient, dbInstance);
                if (eideticParameters == null) {
                    continue;
                }

                String period = getPeriod(eideticParameters, dbInstance);
                if (period == null) {
                    continue;
                }

                Integer keep = getKeep(eideticParameters, dbInstance);
                if (keep == null) {
                    continue;
                }

                Boolean success;
                success = snapshotDecision(region_, rdsClient, dbInstance, period);
                if (!success) {
                    continue;
                }

                success = snapshotCreation(region_, rdsClient, dbInstance, period, date);
                if (!success) {
                    continue;
                }

                snapshotDeletion(region_, rdsClient, dbInstance, period, keep);

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in RDSSnapshotDBInstanceNoTime workflow\", stacktrace=\""
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

    public JSONObject getIntTagValue(Region region, AmazonRDSClient amazonRDSClient, DBInstance dbInstance) {
        if (dbInstance == null) {
            return null;
        }

        String arn = String.format("arn:aws:rds:%s:%s:db:%s", region_.getName(), awsAccountId_, dbInstance.getDBInstanceIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        List<Tag> tags = RDSClientMethods.getTags(region,
                amazonRDSClient,
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

        DBInstanceTags_.put(dbInstance, tags);

        eideticParameters.put("CreateSnapshot", createSnapshot);
        return eideticParameters;

    }

    public String getPeriod(JSONObject eideticParameters, DBInstance dbInstance) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\"");
            return null;
        }

        String period = null;
        if (createSnapshot.containsKey("Interval")) {
            try {
                period = createSnapshot.get("Interval").toString();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return period;
    }

    public Integer getKeep(JSONObject eideticParameters, DBInstance dbInstance) {
        if ((eideticParameters == null) | (dbInstance == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\"");
            return null;
        }

        Integer keep = null;
        if (createSnapshot.containsKey("Retain")) {
            try {
                keep = Integer.parseInt(createSnapshot.get("Retain").toString());
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return keep;
    }

    public boolean snapshotDecision(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, String period) {
        if ((rdsClient == null) || (dbInstance == null) || (period == null)) {
            return false;
        }
        try {

            List<DBSnapshot> int_snapshots = getAllDBSnapshotsOfDBInstance(region, rdsClient, dbInstance, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<DBSnapshot> comparelist = new ArrayList();

            for (DBSnapshot snapshot : int_snapshots) {
                if (!snapshot.getDBSnapshotIdentifier().startsWith("eidetic")) {
                    continue;
                }
                //getResourceTags(AmazonRDSClient rdsClient, String arn, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier)
                String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region_.getName(), awsAccountId_, snapshot.getDBSnapshotIdentifier());
                Collection<Tag> tags_dbSnapshot = getResourceTags(region, rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                DBSnapshotTags_.put(snapshot, tags_dbSnapshot);
                String sndesc = null;
                for (Tag tag : tags_dbSnapshot) {
                    if ("description".equalsIgnoreCase(tag.getKey())) {
                        sndesc = tag.getValue();
                        break;
                    }
                }
                if (sndesc == null) {
                    //logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbsnapshot has not description tag\",\"DBSnapshot_Identifier=\""
                    //       + snapshot.getDBSnapshotIdentifier() + "\"");
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

            List<DBSnapshot> sortedCompareList = new ArrayList<>(comparelist);
            sortDBSnapshotsByDate(sortedCompareList);

            int hours = getHoursBetweenNowAndNewestDBSnapshot(sortedCompareList);
            int days = getDaysBetweenNowAndNewestDBSnapshot(sortedCompareList);

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

    public boolean snapshotCreation(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, String period, Date date) {
        if ((date == null) || (rdsClient == null) || (dbInstance == null) || (period == null)) {
            return false;
        }

        try {

            if ("day".equalsIgnoreCase(period)) {
            } else if ("hour".equalsIgnoreCase(period)) {
            } else if ("week".equalsIgnoreCase(period)) {
            } else if ("month".equalsIgnoreCase(period)) {
            } else {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\"");
                return false;
            }

            Collection<Tag> tags;

            if (DBInstanceTags_.containsKey(dbInstance)) {
                tags = DBInstanceTags_.get(dbInstance);
            } else {
                String arn = String.format("arn:aws:rds:%s:%s:db:%s", region_.getName(), awsAccountId_, dbInstance.getDBInstanceIdentifier());
                tags = getResourceTags(region, rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                DBInstanceTags_.put(dbInstance, tags);
            }

            String dbInstanceIdentifier = "none";
            try {
                dbInstanceIdentifier = dbInstance.getDBInstanceIdentifier();
            } catch (Exception e) {
                logger.debug("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\", Could not get dbInstance: " + dbInstance.getDBInstanceIdentifier());
            }

            String description = period + "_snapshot " + dbInstance.getDBInstanceIdentifier() + " by Eidetic NoTime at " + date.toString();
            tags.add(new Tag().withKey("description").withValue(description));

            DBSnapshot current_snap;
            try {
                //createDBSnapshotOfDBInstance(AmazonRDSClient rdsClient, DBInstance dbInstance, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier
                current_snap = createDBSnapshotOfDBInstance(region, rdsClient, dbInstance, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Creating snapshot from dbInstance\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\", \"snapshot\"=\"" + current_snap.getDBSnapshotIdentifier() + "\"");
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error creating snapshot from dbInstance\", DBInstance_Identifier=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
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
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\", could not remove tag from tags: " + dbInstance.getDBInstanceIdentifier() + System.lineSeparator() + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }

            try {
                String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region_.getName(), awsAccountId_, current_snap.getDBSnapshotIdentifier());
                setResourceTags(region, rdsClient, arn, tags, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + current_snap.getDBSnapshotIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return false;
            }

            DBSnapshotTags_.put(current_snap, tags);

        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error, Error=\"error in snapshotCreation\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

    public boolean snapshotDeletion(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, String period, Integer keep) {
        if ((keep == null) || (rdsClient == null) || (dbInstance == null) || (period == null)) {
            return false;
        }

        try {
            List<DBSnapshot> del_snapshots = getAllDBSnapshotsOfDBInstance(region, rdsClient, dbInstance, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<DBSnapshot> deletelist = new ArrayList();

            for (DBSnapshot snapshot : del_snapshots) {
                if (!snapshot.getDBSnapshotIdentifier().startsWith("eidetic")) {
                    continue;
                }
                String desc = null;
                Collection<Tag> tags;
                if (DBSnapshotTags_.containsKey(snapshot)) {
                    tags = DBSnapshotTags_.get(snapshot);
                } else {
                    String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region_.getName(), awsAccountId_, snapshot.getDBSnapshotIdentifier());
                    tags = getResourceTags(region, rdsClient, arn, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                    DBSnapshotTags_.put(snapshot, tags);
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
                    //logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbsnapshot has not description tag\",\"DBSnapshot_Identifier=\""
                    //      + snapshot.getDBSnapshotIdentifier() + "\"");
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

            List<DBSnapshot> sortedDeleteList = new ArrayList<>(deletelist);
            sortDBSnapshotsByDate(sortedDeleteList);

            int delta = sortedDeleteList.size() - (keep - 1);

            for (int i : range(0, delta - 1)) {
                try {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Info\", Info=\"Deleting snapshot from dbInstance\", DBInstance_Identifier=\"" + sortedDeleteList.get(i).getDBInstanceIdentifier() + "\", \"dbSnapshot\"=\"" + sortedDeleteList.get(i).getDBSnapshotIdentifier() + "\"");
                    deleteDBSnapshot(region, rdsClient, dbInstance, sortedDeleteList.get(i), numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + sortedDeleteList.get(i).getDBSnapshotIdentifier() + "\", stacktrace=\""
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
