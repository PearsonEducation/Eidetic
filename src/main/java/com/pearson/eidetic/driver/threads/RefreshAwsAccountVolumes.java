/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class RefreshAwsAccountVolumes implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;
    private final HashMap<Volume, Boolean> volTimeHasTag_ = new HashMap();
    private final HashMap<Volume, Boolean> volNoTimeHasTag_ = new HashMap();
    private final HashMap<Volume, Boolean> volCopyHasTag_ = new HashMap();

    public RefreshAwsAccountVolumes(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
        this.uniqueAwsAccountIdentifier_ = awsAccount.getUniqueAwsAccountIdentifier();
        this.maxApiRequestsPerSecond_ = awsAccount.getMaxApiRequestsPerSecond();
        this.numRetries_ = ApplicationConfiguration.getAwsCallRetryAttempts();
    }

    @Override
    public void run() {

        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime;

        localVolumeNoTime = awsAccount_.getVolumeNoTime_Copy();

        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime;

        localVolumeTime = awsAccount_.getVolumeTime_Copy();

        ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots;

        localCopyVolumeSnapshots = awsAccount_.getCopyVolumeSnapshots_Copy();

        JSONParser parser = new JSONParser();

        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : localVolumeNoTime.entrySet()) {
            com.amazonaws.regions.Region region = entry.getKey();
            AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

            List<Volume> volumes = initialization(ec2Client,
                    localVolumeNoTime,
                    localVolumeTime,
                    localCopyVolumeSnapshots,
                    region);

            for (Volume volume : volumes) {

                JSONObject eideticParameters;
                eideticParameters = getEideticParameters(volume, parser);
                if (eideticParameters == null) {
                    continue;
                }

                JSONObject createSnapshot;
                createSnapshot = getCreateSnapshot(volume, eideticParameters);
                if (createSnapshot == null) {
                    continue;
                }

                HashMap<Integer, ConcurrentHashMap<Region, ArrayList<Volume>>> resultSet;
                resultSet = refreshCreateSnapshotVolumes(volume, createSnapshot, localVolumeTime, localVolumeNoTime, region);
                if (resultSet == null) {
                    continue;
                }
                if (resultSet.isEmpty()) {
                    continue;
                }

                try {
                    localVolumeTime = resultSet.get(0);
                    localVolumeNoTime = resultSet.get(1);
                } catch (Exception e) {
                    logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error getting from resultSet\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }

                //Time for CopyVolumeSnapshots
                //If this volume does not have copysnapshot
                localCopyVolumeSnapshots = refreshCopyVolumeSnapshots(volume, eideticParameters, localCopyVolumeSnapshots, region);

            }

            localVolumeTime = processLocalVolumeTime(localVolumeTime, region);

            localVolumeNoTime = processLocalVolumeNoTime(localVolumeNoTime, region);

            localCopyVolumeSnapshots = processLocalCopyVolumeSnapshots(localCopyVolumeSnapshots, region);
            
            ec2Client.shutdown();

        }

        awsAccount_.replaceVolumeNoTime(localVolumeNoTime);

        awsAccount_.replaceVolumeTime(localVolumeTime);

        awsAccount_.replaceCopyVolumeSnapshots(localCopyVolumeSnapshots);
        
        
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

    public List<Volume> initialization(AmazonEC2Client ec2Client,
            ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime,
            ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime,
            ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots,
            Region region) {
        Filter[] filters = new Filter[1];
        filters[0] = new Filter().withName("tag-key").withValues("Eidetic");

        DescribeVolumesRequest describeVolumesRequest
                = new DescribeVolumesRequest().withFilters(filters);
        DescribeVolumesResult describeVolumeResult
                = EC2ClientMethods.describeVolumes(ec2Client, 
                            describeVolumesRequest, 
                            numRetries_, 
                            maxApiRequestsPerSecond_, 
                            uniqueAwsAccountIdentifier_);
        
        for (Volume vol : localVolumeNoTime.get(region)) {
            volNoTimeHasTag_.put(vol, false);
        }
        for (Volume vol : localVolumeTime.get(region)) {
            volTimeHasTag_.put(vol, false);
        }
        for (Volume vol : localCopyVolumeSnapshots.get(region)) {
            volCopyHasTag_.put(vol, false);
        }

        List<Volume> volumes = describeVolumeResult.getVolumes();

        return volumes;
    }

    private JSONObject getEideticParameters(Volume volume, JSONParser parser) {
        JSONObject eideticParameters = null;
        for (Tag tag : volume.getTags()) {
            String tagValue = null;
            if (tag.getKey().equalsIgnoreCase("Eidetic")) {
                tagValue = tag.getValue();
            }
            if (tagValue == null) {
                continue;
            }

            try {
                Object obj = parser.parse(tagValue);
                eideticParameters = (JSONObject) obj;
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                break;
            }
        }
        return eideticParameters;
    }

    private JSONObject getCreateSnapshot(Volume volume, JSONObject eideticParameters) {
        JSONObject createSnapshot = null;
        try {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        } catch (Exception e) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
        }
        return createSnapshot;
    }

    private HashMap<Integer, ConcurrentHashMap<Region, ArrayList<Volume>>> refreshCreateSnapshotVolumes(Volume volume, JSONObject createSnapshot, ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime, ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime, Region region) {
        HashMap<Integer, ConcurrentHashMap<Region, ArrayList<Volume>>> resultSet = new HashMap();
        //New volume detected! Return value continue     
        if (!localVolumeNoTime.get(region).contains(volume) && !localVolumeTime.get(region).contains(volume)) {
            if (createSnapshot.containsKey("RunAt")) {//Add tries
                try {
                    localVolumeTime.get(region).add(volume);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to VolumeTime_\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                volTimeHasTag_.put(volume, true);
            } else {
                try {
                    localVolumeNoTime.get(region).add(volume);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to VolumeTime_\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                volNoTimeHasTag_.put(volume, true);
            }
            resultSet.put(0, localVolumeTime);
            resultSet.put(1, localVolumeNoTime);
            return resultSet;
        }

        //Old volume Tag is left unchanged
        if ((createSnapshot.containsKey("RunAt")) && localVolumeTime.get(region).contains(volume)) {
            volTimeHasTag_.replace(volume, true);
        } else if (!(createSnapshot.containsKey("RunAt")) && localVolumeNoTime.get(region).contains(volume)) {
            volNoTimeHasTag_.replace(volume, true);

            //If we change the value of the tag.
        } else if ((createSnapshot.containsKey("RunAt")) && localVolumeNoTime.get(region).contains(volume)) {
            volTimeHasTag_.put(volume, true);
            volNoTimeHasTag_.replace(volume, false);
            try {
                localVolumeTime.get(region).add(volume);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to VolumeNoTime_\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else if (!(createSnapshot.containsKey("RunAt")) && localVolumeTime.get(region).contains(volume)) {
            volNoTimeHasTag_.put(volume, true);
            volTimeHasTag_.replace(volume, false);
            try {
                localVolumeNoTime.get(region).add(volume);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to VolumeNoTime_\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        } else {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\"");
        }
        resultSet.put(0, localVolumeTime);
        resultSet.put(1, localVolumeNoTime);
        return resultSet;
    }

    private ConcurrentHashMap<Region, ArrayList<Volume>> refreshCopyVolumeSnapshots(Volume volume, JSONObject eideticParameters, ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots, Region region) {
        if (volume == null || eideticParameters == null || region == null) {
            return localCopyVolumeSnapshots;
        }
        if (!eideticParameters.containsKey("CopySnapshot")) {
            //and it previously did
            if (volCopyHasTag_.containsKey(volume)) {
                //We leave volCopyHasTag_ at false
                return localCopyVolumeSnapshots;
                //else it previously did not, and we do not worry
            } else {
                //we continue along to the next volume.
                return localCopyVolumeSnapshots;
            }
            //It does have CopySnapshot
        } else {
            //Did it previously?
            if (volCopyHasTag_.containsKey(volume)) {
                //Yeah it does, set to true
                try {
                    volCopyHasTag_.replace(volume, true);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to CopyVolumeSnapshots\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            } else {
                //It did not, we add to localCopyVolumeSnapshots
                try {
                    localCopyVolumeSnapshots.get(region).add(volume);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to CopyVolumeSnapshots\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }

                try {
                    volCopyHasTag_.put(volume, true);
                } catch (Exception e) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error adding vol to CopyVolumeSnapshots\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
            }
        }
        return localCopyVolumeSnapshots;
    }

    private ConcurrentHashMap<Region, ArrayList<Volume>> processLocalVolumeTime(ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime, Region region) {
        for (Map.Entry pair : volTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            Volume vol = (Volume) pair.getKey();
            try {
                localVolumeTime.get(region).remove(vol);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing vol from VolumeTime_\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        volTimeHasTag_.clear();
        return localVolumeTime;
    }

    private ConcurrentHashMap<Region, ArrayList<Volume>> processLocalVolumeNoTime(ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime, Region region) {
        for (Map.Entry pair : volNoTimeHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            Volume vol = (Volume) pair.getKey();
            try {
                localVolumeNoTime.get(region).remove(vol);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing vol from VolumeNoTime_\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        volNoTimeHasTag_.clear();
        return localVolumeNoTime;
    }

    private ConcurrentHashMap<Region, ArrayList<Volume>> processLocalCopyVolumeSnapshots(ConcurrentHashMap<Region, ArrayList<Volume>> localCopyVolumeSnapshots, Region region) {
        for (Map.Entry pair : volCopyHasTag_.entrySet()) {
            Boolean contin = (Boolean) pair.getValue();
            if (contin) {
                continue;
            }
            Volume vol = (Volume) pair.getKey();
            try {
                localCopyVolumeSnapshots.get(region).remove(vol);
            } catch (Exception e) {
                logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error removing vol from CopyVolumeSnapshots_\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
        volCopyHasTag_.clear();
        return localCopyVolumeSnapshots;
    }

}
