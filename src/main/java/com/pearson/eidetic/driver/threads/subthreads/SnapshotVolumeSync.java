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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class SnapshotVolumeSync extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotVolumeSync.class.getName());

    private Boolean isFinished_ = false;
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final com.amazonaws.regions.Region region_;
    private final ArrayList<Volume> VolumeSync_;
    private final Boolean Validator_;

    public SnapshotVolumeSync(String awsAccessKeyId, String awsSecretKey, String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond,
            Integer numRetries, com.amazonaws.regions.Region region, ArrayList<Volume> VolumeSync, Boolean validator) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
        this.region_ = region;
        this.VolumeSync_ = VolumeSync;
        this.Validator_ = validator;
    }

    @Override
    public void run() {
        isFinished_ = false;
        AmazonEC2Client ec2Client = connect(region_, awsAccessKeyId_, awsSecretKey_);
     
        for (Volume vol : VolumeSync_) {
            try {

                Date date = new java.util.Date();
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
                
                //Same
                Integer keep = getKeep(eideticParameters, vol);
                if (keep == null) {
                    continue;
                }

                Boolean success;
                success = snapshotCreation(region_, ec2Client, vol, date);
                if (!success) {
                    continue;
                }

                snapshotDeletion(region_, ec2Client, vol, keep);

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

    public boolean snapshotCreation(Region region, AmazonEC2Client ec2Client, Volume vol, Date date) {
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

            String description;
            if (Validator_) {
                description = "sync_snapshot " + vol.getVolumeId() + " called by Eidetic Validator Synchronizer at " + date.toString()
                        + ". Volume attached to " + volumeAttachmentInstance;
            } else {
                description = "sync_snapshot " + vol.getVolumeId() + " by Eidetic Synchronizer at " + date.toString()
                        + ". Volume attached to " + volumeAttachmentInstance;
            }

            Snapshot current_snap;
            try {
                current_snap = createSnapshotOfVolume(region, ec2Client, vol, description, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
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

    public boolean snapshotDeletion(Region region, AmazonEC2Client ec2Client, Volume vol, Integer keep) {
        if ((keep == null) || (ec2Client == null) || (vol == null)) {
            return false;
        }

        try {
            List<Snapshot> del_snapshots = getAllSnapshotsOfVolume(region, ec2Client, vol, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);

            List<Snapshot> deletelist = new ArrayList();
            
            for (Snapshot snapshot : del_snapshots) {
                String desc = snapshot.getDescription();
                if (desc.startsWith("sync_snapshot")) {
                    deletelist.add(snapshot);
                }
            }
            List<Snapshot> sortedDeleteList = new ArrayList<>(deletelist);
            sortSnapshotsByDate(sortedDeleteList);

            int delta = sortedDeleteList.size() - keep;

            for (int i : range(0, delta - 1)) {
                try {
                    deleteSnapshot(region, ec2Client, vol, sortedDeleteList.get(i), numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
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
