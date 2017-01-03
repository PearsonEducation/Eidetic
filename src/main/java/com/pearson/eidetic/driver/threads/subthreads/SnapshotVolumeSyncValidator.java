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
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import com.pearson.eidetic.driver.threads.EideticSubThreadMethods;
import static com.pearson.eidetic.driver.threads.EideticSubThreadMethods.range;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javafx.util.Pair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class SnapshotVolumeSyncValidator extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = false;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final com.amazonaws.regions.Region region_;
    private final ArrayList<Volume> VolumeSyncValidate_;
    private final HashMap<String, ArrayList<MutableTriple<Volume, Integer, Integer>>> validateCluster_ = new HashMap<>();

    public SnapshotVolumeSyncValidator(String awsAccessKeyId, String awsSecretKey, String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond,
            Integer numRetries, com.amazonaws.regions.Region region, ArrayList<Volume> VolumeSyncValidate) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
        this.region_ = region;
        this.VolumeSyncValidate_ = VolumeSyncValidate;
    }

    @Override
    public void run() {
        isFinished_ = false;
        AmazonEC2Client ec2Client = connect(region_, awsAccessKeyId_, awsSecretKey_);

        for (Volume vol : VolumeSyncValidate_) {
            try {

                JSONParser parser = new JSONParser();

                String inttagvalue = getIntTagValue(vol);
                if (inttagvalue == null) {
                    continue;
                }

                JSONObject eideticParameters;
                try {
                    Object obj = parser.parse(inttagvalue);
                    eideticParameters = (JSONObject) obj;
                } catch (Exception e) {
                    logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }
                if (eideticParameters == null) {
                    continue;
                }

                //Same
                Integer keep = getKeep(eideticParameters, vol);
                if (keep == null) {
                    continue;
                }

                JSONObject syncSnapshot = null;
                if (eideticParameters.containsKey("SyncSnapshot")) {
                    syncSnapshot = (JSONObject) eideticParameters.get("SyncSnapshot");
                }
                if (syncSnapshot == null) {
                    logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
                    continue;
                }

                JSONObject validateParameters;
                try {
                    validateParameters = (JSONObject) syncSnapshot.get("Validate");
                } catch (Exception e) {
                    logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }

                Integer createAfter = getCreateAfter(validateParameters, vol);

                String cluster = getCluster(validateParameters, vol);               

                if (validateCluster_.containsKey(cluster)) {
                    validateCluster_.get(cluster).add(new MutableTriple(vol, createAfter, keep));
                } else {
                    validateCluster_.put(cluster, new ArrayList<MutableTriple<Volume, Integer, Integer>>());
                    validateCluster_.get(cluster).add(new MutableTriple(vol, createAfter, keep));
                }

            } catch (Exception e) {
                logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in SnapshotVolumeSync workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }

        }

        for (String cluster : validateCluster_.keySet()) {

            try {
                ArrayList<MutableTriple<Volume, Integer, Integer>> myList = validateCluster_.get(cluster);

                Boolean snapshotCluster = false;
                Integer min = Integer.MAX_VALUE;
                for (MutableTriple trip : myList) {
                    if (((Integer) trip.getMiddle()) < min) {
                        min = (Integer) trip.getMiddle();
                    }
                }
                

                for (MutableTriple trip : myList) {
                    if (snapshotDecision(ec2Client, (Volume) trip.getLeft(), min)) {
                        snapshotCluster = true;
                    }
                }

                if (snapshotCluster) {
                    ArrayList<Volume> vols = new ArrayList<>();
                    for (MutableTriple trip : myList) {
                        vols.add((Volume) trip.getLeft());
                    }

                    SnapshotVolumeSync thread = new SnapshotVolumeSync(
                            awsAccessKeyId_,
                            awsSecretKey_,
                            uniqueAwsAccountIdentifier_,
                            maxApiRequestsPerSecond_,
                            ApplicationConfiguration.getAwsCallRetryAttempts(),
                            region_,
                            vols,
                            true);

                    try {
                        thread.run();
                    } catch (Exception e) {
                        String responseMessage = "Error running cluster validator thread for cluster " + cluster;
                        logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Error=\"" + responseMessage + "\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }

                    Integer wait = 0;
                    while (!(thread.isFinished()) && (wait <= 200)) {
                        Threads.sleepMilliseconds(250);
                        //break after 50 seconds
                        wait = wait + 1;
                    }

                }

            } catch (Exception e) {
                logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in SnapshotVolumeSync workflow\", stacktrace=\""
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

    public Integer getKeep(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null) | (vol == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("SyncSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("SyncSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
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

    private Integer getCreateAfter(JSONObject validateParameters, Volume vol) {
        if ((validateParameters == null) | (vol == null)) {
            return null;
        }

        Integer createAfter = null;
        if (validateParameters.containsKey("CreateAfter")) {
            try {
                createAfter = Integer.parseInt(validateParameters.get("CreateAfter").toString());
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return createAfter;
    }

    private String getCluster(JSONObject validateParameters, Volume vol) {
        if ((validateParameters == null) | (vol == null)) {
            return null;
        }

        String cluster = null;
        if (validateParameters.containsKey("Cluster")) {
            try {
                cluster = validateParameters.get("Cluster").toString();
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return cluster;

    }

    public boolean snapshotDecision(AmazonEC2Client ec2Client, Volume vol, Integer createAfter) {
        if ((ec2Client == null) || (vol == null) || (createAfter == null)) {
            return false;
        }
        try {

            List<Snapshot> int_snapshots = getAllSnapshotsOfVolume(ec2Client, vol, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<Snapshot> comparelist = new ArrayList();

            for (Snapshot snapshot : int_snapshots) {
                String sndesc = snapshot.getDescription();
                if (sndesc.startsWith("sync_snapshot")) {
                    comparelist.add(snapshot);
                }
            }

            List<Snapshot> sortedCompareList = new ArrayList<>(comparelist);
            sortSnapshotsByDate(sortedCompareList);

            int hours = getHoursBetweenNowAndNewestSnapshot(sortedCompareList);
            int days = getDaysBetweenNowAndNewestSnapshot(sortedCompareList);

            if (hours <= createAfter) {
                return false;
            }

        } catch (Exception e) {
            logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

    public boolean snapshotCreation(AmazonEC2Client ec2Client, Volume vol, Date date) {
        if ((date == null) || (ec2Client == null) || (vol == null)) {
            return false;
        }

        try {

            Collection<Tag> tags_volume = getResourceTags(vol);

            String volumeAttachmentInstance = "none";
            try {
                volumeAttachmentInstance = vol.getAttachments().get(0).getInstanceId();
            } catch (Exception e) {
                logger.debug("Volume not attached to instance: " + vol.getVolumeId());
            }

            String description = "sync_snapshot " + vol.getVolumeId() + " by Eidetic Synchronizer at " + date.toString()
                    + ". Volume attached to " + volumeAttachmentInstance;

            Snapshot current_snap;
            try {
                current_snap = createSnapshotOfVolume(ec2Client, vol, description, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return false;
            }

            try {
                setResourceTags(ec2Client, current_snap, tags_volume, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event\"Error\", Error=\"error adding tags to snapshot\", Snapshot_id=\"" + current_snap.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return false;
            }

        } catch (Exception e) {
            logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error, Error=\"error in snapshotCreation\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

    public boolean snapshotDeletion(AmazonEC2Client ec2Client, Volume vol, Integer keep) {
        if ((keep == null) || (ec2Client == null) || (vol == null)) {
            return false;
        }

        try {
            List<Snapshot> del_snapshots = getAllSnapshotsOfVolume(ec2Client, vol, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<Snapshot> deletelist = new ArrayList();

            List<Snapshot> sortedDeleteList = new ArrayList<>(deletelist);
            sortSnapshotsByDate(sortedDeleteList);

            int delta = sortedDeleteList.size() - keep;

            for (int i : range(0, delta - 1)) {
                try {
                    deleteSnapshot(ec2Client, sortedDeleteList.get(i), numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
                } catch (Exception e) {
                    logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + sortedDeleteList.get(i).getSnapshotId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        } catch (Exception e) {
            logger.error("awsAccountId=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDeletion\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }

        return true;
    }

}
