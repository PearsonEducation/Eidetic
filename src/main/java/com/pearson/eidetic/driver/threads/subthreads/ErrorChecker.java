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
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.EC2ClientMethods;
import com.pearson.eidetic.driver.threads.EideticSubThread;
import com.pearson.eidetic.driver.threads.EideticSubThreadMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class ErrorChecker extends EideticSubThreadMethods implements Runnable, EideticSubThread {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private Boolean isFinished_ = null;
    private AwsAccount awsAccount_ = null;
    private final String uniqueAwsAccountIdentifier_;
    private final Integer maxApiRequestsPerSecond_;
    private final Integer numRetries_;

    public ErrorChecker(AwsAccount awsAccount, String uniqueAwsAccountIdentifier, Integer maxApiRequestsPerSecond, Integer numRetries) {
        this.awsAccount_ = awsAccount;
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

        for (Map.Entry<com.amazonaws.regions.Region, ArrayList<Volume>> entry : awsAccount_.getVolumeNoTime_Copy().entrySet()) {
            try {
                //Get all pending!
                com.amazonaws.regions.Region region = entry.getKey();
                AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

                List<Snapshot> error_snapshots = getAllErrorSnapshots(ec2Client);
                if (error_snapshots.isEmpty()) {
                    continue;
                }

                loggerOuputSnapshotErrors(error_snapshots, region);

                deleteSnapshotErrors(ec2Client, error_snapshots);
                
                ec2Client.shutdown();

            } catch (Exception e) {
                logger.error("Event=\"Error\", Error=\"error in ErrorChecker workflow\", stacktrace=\""
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

    private List<Snapshot> getAllErrorSnapshots(AmazonEC2Client ec2Client) {
        Filter[] filters = new Filter[1];
        filters[0] = new Filter().withName("status").withValues("error");

        DescribeSnapshotsRequest describeSnapshotRequest
                = new DescribeSnapshotsRequest().withOwnerIds("self").withFilters(filters);
        DescribeSnapshotsResult describeSnapshotsResult
                = EC2ClientMethods.describeSnapshots(ec2Client,
                        describeSnapshotRequest,
                        numRetries_,
                        maxApiRequestsPerSecond_,
                        uniqueAwsAccountIdentifier_);

        return describeSnapshotsResult.getSnapshots();
    }

    private void loggerOuputSnapshotErrors(List<Snapshot> error_snapshots, Region region) {
        for (Snapshot snapshot : error_snapshots) {
            logger.info("Event=\"SnapshotInErrorState\", region=\"" + region.toString() + ", snapshot_id=\"" + snapshot.getSnapshotId() + "\"");
        }
    }

    private void deleteSnapshotErrors(AmazonEC2Client ec2Client, List<Snapshot> error_snapshots) {
        for (Snapshot snapshot : error_snapshots) {
            try {
                deleteSnapshot(ec2Client, snapshot, numRetries_, maxApiRequestsPerSecond_, uniqueAwsAccountIdentifier_);
            } catch (Exception e) {
                logger.error("Event=\"Error\", Error=\"error deleting snapshot\", Snapshot_id=\"" + snapshot.getSnapshotId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }
    }

}
