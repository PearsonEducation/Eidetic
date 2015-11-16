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
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.google.common.collect.Lists;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeTime;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.Monitor;
import com.pearson.eidetic.driver.MonitorMethods;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uwalkj6
 */
public class MonitorSnapshotVolumeTime extends MonitorMethods implements Runnable, Monitor {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    private final AwsAccount awsAccount_;

    private Integer today_;

    //private HashMap<Region,Set<Volume>> didMySnapshotHour_;
    private final HashMap<Region, HashSet<Date>> didMySnapshotDay_ = new HashMap<>();
    private final HashMap<Region, Integer> splitFactorDay_ = new HashMap<>();
    private final HashMap<Region, HashMap<Date, ArrayList<Volume>>> timeDay_ = new HashMap<>();
    private final HashMap<Region, ArrayList<ArrayList<Volume>>> localVolumeTimeListDay_ = new HashMap<>();
    private final HashMap<Region, ArrayList<SnapshotVolumeTime>> EideticSubThreads_ = new HashMap<>();
    //private HashMap<Region,Set<Volume>> didMySnapshotWeek_;
    //private HashMap<Region,Set<Volume>> didMySnapshotMonth_;

    private final DateFormat dayFormat_ = new SimpleDateFormat("HH:mm:ss");

    public MonitorSnapshotVolumeTime(AwsAccount awsAccount) {
        this.awsAccount_ = awsAccount;
    }

    @Override
    public void run() {
        Calendar calendar_int = Calendar.getInstance();

        //0-365
        today_ = calendar_int.get(Calendar.DAY_OF_YEAR);


        ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime;

        localVolumeTime = awsAccount_.getVolumeTime_Copy();

        for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
            Region region = entry.getKey();
            splitFactorDay_.put(region, 10);
            HashSet<Date> newHashSet = new HashSet<>();
            didMySnapshotDay_.put(entry.getKey(), newHashSet);
        }

        addAlreadyDoneTodaySnapshots(localVolumeTime);

        while (true) {
            try {
                //Reset my stuff
                if (isItTomorrow(today_)) {
                    calendar_int = Calendar.getInstance();

                    today_ = calendar_int.get(Calendar.DAY_OF_YEAR);
                    resetDidMySnapshotDay();

                }

                localVolumeTime = awsAccount_.getVolumeTime_Copy();
                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localVolumeTime.get(region).isEmpty()) {
                        continue;
                    }

                    timeDay_.put(region, extractRunAt(localVolumeTime.get(region)));

                }

                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localVolumeTime.get(region).isEmpty()) {
                        continue;
                    }

                    timeDay_.get(region).keySet().removeAll(didMySnapshotDay_.get(region));
                    Calendar calendar = Calendar.getInstance();
                    Date now = calendar.getTime();
                    now = dayFormat_.parse(dayFormat_.format(now));

                    List<Date> lessThanNow = findLessThanNow(timeDay_.get(region).keySet(), now);

                    if (!lessThanNow.isEmpty()) {
                        for (Date date : lessThanNow) {
                            ArrayList<Volume> volumes = timeDay_.get(region).get(date);
                            List<List<Volume>> listOfLists = Lists.partition(volumes, splitFactorDay_.get(region));

                            if (localVolumeTimeListDay_.get(region) == null || localVolumeTimeListDay_.get(region).isEmpty()) {
                                localVolumeTimeListDay_.put(region, listsToArrayLists(listOfLists));
                            } else {
                                try {
                                    localVolumeTimeListDay_.get(region).add(listsToArrayLists(listOfLists).get(0));
                                } catch (Exception e) {
                                }
                            }

                            ArrayList<SnapshotVolumeTime> threads = new ArrayList<>();

                            for (ArrayList<Volume> vols : listsToArrayLists(listOfLists)) {
                                threads.add(new SnapshotVolumeTime(
                                        awsAccount_.getAwsAccessKeyId(),
                                        awsAccount_.getAwsSecretKey(),
                                        awsAccount_.getUniqueAwsAccountIdentifier(),
                                        awsAccount_.getMaxApiRequestsPerSecond(),
                                        ApplicationConfiguration.getAwsCallRetryAttempts(),
                                        region,
                                        vols));

                            }

                            didMySnapshotDay_.get(region).add(date);

                            EideticSubThreads_.put(region, threads);

                        }

                    }
                }
                //localVolumeTimeListDay now has hashmaps of regions with keys of arrays of arrays of volumes to take snapshots of.

                HashMap<Region, Integer> secsSlept = new HashMap<>();
                HashMap<Region, Boolean> allDead = new HashMap<>();

                for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
                    Region region = entry.getKey();

                    if (localVolumeTimeListDay_.get(region) == null || localVolumeTimeListDay_.get(region).isEmpty()) {
                        continue;
                    }

                    //Initializing content
                    secsSlept.put(region, 0);

                    //Initializing content
                    allDead.put(region, false);

                    Threads.threadExecutorFixedPool(EideticSubThreads_.get(region), splitFactorDay_.get(region), 300, TimeUnit.MINUTES);
                }

                //LETS SEE IF THEY'RE DEAD
                Boolean ejection = false;
                Boolean theyreDead;
                while (true) {
                    for (Map.Entry<Region, ArrayList<SnapshotVolumeTime>> entry : EideticSubThreads_.entrySet()) {
                        Region region = entry.getKey();

                        if (areAllThreadsDead(EideticSubThreads_.get(region))) {
                            allDead.put(region, true);
                        } else {
                            secsSlept.replace(region, secsSlept.get(region), secsSlept.get(region) + 1);
                            if (secsSlept.get(region) > 1800) {
                                splitFactorDay_.replace(region, splitFactorDay_.get(region), splitFactorDay_.get(region) + 1);
                                logger.info("Event=\"increasing_splitFactor\", Monitor=\"SnapshotVolumeTime\", splitFactor=\""
                                        + Integer.toString(splitFactorDay_.get(region)) + "\", VolumeTimeSize=\"" + Integer.toString(localVolumeTime.get(region).size()) + "\"");
                                ejection = true;
                                break;
                            }

                        }

                    }

                    //I dont like this
                    theyreDead = true;
                    for (Map.Entry<Region, ArrayList<SnapshotVolumeTime>> entry : EideticSubThreads_.entrySet()) {
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
                for (Map.Entry<Region, ArrayList<SnapshotVolumeTime>> entry : EideticSubThreads_.entrySet()) {
                    Region region = entry.getKey();

                    int timeRemaining = 1800 - secsSlept.get(region);

                    if ((splitFactorDay_.get(region) > 5) & (timeRemaining > 60)) {
                        splitFactorDay_.replace(region, splitFactorDay_.get(region), splitFactorDay_.get(region) - 1);
                        logger.info("Event=\"decreasing_splitFactor\", Monitor=\"SnapshotVolumeNoTime\", splitFactor=\""
                                + Integer.toString(splitFactorDay_.get(region)) + "\", VolumeNoTimeSize=\"" + Integer.toString(localVolumeTime.get(region).size()) + "\"");
                    }
                }

                localVolumeTimeListDay_.clear();
                EideticSubThreads_.clear();

                Threads.sleepSeconds(30);

            } catch (Exception e) {
                logger.error("Error=\"MonitorSnapshotVolumeTimeFailure\", stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

    }

    private boolean isItTomorrow(Integer today) {
        Calendar calendarTest = Calendar.getInstance();
        return today != (calendarTest.get(Calendar.DAY_OF_YEAR));
    }

    private HashMap<Date, ArrayList<Volume>> extractRunAt(ArrayList<Volume> volumes) {
        JSONParser parser = new JSONParser();
        HashMap<Date, ArrayList<Volume>> returnHash = new HashMap();
        for (Volume volume : volumes) {
            for (Tag tag : volume.getTags()) {
                String tagValue = null;
                if (tag.getKey().equalsIgnoreCase("Eidetic")) {
                    tagValue = tag.getValue();
                }
                if (tagValue == null) {
                    continue;
                }
                JSONObject eideticParameters;
                try {
                    Object obj = parser.parse(tagValue);
                    eideticParameters = (JSONObject) obj;
                } catch (Exception e) {
                    logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }
                JSONObject createSnapshot;
                try {
                    createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
                } catch (Exception e) {
                    logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }
                String runAt = null;
                if (createSnapshot.containsKey("RunAt")) {
                    runAt = createSnapshot.get("RunAt").toString();
                }

                Date date = null;
                try {
                    date = dayFormat_.parse(runAt);
                } catch (ParseException e) {
                    logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + volume.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                }
                if (date == null) {
                    continue;
                }

                if (returnHash.keySet().contains(date)) {
                    returnHash.get(date).add(volume);
                } else {
                    ArrayList<Volume> newArrayList = new ArrayList();
                    newArrayList.add(volume);
                    returnHash.put(date, newArrayList);
                }
                break;
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

    private void resetDidMySnapshotDay() {
        didMySnapshotDay_.clear();

        for (Map.Entry<Region, Integer> entry : splitFactorDay_.entrySet()) {
            HashSet<Date> newHashSet = new HashSet<>();
            didMySnapshotDay_.put(entry.getKey(), newHashSet);
        }

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

    private void addAlreadyDoneTodaySnapshots(ConcurrentHashMap<Region, ArrayList<Volume>> localVolumeTime) {
        HashMap<Region, HashMap<Date, ArrayList<Volume>>> timeDay = new HashMap<>();

        for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
            Region region = entry.getKey();

            if (localVolumeTime.get(region).isEmpty()) {
                continue;
            }

            timeDay.put(region, extractRunAt(localVolumeTime.get(region)));
        }

        for (Map.Entry<Region, ArrayList<Volume>> entry : localVolumeTime.entrySet()) {
            Region region = entry.getKey();

            if (localVolumeTime.get(region).isEmpty()) {
                continue;
            }

            AmazonEC2Client ec2Client = connect(region, awsAccount_.getAwsAccessKeyId(), awsAccount_.getAwsSecretKey());

            for (Volume vol : localVolumeTime.get(region)) {
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
                    logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                            + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    continue;
                }

                String period = getPeriod(eideticParameters, vol);
                if (period == null) {
                    continue;
                }

                Integer keep = getKeep(eideticParameters, vol);
                if (keep == null) {
                    continue;
                }

                Boolean success;
                success = snapshotDecision(ec2Client, vol, period);
                //Success true means we need to take a snapshot, which it can handle l8r
                //Success false means we need to add to hashset
                if (!success) {
                    String runAt = getRunAt(eideticParameters, vol);
                    try {
                        date = dayFormat_.parse(runAt);
                    } catch (ParseException e) {
                        logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                                + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                    }
                    if (date == null) {
                        continue;
                    }
                    didMySnapshotDay_.get(region).add(date);
                }
            }
        }

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

    public String getPeriod(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
            return null;
        }

        String period = null;
        if (createSnapshot.containsKey("Interval")) {
            try {
                period = createSnapshot.get("Interval").toString();
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return period;
    }

    public String getRunAt(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null)) {
            return null;
        }
        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
            return null;
        }

        String runAt = null;
        if (createSnapshot.containsKey("RunAt")) {
            try {
                runAt = createSnapshot.get("RunAt").toString();
            } catch (Exception e) {
                logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\", stacktrace=\""
                        + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            }
        }

        return runAt;
    }

    public Integer getKeep(JSONObject eideticParameters, Volume vol) {
        if ((eideticParameters == null) || (vol == null)) {
            return null;
        }

        JSONObject createSnapshot = null;
        if (eideticParameters.containsKey("CreateSnapshot")) {
            createSnapshot = (JSONObject) eideticParameters.get("CreateSnapshot");
        }
        if (createSnapshot == null) {
            logger.error("Event=Error, Error=\"Malformed Eidetic Tag\", Volume_id=\"" + vol.getVolumeId() + "\"");
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

    public boolean snapshotDecision(AmazonEC2Client ec2Client, Volume vol, String period) {
        if ((ec2Client == null) || (vol == null) || (period == null)) {
            return false;
        }
        try {

            List<Snapshot> int_snapshots = getAllSnapshotsOfVolume(ec2Client, vol,
                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                    awsAccount_.getMaxApiRequestsPerSecond(),
                    awsAccount_.getUniqueAwsAccountIdentifier());

            List<Snapshot> comparelist = new ArrayList();

            for (Snapshot snapshot : int_snapshots) {
                String desc = snapshot.getDescription();
                if ("week".equalsIgnoreCase(period) && desc.startsWith("week_snapshot")) {
                    comparelist.add(snapshot);
                } else if ("day".equalsIgnoreCase(period) && desc.startsWith("day_snapshot")) {
                    if (!desc.contains("snapshot checker")) {
                        comparelist.add(snapshot);
                    }
                } else if ("hour".equalsIgnoreCase(period) && desc.startsWith("hour_snapshot")) {
                    comparelist.add(snapshot);
                } else if ("month".equalsIgnoreCase(period) && desc.startsWith("month_snapshot")) {
                    comparelist.add(snapshot);
                }
            }

            List<Snapshot> sortedCompareList = new ArrayList<>(comparelist);
            sortSnapshotsByDate(sortedCompareList);

            int hours = getHoursBetweenNowAndNewestSnapshot(sortedCompareList);
            int days = getDaysBetweenNowAndNewestSnapshot(sortedCompareList);

            if (("week".equalsIgnoreCase(period) && days < 0) || ("week".equalsIgnoreCase(period) && days >= 7)) {
            } else if (("hour".equalsIgnoreCase(period) && hours < 0) || ("hour".equalsIgnoreCase(period) && hours >= 1)) {
            } else if (("day".equalsIgnoreCase(period) && days < 0) || ("day".equalsIgnoreCase(period) && days >= 1)) {
            } else if (("month".equalsIgnoreCase(period) && days < 0) || ("month".equalsIgnoreCase(period) && days >= 30)) {
            } else {
                return false;
            }

        } catch (Exception e) {
            logger.info("Event=\"Error\", Error=\"error in snapshotDecision\", stacktrace=\""
                    + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return false;
        }

        return true;
    }

}
