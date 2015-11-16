/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author uwalkj6
 */
public interface EideticSubThread {
    
    public boolean isFinished();
    
    List<Snapshot> getAllSnapshotsOfVolume(AmazonEC2Client ec2Client, Volume vol, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier); 
    
    Collection<Tag> getResourceTags(Volume vol);
    
    Collection<Tag> getResourceTags(Instance instance);
    
    Collection<Tag> getResourceTags(Snapshot snapshot);
    
    void setResourceTags(AmazonEC2Client ec2Client, Volume vol, Collection<Tag> tags, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    void setResourceTags(AmazonEC2Client ec2Client, Instance instance, Collection<Tag> tags, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    void setResourceTags(AmazonEC2Client ec2Client, Snapshot snapshot, Collection<Tag> tags, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    void sortSnapshotsByDate(List<Snapshot> comparelist);
    
    int getHoursBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList);
    
    int getDaysBetweenNowAndNewestSnapshot(List<Snapshot> sortedCompareList);
    
    Snapshot createSnapshotOfVolume(AmazonEC2Client ec2Client, Volume vol, String description, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    Snapshot createSnapshotOfVolume(AmazonEC2Client ec2Client, Volume vol, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    

}
