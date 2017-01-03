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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import com.pearson.eidetic.driver.threads.EideticSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class TagChecker extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    //look at all instances with data tag
    //check to see if data volume has eidetic tags
    //apply "default eidetictag" from application.properties
    //log error volume did not have tag
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = null;
    private AwsAccount awsAccount_ = null;
    private final String defaultEideticTagValue_;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;

    public TagChecker(AwsAccount awsAccount, String defaultEideticTagValue, String uniqueAwsAccountIdentifier,
            Integer maxApiRequestsPerSecond, Integer numRetries) {
        this.awsAccount_ = awsAccount;
        this.defaultEideticTagValue_ = defaultEideticTagValue;
        this.uniqueAwsAccountIdentifier_ = uniqueAwsAccountIdentifier;
        this.maxApiRequestsPerSecond_ = maxApiRequestsPerSecond;
        this.numRetries_ = numRetries;
    }

    //check for all error snapshots in all regions
    //log error all error snapshots
    //delete all error snapshots
    @Override
    public void run() {
        isFinished_ = false;
        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeNoTime;
        localVolumeNoTime = awsAccount_.getVolumeNoTime_Copy();

        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : localVolumeNoTime.entrySet()) {
            try {
                //Get all pending!
                com.amazonaws.regions.Region region = entry.getKey();
                AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

                List<Instance> instances = getAllInstancesWithDataTag(ec2Client);
                if (instances.isEmpty()) {
                    continue;
                }

                List<Volume> volumesWithNoEidetic = checkVolumes(ec2Client, instances);
                if (volumesWithNoEidetic.isEmpty()) {
                    continue;
                }

                addTagToVolumes(ec2Client, volumesWithNoEidetic);

                ec2Client.shutdown();

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in TagChecker workflow\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

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

    private List<Instance> getAllInstancesWithDataTag(AmazonEC2Client ec2Client) {
        Filter[] filters = new Filter[1];
        filters[0] = new Filter().withName("tag-key").withValues("Data");

        DescribeInstancesRequest describeInstanceRequest
                = new DescribeInstancesRequest().withFilters(filters);
        DescribeInstancesResult describeInstancesResult
                = EC2ClientMethods.describeInstances(ec2Client,
                        describeInstanceRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        List<Instance> instances = new ArrayList();
        for (Reservation reservation : describeInstancesResult.getReservations()) {
            instances.addAll(reservation.getInstances());
        }

        return instances;
    }

    private List<Volume> checkVolumes(AmazonEC2Client ec2Client, List<Instance> instances) {
        List<Volume> volumesWithNoTags = new ArrayList();

        /*
         Here is what we have:
         Instance:
         -instance.id -> String
         -instance.blockdevicemapping -> InstanceBlockDeviceMapping
         -BlockDeviceMapping:
         --BlockDeviceMapping.deviceName -> String == (/dev/xvdk)
         --BlockDeviceMapping.getEbs -> ​EbsInstanceBlockDevice
         --EbsInstanceBLockDevice:
         ---EbsInstanceBLockDevice.id - String == (volume.id)
        
         For instances
         Set<HashMap<instance.id,HashMap<deviceName,volume.id>>>
        
         Volume:
         -volume.id -> String
         -volume.getAttachments -> volumeAttachments
         -volumeAttachments:
         --getDevice -> String == (/dev/xvdk)
         --getInstanceId -> String == instance.id
        
         For vols
         Set<HashMap<instance.id,HashMap<deviceName,volume.id>>>
                
         If anything remians in the instance set 
        
         */
        List<Volume> volumes = getEideticVolumes(ec2Client);
        //if (volumes.isEmpty()) {
        //return volumesWithNoTags;
        //}

        //Set<HashMap<instance.id,HashMap<deviceName,volume.id>>>
        Set<HashMap<String, HashMap<String, String>>> volumeSet = new HashSet();
        for (Volume volume : volumes) {
            String volumeId = volume.getVolumeId();
            List<VolumeAttachment> attachments = volume.getAttachments();
            for (VolumeAttachment attach : attachments) {
                HashMap<String, HashMap<String, String>> setMember = new HashMap();
                HashMap<String, String> internalHash = new HashMap();

                if (volumeId == null || attach.getDevice() == null || attach.getInstanceId() == null) {
                    continue;
                }

                internalHash.put(attach.getDevice(), volumeId);
                setMember.put(attach.getInstanceId(), internalHash);

                volumeSet.add(setMember);
            }
        }

        //Set<HashMap<instance.id,HashMap<deviceName,volume.id>>>
        Set<HashMap<String, HashMap<String, String>>> instanceSet = new HashSet();
        for (Instance instance : instances) {
            String instanceId = instance.getInstanceId();
            List<InstanceBlockDeviceMapping> blocks = instance.getBlockDeviceMappings();

            List<Tag> tags = instance.getTags();

            Tag tagz = null;
            for (Tag t : tags) {
                if (t.getKey().equalsIgnoreCase("Data")) {
                    tagz = t;
                    break;
                }
            }
            if (tagz == null) {
                continue;
            }

            String[] stringtags = tagz.getValue().split(",");

            for (String tag : stringtags) {

                String deviceName;
                String volumeId;
                for (InstanceBlockDeviceMapping instanceBlockDeviceMapping : blocks) {

                    deviceName = instanceBlockDeviceMapping.getDeviceName();

                    if (!deviceName.equalsIgnoreCase(tag))  {
                        continue;
                    }

                    volumeId = instanceBlockDeviceMapping.getEbs().getVolumeId();

                    HashMap<String, HashMap<String, String>> setMember = new HashMap();
                    HashMap<String, String> internalHash = new HashMap();

                    if (volumeId == null | volumeId == null) {
                        continue;
                    }

                    internalHash.put(deviceName, volumeId);
                    setMember.put(instanceId, internalHash);

                    instanceSet.add(setMember);
                }
            }
        }
        if (instanceSet.isEmpty()) {
            return volumesWithNoTags;
        }

        //Need to validate
        instanceSet.removeAll(volumeSet);
        if (instanceSet.isEmpty()) {
            return volumesWithNoTags;
        }

        //Instance keys, to get volumeIds
        Set<String> volumeIds = new HashSet();
        for (HashMap<String, HashMap<String, String>> i : instanceSet) {
            Set<String> curSet = i.keySet();
            for (String s : curSet) {
                Set<String> dvns = i.get(s).keySet();
                for (String dvn : dvns) {
                    if (!volumeIds.contains(dvn)) {
                        volumeIds.add(i.get(s).get(dvn));
                    }
                }
            }
        }

        for (String i : volumeIds) {

            DescribeVolumesRequest describeVolumesRequest
                    = new DescribeVolumesRequest().withVolumeIds(i);
            DescribeVolumesResult describeVolumeResult
                    = EC2ClientMethods.describeVolumes(ec2Client,
                            describeVolumesRequest,
                            numRetries_,
                            maxApiRequestsPerSecond_,
                            uniqueAwsAccountIdentifier_);

            Volume volume = null;
            try {
                volume = describeVolumeResult.getVolumes().get(0);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event\"Error\", Error=\"volume id does not exist\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }

            volumesWithNoTags.add(volume);
        }

        return volumesWithNoTags;
    }

    private List<Volume> getEideticVolumes(AmazonEC2Client ec2Client) {
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
        return describeVolumeResult.getVolumes();
    }

    private void addTagToVolumes(AmazonEC2Client ec2Client, List<Volume> volumesWithNoEidetic) {
        for (Volume volume : volumesWithNoEidetic) {
            Collection<Tag> tags_volume = getResourceTags(volume);

            Tag tag = new Tag("Eidetic", defaultEideticTagValue_);
            tags_volume.add(tag);

            try {
                setResourceTags(ec2Client, volume, tags_volume, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event\"Error\", Error=\"error adding tags to volume\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
    }
}
