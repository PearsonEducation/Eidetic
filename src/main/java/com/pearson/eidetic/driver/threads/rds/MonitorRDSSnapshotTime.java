/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.rds;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.Protocol;
import com.amazonaws.regions.Region;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AddTagsToResourceRequest;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.Tag;
import com.google.common.collect.Lists;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.aws.RDSClientMethods;
import com.pearson.eidetic.driver.MonitorRDS;
import com.pearson.eidetic.driver.MonitorRDSMethods;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterTime;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBInstanceTime;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Judah Walker
 */
public class MonitorRDSSnapshotTime extends MonitorRDSMethods implements Runnable, MonitorRDS {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;

    private Integer today_;

    private final HashMap<Region, HashSet<Date>> didMyDBInstanceSnapshotDay_ = new HashMap<>();
    private final HashMap<Region, HashSet<Date>> didMyDBClusterSnapshotDay_ = new HashMap<>();
    private final HashMap<Region, Integer> splitFactorDBInstanceDay_ = new HashMap<>();
    private final HashMap<Region, Integer> splitFactorDBClusterDay_ = new HashMap<>();

    private final HashMap<Region, HashMap<Date, ArrayList<DBInstance>>> timeDayDBInstance_ = new HashMap<>();
    private final HashMap<Region, HashMap<Date, ArrayList<DBCluster>>> timeDayDBCluster_ = new HashMap<>();

    private final HashMap<Region, ArrayList<ArrayList<DBInstance>>> localDBInstanceTimeListDay_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<DBCluster>>> localDBClusterTimeListDay_ = new HashMap<>();

    private final HashMap<Region, ArrayList<RDSSnapshotDBInstanceTime>> DBInstanceSubThreads_ = new HashMap<>();
    private final HashMap<Region, ArrayList<RDSSnapshotDBClusterTime>> DBClusterSubThreads_ = new HashMap<>();

    private final HashMap<Region, HashMap<DBInstance, List<Tag>>> DBInstanceTags_;
    private final HashMap<Region, HashMap<DBSnapshot, Collection<Tag>>> DBSnapshotTags_;
    private final HashMap<Region, HashMap<DBCluster, List<Tag>>> DBClusterTags_;
    private final HashMap<Region, HashMap<DBClusterSnapshot, Collection<Tag>>> DBClusterSnapshotTags_;

    private final DateFormat dayFormat_ = new SimpleDateFormat("HH:mm:ss");
    private final String uniqueAwsAccountIdentifier_;

    private int EideticSubThreads_;

    public MonitorRDSSnapshotTime(AwsAccount awsAccount) {
        if (awsAccount == null) {
            throw new IllegalArgumentException("null is not valid for AWSAccount");
        }

        this.awsAccount_ = awsAccount;
        this.uniqueAwsAccountIdentifier_ = awsAccount_.getUniqueAwsAccountIdentifier();
        this.DBInstanceTags_ = new HashMap();
        this.DBSnapshotTags_ = new HashMap();
        this.DBClusterTags_ = new HashMap();
        this.DBClusterSnapshotTags_ = new HashMap();
    }

    @Override
    public void run() {
        Calendar calendar_int = Calendar.getInstance();

        //0-365
        today_ = calendar_int.get(Calendar.DAY_OF_YEAR);

        ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime;
        ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime;
        localDBInstanceTime = awsAccount_.getDBInstanceTime_Copy();

        for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
            Region region = entry.getKey();
            splitFactorDBInstanceDay_.put(region, 10);
            splitFactorDBClusterDay_.put(region, 10);
            HashSet<Date> newHashSet = new HashSet<>();
            didMyDBInstanceSnapshotDay_.put(entry.getKey(), newHashSet);
            didMyDBClusterSnapshotDay_.put(entry.getKey(), newHashSet);
        }

        localDBClusterTime = awsAccount_.getDBClusterTime_Copy();

        for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
            Region region = entry.getKey();
            DBInstanceTags_.put(region, new HashMap());
            DBSnapshotTags_.put(region, new HashMap());
            DBClusterTags_.put(region, new HashMap());
            DBClusterSnapshotTags_.put(region, new HashMap());
            if (!splitFactorDBInstanceDay_.containsKey(region)) {
                splitFactorDBInstanceDay_.put(region, 10);
            }
            if (!splitFactorDBClusterDay_.containsKey(region)) {
                splitFactorDBClusterDay_.put(region, 10);
            }
            if (!didMyDBInstanceSnapshotDay_.containsKey(entry.getKey())) {
                HashSet<Date> newHashSet = new HashSet<>();
                didMyDBInstanceSnapshotDay_.put(entry.getKey(), newHashSet);
            }

        }

        addAlreadyDoneTodayDBSnapshots(localDBInstanceTime);
        addAlreadyDoneTodayDBClusterSnapshots(localDBClusterTime);

        while (true) {
            try {
                //Reset my stuff
                if (isItTomorrow(today_)) {
                    calendar_int = Calendar.getInstance();

                    today_ = calendar_int.get(Calendar.DAY_OF_YEAR);
                    resetDidMyDBInstanceSnapshotDay();
                    resetDidMyDBClusterSnapshotDay();

                }

                //Populate localDBInstanceTime
                localDBInstanceTime = awsAccount_.getDBInstanceTime_Copy();
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
                    Region region = entry.getKey();

                    if (!DBInstanceTags_.containsKey(region)) {
                        DBInstanceTags_.put(region, new HashMap());
                    }

                    if (!DBSnapshotTags_.containsKey(region)) {
                        DBSnapshotTags_.put(region, new HashMap());
                    }

                    if (localDBInstanceTime.get(region).isEmpty()) {
                        continue;
                    }
                    AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());
                    timeDayDBInstance_.put(region, extractRunAtFromDBInstances(rdsClient, localDBInstanceTime.get(region), region));
                    rdsClient.shutdown();
                }

                //Populate localDBClusterTime
                localDBClusterTime = awsAccount_.getDBClusterTime_Copy();
                for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
                    Region region = entry.getKey();

                    if (!DBClusterTags_.containsKey(region)) {
                        DBClusterTags_.put(region, new HashMap());
                    }

                    if (!DBClusterSnapshotTags_.containsKey(region)) {
                        DBClusterSnapshotTags_.put(region, new HashMap());
                    }

                    if (localDBClusterTime.get(region).isEmpty()) {
                        continue;
                    }
                    AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());
                    timeDayDBCluster_.put(region, extractRunAtFromDBClusters(rdsClient, localDBClusterTime.get(region), region));
                    rdsClient.shutdown();
                }

                //Populate dbinstance Threads
                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localDBInstanceTime.get(region).isEmpty()) {
                        continue;
                    }

                    timeDayDBInstance_.get(region).keySet().removeAll(didMyDBInstanceSnapshotDay_.get(region));
                    Calendar calendar = Calendar.getInstance();
                    Date now = calendar.getTime();
                    now = dayFormat_.parse(dayFormat_.format(now));

                    List<Date> lessThanNow = findLessThanNow(timeDayDBInstance_.get(region).keySet(), now);

                    if (!lessThanNow.isEmpty()) {
                        for (Date date : lessThanNow) {
                            ArrayList<DBInstance> instances = timeDayDBInstance_.get(region).get(date);
                            List<List<DBInstance>> listOfLists = Lists.partition(instances, splitFactorDBInstanceDay_.get(region));

                            if (localDBInstanceTimeListDay_.get(region) == null || localDBInstanceTimeListDay_.get(region).isEmpty()) {
                                localDBInstanceTimeListDay_.put(region, listsOfDBInstancesToArrayLists(listOfLists));
                            } else {
                                try {
                                    localDBInstanceTimeListDay_.get(region).add(listsOfDBInstancesToArrayLists(listOfLists).get(0));
                                } catch (Exception e) {
                                }
                            }

                            ArrayList<RDSSnapshotDBInstanceTime> threads = new ArrayList<>();

                            for (ArrayList<DBInstance> dbInstances : listsOfDBInstancesToArrayLists(listOfLists)) {
                                threads.add(new RDSSnapshotDBInstanceTime(
                                        awsAccount_.getAwsAccessKeyId(),
                                        awsAccount_.getAwsSecretKey(),
                                        awsAccount_.getAwsAccountId(),
                                        awsAccount_.getUniqueAwsAccountIdentifier(),
                                        awsAccount_.getMaxApiRequestsPerSecond(),
                                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                                        region,
                                        dbInstances));

                            }

                            didMyDBInstanceSnapshotDay_.get(region).add(date);

                            if (DBInstanceSubThreads_.containsKey(region) && !DBInstanceSubThreads_.get(region).isEmpty()) {
                                DBInstanceSubThreads_.get(region).addAll(threads);
                            } else {
                                DBInstanceSubThreads_.put(region, threads);
                            }

                            try {
                                for (Entry<Region, ArrayList<RDSSnapshotDBInstanceTime>> test : DBInstanceSubThreads_.entrySet()) {
                                    if (!test.getValue().isEmpty()) {
                                        for (RDSSnapshotDBInstanceTime test2 : test.getValue()) {
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                            }

                        }

                    }
                }

                //Populate dbcluster threads
                for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localDBClusterTime.get(region).isEmpty()) {
                        continue;
                    }

                    timeDayDBCluster_.get(region).keySet().removeAll(didMyDBClusterSnapshotDay_.get(region));
                    Calendar calendar = Calendar.getInstance();
                    Date now = calendar.getTime();
                    now = dayFormat_.parse(dayFormat_.format(now));

                    List<Date> lessThanNow = findLessThanNow(timeDayDBCluster_.get(region).keySet(), now);

                    if (!lessThanNow.isEmpty()) {
                        for (Date date : lessThanNow) {
                            ArrayList<DBCluster> clusters = timeDayDBCluster_.get(region).get(date);
                            List<List<DBCluster>> listOfLists = Lists.partition(clusters, splitFactorDBClusterDay_.get(region));

                            if (localDBClusterTimeListDay_.get(region) == null || localDBClusterTimeListDay_.get(region).isEmpty()) {
                                localDBClusterTimeListDay_.put(region, listsOfDBClustersToArrayLists(listOfLists));
                            } else {
                                try {
                                    localDBClusterTimeListDay_.get(region).add(listsOfDBClustersToArrayLists(listOfLists).get(0));
                                } catch (Exception e) {
                                }
                            }

                            ArrayList<RDSSnapshotDBClusterTime> threads = new ArrayList<>();

                            for (ArrayList<DBCluster> dbClusters : listsOfDBClustersToArrayLists(listOfLists)) {
                                threads.add(new RDSSnapshotDBClusterTime(
                                        awsAccount_.getAwsAccessKeyId(),
                                        awsAccount_.getAwsSecretKey(),
                                        awsAccount_.getAwsAccountId(),
                                        awsAccount_.getUniqueAwsAccountIdentifier(),
                                        awsAccount_.getMaxApiRequestsPerSecond(),
                                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                                        region,
                                        dbClusters));

                            }

                            didMyDBClusterSnapshotDay_.get(region).add(date);

                            if (DBClusterSubThreads_.containsKey(region) && !DBClusterSubThreads_.get(region).isEmpty()) {
                                DBClusterSubThreads_.get(region).addAll(threads);
                            } else {
                                DBClusterSubThreads_.put(region, threads);
                            }

                        }

                    }
                }

                //localDBInstanceTimeListDay now has hashmaps of regions with keys of arrays of arrays of objects to take snapshots of.
                HashMap<Region, Integer> secsSlept = new HashMap<>();
                HashMap<Region, Boolean> allDead = new HashMap<>();

                for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localDBInstanceTimeListDay_.get(region) == null || localDBInstanceTimeListDay_.get(region).isEmpty()) {
                        continue;
                    }

                    //Initializing content
                    secsSlept.put(region, 0);

                    //Initializing content
                    allDead.put(region, false);
                    try {
                        Threads.threadExecutorFixedPool(DBInstanceSubThreads_.get(region), splitFactorDBInstanceDay_.get(region), 300, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                }

                for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localDBClusterTimeListDay_.get(region) == null || localDBClusterTimeListDay_.get(region).isEmpty()) {
                        continue;
                    }

                    //Initializing content
                    secsSlept.put(region, 0);

                    //Initializing content
                    allDead.put(region, false);

                    Threads.threadExecutorFixedPool(DBClusterSubThreads_.get(region), splitFactorDBClusterDay_.get(region), 300, TimeUnit.MINUTES);
                }

                //LETS SEE IF THEY'RE DEAD
                Boolean ejection = false;
                Boolean theyreDead;

                while (true) {
                    for (Map.Entry<Region, Boolean> entry : allDead.entrySet()) {
                        Region region = entry.getKey();

                        if ((DBInstanceSubThreads_.containsKey(region) && areAllThreadsDead(DBInstanceSubThreads_.get(region))) && (DBClusterSubThreads_.containsKey(region) && areAllThreadsDead(DBClusterSubThreads_.get(region)))
                                || (!DBInstanceSubThreads_.containsKey(region) && DBClusterSubThreads_.containsKey(region) && areAllThreadsDead(DBClusterSubThreads_.get(region)))
                                || (!DBClusterSubThreads_.containsKey(region) && DBInstanceSubThreads_.containsKey(region) && areAllThreadsDead(DBInstanceSubThreads_.get(region)))
                                || (!DBClusterSubThreads_.containsKey(region) && !DBInstanceSubThreads_.containsKey(region))) {
                            allDead.put(region, true);
                        } else {
                            secsSlept.replace(region, secsSlept.get(region), secsSlept.get(region) + 1);
                            if (secsSlept.get(region) > 1800) {
                                splitFactorDBInstanceDay_.replace(region, splitFactorDBInstanceDay_.get(region), splitFactorDBInstanceDay_.get(region) + 1);
                                splitFactorDBClusterDay_.replace(region, splitFactorDBClusterDay_.get(region), splitFactorDBClusterDay_.get(region) + 1);

                                ejection = true;
                                break;
                            }

                        }

                    }

                    //I dont like this
                    theyreDead = true;
                    for (Map.Entry<Region, Boolean> entry : allDead.entrySet()) {
                        Region region = entry.getKey();

                        //If any of them have false
                        if (!allDead.get(region)) {
                            theyreDead = false;
                        }
                    }

                    if (ejection || theyreDead) {
                        break;
                    }

                    Threads.sleepSeconds(1);
                }

                //See if decrease splitfactor
                for (Map.Entry<Region, Boolean> entry : allDead.entrySet()) {
                    Region region = entry.getKey();

                    int timeRemaining = 1800 - secsSlept.get(region);

                    if ((splitFactorDBInstanceDay_.get(region) > 5) & (timeRemaining > 60)) {
                        splitFactorDBInstanceDay_.replace(region, splitFactorDBInstanceDay_.get(region), splitFactorDBInstanceDay_.get(region) - 1);
                    }

                    if ((splitFactorDBClusterDay_.get(region) > 5) & (timeRemaining > 60)) {
                        splitFactorDBClusterDay_.replace(region, splitFactorDBClusterDay_.get(region), splitFactorDBClusterDay_.get(region) - 1);
                    }
                }
                DBInstanceSubThreads_.clear();
                DBClusterSubThreads_.clear();
                DBInstanceSubThreads_.clear();
                DBClusterSubThreads_.clear();

                DBInstanceTags_.clear();
                DBSnapshotTags_.clear();
                DBClusterTags_.clear();
                DBClusterSnapshotTags_.clear();

                Threads.sleepSeconds(30);

            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Error=\"MonitorRDSSnapshotTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

    }

    private boolean isItTomorrow(Integer today) {
        Calendar calendarTest = Calendar.getInstance();
        return today != (calendarTest.get(Calendar.DAY_OF_YEAR));
    }

    private HashMap<Date, ArrayList<DBInstance>> extractRunAtFromDBInstances(AmazonRDSClient rdsClient, ArrayList<DBInstance> dbInstances, Region region) {
        JSONParser parser = new JSONParser();
        HashMap<Date, ArrayList<DBInstance>> returnHash = new HashMap();
        for (DBInstance dbInstance : dbInstances) {
            List<Tag> tags;
            if (!DBInstanceTags_.containsKey(region)) {
                DBInstanceTags_.put(region, new HashMap());
            }
            if (DBInstanceTags_.get(region).containsKey(dbInstance)) {
                tags = DBInstanceTags_.get(region).get(dbInstance);
            } else {
                String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccount_.getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
                ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                tags = RDSClientMethods.getTags(rdsClient,
                        listTagsForResourceRequest,
                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                        awsAccount_.getMaxApiRequestsPerSecond(),
                        awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();
                DBInstanceTags_.get(region).put(dbInstance, tags);
            }
            if (tags != null) {
                for (Tag tag : tags) {
                    String runAt = null;
                    if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                        runAt = tag.getValue();
                    }

                    if (runAt == null) {
                        continue;
                    }

                    Date date = null;
                    try {
                        date = dayFormat_.parse(runAt);
                    } catch (ParseException e) {
                        logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                    if (date == null) {
                        continue;
                    }

                    if (returnHash.keySet().contains(date)) {
                        returnHash.get(date).add(dbInstance);
                    } else {
                        ArrayList<DBInstance> newArrayList = new ArrayList();
                        newArrayList.add(dbInstance);
                        returnHash.put(date, newArrayList);
                    }
                    break;
                }
            }
        }
        return returnHash;
    }

    private HashMap<Date, ArrayList<DBCluster>> extractRunAtFromDBClusters(AmazonRDSClient rdsClient, ArrayList<DBCluster> dbClusters, Region region) {
        HashMap<Date, ArrayList<DBCluster>> returnHash = new HashMap();
        for (DBCluster dbCluster : dbClusters) {
            List<Tag> tags;
            if (!DBClusterTags_.containsKey(region)) {
                DBClusterTags_.put(region, new HashMap());
            }
            if (DBClusterTags_.get(region).containsKey(dbCluster)) {
                tags = DBClusterTags_.get(region).get(dbCluster);
            } else {
                String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region.getName(), awsAccount_.getAwsAccountId(), dbCluster.getDBClusterIdentifier());
                ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
                tags = RDSClientMethods.getTags(rdsClient,
                        listTagsForResourceRequest,
                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                        awsAccount_.getMaxApiRequestsPerSecond(),
                        awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();
                DBClusterTags_.get(region).put(dbCluster, tags);
            }
            if (tags != null) {
                for (Tag tag : tags) {
                    String runAt = null;
                    if (tag.getKey().equalsIgnoreCase("Eidetic_RunAt")) {
                        runAt = tag.getValue();
                    }

                    if (runAt == null) {
                        continue;
                    }

                    Date date = null;
                    try {
                        date = dayFormat_.parse(runAt);
                    } catch (ParseException e) {
                        logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                    if (date == null) {
                        continue;
                    }

                    if (returnHash.keySet().contains(date)) {
                        returnHash.get(date).add(dbCluster);
                    } else {
                        ArrayList<DBCluster> newArrayList = new ArrayList();
                        newArrayList.add(dbCluster);
                        returnHash.put(date, newArrayList);
                    }
                    break;
                }
            }
        }
        return returnHash;
    }

    private List<Date> findLessThanNow(Set<Date> keySet, Date now) {
        List<Date> min = new ArrayList();
        for (Date date : keySet) {

            if (date.before(now)) {
                min.add(date);
            }
        }
        return min;
    }

    private void resetDidMyDBInstanceSnapshotDay() {
        didMyDBInstanceSnapshotDay_.clear();

        for (Map.Entry<Region, Integer> entry : splitFactorDBInstanceDay_.entrySet()) {
            HashSet<Date> newHashSet = new HashSet<>();
            didMyDBInstanceSnapshotDay_.put(entry.getKey(), newHashSet);
        }

    }

    private void resetDidMyDBClusterSnapshotDay() {
        didMyDBClusterSnapshotDay_.clear();

        for (Map.Entry<Region, Integer> entry : splitFactorDBClusterDay_.entrySet()) {
            HashSet<Date> newHashSet = new HashSet<>();
            didMyDBClusterSnapshotDay_.put(entry.getKey(), newHashSet);
        }

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

    private void addAlreadyDoneTodayDBSnapshots(ConcurrentHashMap<Region, ArrayList<DBInstance>> localDBInstanceTime) {
        HashMap<Region, HashMap<Date, ArrayList<DBInstance>>> timeDay = new HashMap<>();

        for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
            Region region = entry.getKey();

            if (localDBInstanceTime.get(region).isEmpty()) {
                continue;
            }
            AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());
            timeDay.put(region, extractRunAtFromDBInstances(rdsClient, localDBInstanceTime.get(region), region));
            rdsClient.shutdown();
        }

        for (Map.Entry<Region, ArrayList<DBInstance>> entry : localDBInstanceTime.entrySet()) {
            Region region = entry.getKey();

            if (localDBInstanceTime.get(region).isEmpty()) {
                continue;
            }

            AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

            for (DBInstance dbInstance : localDBInstanceTime.get(region)) {
                Date date = new java.util.Date();

                JSONObject eideticParameters = getIntTagValue(rdsClient, dbInstance, region);
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
                success = snapshotDecision(rdsClient, dbInstance, period, region);
                //Success true means we need to take a snapshot, which it can handle l8r
                //Success false means we need to add to hashset
                if (!success) {
                    String runAt = getRunAt(eideticParameters, dbInstance);
                    try {
                        date = dayFormat_.parse(runAt);
                    } catch (ParseException e) {
                        logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", DBInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                    if (date == null) {
                        continue;
                    }
                    didMyDBInstanceSnapshotDay_.get(region).add(date);
                }
            }

            rdsClient.shutdown();

        }

    }

    private void addAlreadyDoneTodayDBClusterSnapshots(ConcurrentHashMap<Region, ArrayList<DBCluster>> localDBClusterTime) {
        HashMap<Region, HashMap<Date, ArrayList<DBCluster>>> timeDay = new HashMap<>();

        for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
            Region region = entry.getKey();

            if (localDBClusterTime.get(region).isEmpty()) {
                continue;
            }
            AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());
            timeDay.put(region, extractRunAtFromDBClusters(rdsClient, localDBClusterTime.get(region), region));
            rdsClient.shutdown();
        }

        for (Map.Entry<Region, ArrayList<DBCluster>> entry : localDBClusterTime.entrySet()) {
            Region region = entry.getKey();

            if (localDBClusterTime.get(region).isEmpty()) {
                continue;
            }

            AmazonRDSClient rdsClient = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

            for (DBCluster dbCluster : localDBClusterTime.get(region)) {
                Date date = new java.util.Date();

                JSONObject eideticParameters = getIntTagValue(rdsClient, dbCluster, region);
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
                success = snapshotDecision(rdsClient, dbCluster, period, region);
                //Success true means we need to take a snapshot, which it can handle l8r
                //Success false means we need to add to hashset
                if (!success) {
                    String runAt = getRunAt(eideticParameters, dbCluster);
                    try {
                        date = dayFormat_.parse(runAt);
                    } catch (ParseException e) {
                        logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                    if (date == null) {
                        continue;
                    }
                    didMyDBClusterSnapshotDay_.get(region).add(date);
                }
            }

            rdsClient.shutdown();

        }

    }

    public JSONObject getIntTagValue(AmazonRDSClient amazonRDSClient, DBInstance dbInstance, Region region) {
        if ((amazonRDSClient == null) || (dbInstance == null) || (region == null)) {
            return null;
        }

        JSONObject eideticParameters = new JSONObject();
        JSONObject createSnapshot = new JSONObject();
        String arn = String.format("arn:aws:rds:%s:%s:db:%s", region.getName(), awsAccount_.getAwsAccountId(), dbInstance.getDBInstanceIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        List<Tag> tags = RDSClientMethods.getTags(amazonRDSClient,
                listTagsForResourceRequest,
                ApplicationConfiguration.getAwsCallRetryAttempts(),
                awsAccount_.getMaxApiRequestsPerSecond(),
                awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();

        for (Tag tag : tags) {
            if ("Eidetic_Interval".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Interval", tag.getValue());
            } else if ("Eidetic_RunAt".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("RunAt", tag.getValue());
            } else if ("Eidetic_Retain".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Retain", tag.getValue());
            }
        }

        DBInstanceTags_.get(region).put(dbInstance, tags);

        eideticParameters.put("CreateSnapshot", createSnapshot);

        return eideticParameters;

    }

    public JSONObject getIntTagValue(AmazonRDSClient amazonRDSClient, DBCluster dbCluster, Region region) {
        if ((amazonRDSClient == null) || (dbCluster == null) || (region == null)) {
            return null;
        }

        JSONObject eideticParameters = new JSONObject();
        JSONObject createSnapshot = new JSONObject();
        String arn = String.format("arn:aws:rds:%s:%s:cluster:%s", region.getName(), awsAccount_.getAwsAccountId(), dbCluster.getDBClusterIdentifier());
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        List<Tag> tags = RDSClientMethods.getTags(amazonRDSClient,
                listTagsForResourceRequest,
                ApplicationConfiguration.getAwsCallRetryAttempts(),
                awsAccount_.getMaxApiRequestsPerSecond(),
                awsAccount_.getUniqueAwsAccountIdentifier()).getTagList();

        for (Tag tag : tags) {
            if ("Eidetic_Interval".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Interval", tag.getValue());
            } else if ("Eidetic_RunAt".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("RunAt", tag.getValue());
            } else if ("Eidetic_Retain".equalsIgnoreCase(tag.getKey())) {
                createSnapshot.put("Retain", tag.getValue());
            }
        }

        DBClusterTags_.get(region).put(dbCluster, tags);

        eideticParameters.put("CreateSnapshot", createSnapshot);

        return eideticParameters;

    }

    public String getPeriod(JSONObject eideticParameters, DBInstance dbInstance) {
        if ((eideticParameters == null) || (dbInstance == null)) {
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

    public String getPeriod(JSONObject eideticParameters, DBCluster dbCluster) {
        if ((eideticParameters == null) || (dbCluster == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\"");
            return null;
        }

        String period = null;
        if (createSnapshot.containsKey("Interval")) {
            try {
                period = createSnapshot.get("Interval").toString();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
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

    public Integer getKeep(JSONObject eideticParameters, DBCluster dbCluster) {
        if ((eideticParameters == null) | (dbCluster == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\"");
            return null;
        }

        Integer keep = null;
        if (createSnapshot.containsKey("Retain")) {
            try {
                keep = Integer.parseInt(createSnapshot.get("Retain").toString());
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_Identifier=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return keep;
    }

    public String getRunAt(JSONObject eideticParameters, DBInstance dbInstance) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\"");
            return null;
        }

        String runAt = null;
        if (createSnapshot.containsKey("RunAt")) {
            try {
                runAt = createSnapshot.get("RunAt").toString();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbInstance_id=\"" + dbInstance.getDBInstanceIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return runAt;
    }

    public String getRunAt(JSONObject eideticParameters, DBCluster dbCluster) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\"");
            return null;
        }

        String runAt = null;
        if (createSnapshot.containsKey("RunAt")) {
            try {
                runAt = createSnapshot.get("RunAt").toString();
            } catch (Exception e) {
                logger.error("awsAccountNickname=\"" + awsAccount_.getUniqueAwsAccountIdentifier() + "\",Event=Error, Error=\"Malformed Eidetic Tag\", dbCluster_id=\"" + dbCluster.getDBClusterIdentifier() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return runAt;
    }

    public boolean snapshotDecision(AmazonRDSClient rdsClient, DBInstance dbInstance, String period, Region region) {
        if ((rdsClient == null) || (dbInstance == null) || (period == null)) {
            return false;
        }
        try {

            List<DBSnapshot> int_snapshots = getAllDBSnapshotsOfDBInstance(rdsClient, dbInstance, ApplicationConfiguration.getAwsCallRetryAttempts(),
                    awsAccount_.getMaxApiRequestsPerSecond(),
                    awsAccount_.getUniqueAwsAccountIdentifier());

            List<DBSnapshot> comparelist = new ArrayList();

            for (DBSnapshot snapshot : int_snapshots) {
                if (!snapshot.getDBSnapshotIdentifier().startsWith("eidetic")) {
                    continue;
                }

                //getResourceTags(AmazonRDSClient rdsClient, String arn, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier)
                String arn = String.format("arn:aws:rds:%s:%s:snapshot:%s", region.getName(), awsAccount_.getAwsAccountId(), snapshot.getDBSnapshotIdentifier());
                Collection<Tag> tags_dbSnapshot = getResourceTags(rdsClient, arn, ApplicationConfiguration.getAwsCallRetryAttempts(),
                        awsAccount_.getMaxApiRequestsPerSecond(),
                        awsAccount_.getUniqueAwsAccountIdentifier());

                if (!DBSnapshotTags_.containsKey(region)) {
                    DBSnapshotTags_.put(region, new HashMap());
                }
                DBSnapshotTags_.get(region).put(snapshot, tags_dbSnapshot);
                String sndesc = null;
                for (Tag tag : tags_dbSnapshot) {
                    if ("description".equalsIgnoreCase(tag.getKey())) {
                        sndesc = tag.getValue();
                        break;
                    }
                }
                if (sndesc == null) {
                    //logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbsnapshot has not description tag, skipping\",\"DBSnapshot_Identifier=\""
                    // + snapshot.getDBSnapshotIdentifier() + "\"");
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

    public boolean snapshotDecision(AmazonRDSClient rdsClient, DBCluster dbCluster, String period, Region region) {
        if ((rdsClient == null) || (dbCluster == null) || (period == null)) {
            return false;
        }
        try {

            List<DBClusterSnapshot> int_snapshots = getAllDBClusterSnapshotsOfDBCluster(rdsClient, dbCluster, ApplicationConfiguration.getAwsCallRetryAttempts(),
                    awsAccount_.getMaxApiRequestsPerSecond(),
                    awsAccount_.getUniqueAwsAccountIdentifier());

            List<DBClusterSnapshot> comparelist = new ArrayList();

            for (DBClusterSnapshot snapshot : int_snapshots) {
                //getResourceTags(AmazonRDSClient rdsClient, String arn, Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier)
                if (!snapshot.getDBClusterSnapshotIdentifier().startsWith("eidetic")) {
                    continue;
                }

                String arn = String.format("arn:aws:rds:%s:%s:cluster-snapshot:%s", region.getName(), awsAccount_.getAwsAccountId(), snapshot.getDBClusterSnapshotIdentifier());
                Collection<Tag> tags_dbClusterSnapshot = getResourceTags(rdsClient, arn, ApplicationConfiguration.getAwsCallRetryAttempts(),
                        awsAccount_.getMaxApiRequestsPerSecond(),
                        awsAccount_.getUniqueAwsAccountIdentifier());
                DBClusterSnapshotTags_.get(region).put(snapshot, tags_dbClusterSnapshot);
                String sndesc = null;
                for (Tag tag : tags_dbClusterSnapshot) {
                    if ("description".equalsIgnoreCase(tag.getKey())) {
                        sndesc = tag.getValue();
                        break;
                    }
                }
                if (sndesc == null) {
                    logger.info("awsAccountNickname=\"" + uniqueAwsAccountIdentifier_ + "\",Event=\"Error\", Error=\"error in snapshotDecision, eidetic dbclustersnapshot has not description tag\",\"DBClusterSnapshot_Identifier=\""
                            + snapshot.getDBClusterSnapshotIdentifier() + "\"");
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

    public Collection<Tag> getResourceTags(AmazonRDSClient rdsClient, String arn, Integer numRetries,
            Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        ListTagsForResourceRequest listTagsForResourceRequest = new ListTagsForResourceRequest().withResourceName(arn);
        return RDSClientMethods.getTags(rdsClient,
                listTagsForResourceRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier).getTagList();
    }

    public void setResourceTags(AmazonRDSClient rdsClient, String arn, Collection<Tag> tags,
            Integer numRetries, Integer maxApiRequestsPerSecond, String uniqueAwsAccountIdentifier) {
        AddTagsToResourceRequest addTagsToResourceRequest = new AddTagsToResourceRequest().withResourceName(arn);
        addTagsToResourceRequest.setTags(tags);
        RDSClientMethods.createTags(rdsClient,
                addTagsToResourceRequest,
                numRetries,
                maxApiRequestsPerSecond,
                uniqueAwsAccountIdentifier);
    }

}
