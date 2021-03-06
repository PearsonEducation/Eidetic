/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.Tag;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Judah Walker
 */
public interface RDSSubThread {
        public boolean isFinished();
    
    List<DBSnapshot> getAllDBSnapshotsOfDBInstance(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier); 
    
    List<DBClusterSnapshot> getAllDBClusterSnapshotsOfDBCluster(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    Collection<Tag> getResourceTags(Region region, AmazonRDSClient rdsClient, String arn, Integer numRetries, 
            Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    void setResourceTags(Region region, AmazonRDSClient rdsClient, String arn, Collection<Tag> tags, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    void sortDBSnapshotsByDate(List<DBSnapshot> comparelist);
    
    void sortDBClusterSnapshotsByDate(List<DBClusterSnapshot> comparelist);
    
    int getHoursBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList);
    
    int getDaysBetweenNowAndNewestDBSnapshot(List<DBSnapshot> sortedCompareList);
    
    int getHoursBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList);
    
    int getDaysBetweenNowAndNewestDBClusterSnapshot(List<DBClusterSnapshot> sortedCompareList);
    
    DBSnapshot createDBSnapshotOfDBInstance(Region region, AmazonRDSClient rdsClient, DBInstance dbInstance, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    
    DBClusterSnapshot createDBClusterSnapshotOfDBCluster(Region region, AmazonRDSClient rdsClient, DBCluster dbCluster, 
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier);
    

}
