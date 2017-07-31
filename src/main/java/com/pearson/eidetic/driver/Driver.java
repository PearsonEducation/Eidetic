package com.pearson.eidetic.driver;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.ApiRequestCounterResetThread;
import com.pearson.eidetic.driver.threads.MonitorSnapshotChecker;
import com.pearson.eidetic.driver.threads.MonitorSnapshotCleaner;
import com.pearson.eidetic.driver.threads.MonitorSnapshotCreationTime;
import com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeNoTime;
import com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime;
import com.pearson.eidetic.driver.threads.MonitorCopySnapshot;
import com.pearson.eidetic.driver.threads.MonitorErrorChecker;
import com.pearson.eidetic.driver.threads.MonitorTagChecker;
import com.pearson.eidetic.driver.threads.MonitorVolumeSyncValidator;
import com.pearson.eidetic.driver.threads.RefreshAwsAccountVolumes;
import com.pearson.eidetic.driver.threads.rds.MonitorRDSSnapshotChecker;
import com.pearson.eidetic.driver.threads.rds.MonitorRDSSnapshotCleaner;
import com.pearson.eidetic.driver.threads.rds.MonitorRDSSnapshotNoTime;
import com.pearson.eidetic.driver.threads.rds.MonitorRDSSnapshotTime;
import com.pearson.eidetic.driver.threads.rds.MonitorRDSTagPropagator;
import com.pearson.eidetic.driver.threads.rds.RefreshAwsAccountRDSData;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.network.http.JettySync;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pearson.eidetic.utilities.FileIo;
import com.pearson.eidetic.utilities.StackTrace;
import com.pearson.eidetic.utilities.Threads;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Judah Walker
 */
public class Driver {

    private static final Logger logger = LoggerFactory.getLogger(Driver.class.getName());
    private static final Map<String, Thread> MyMonitorSnapshotVolumeTimeThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotVolumeNoTimeThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorCopySnapshotThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotCheckerThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotCleanerThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotCreationTimeThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotErrorCheckerThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorSnapshotSyncValidatorThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorInstanceTagCheckerThreads_ = new HashMap<>();

    private static final Map<String, Thread> MyMonitorRDSSnapshotTimeThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorRDSSnapshotNoTimeThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorRDSSnapshotCheckerThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorRDSSnapshotCleanerThreads_ = new HashMap<>();
    private static final Map<String, Thread> MyMonitorRDSTagPropagatorThreads_ = new HashMap<>();

    private static final Map<String, Thread> MyRefreshAwsAccountRDSDataThreads_ = new HashMap<>();

    private static final Map<String, Thread> MyRefreshAwsAccountVolumesThreads_ = new HashMap<>();

    private static JettyThread jetty_;

    public static void main(String[] args) {

        boolean initializeSuccess = initializeApplication();

        if (!initializeSuccess) {
            logger.error("An error occurred during application initialization. Shutting down application...");
            System.exit(-1);
        }

        java.security.Security.setProperty("networkaddress.cache.ttl", "30");

        try {
            /**
             * ApiRequestCounterReset (Resets our API count incase it has
             * reached its limit)
             */
            startApiRequestCounterResetThreads();

            /**
             * Pre-populates the passed AWS accounts with Eidetic volumes
             */
            startAwsAccountInitialization();

            /**
             * RDS Automated Tag Propagator
             *
             */
            if (ApplicationConfiguration.getRDSAutomatedTagPropagator()) {
                logger.info("Starting RDS Automated Manual Snapshot threads");
                startRDSTagPropagatorThreads();
            }

            /**
             * Eidetic (Creation and deletion of Eidetic tags)
             */
            if (ApplicationConfiguration.getEidetic()) {
                logger.info("Starting Eidetic threads");
                startAccountSnapshotThreads();
            }
            /**
             * Eidetic_Express (Creation and deletion of CopySnapshot)
             */
            if (ApplicationConfiguration.getEideticExpress()) {
                logger.info("Starting Eidetic_Express threads");
                startAccountCopySnapshotThreads();
            }
            /**
             * Eidetic_Checker (Validating the creation and deletion of Eidetic
             * tags)
             */
            if (ApplicationConfiguration.getEideticChecker()) {
                logger.info("Starting Eidetic_Checker threads");
                startAccountSnapshotCheckerThreads();
            }
            /**
             * Amnesia (Cleaning all stranded and old snapshots for AWS account)
             */
            if (ApplicationConfiguration.getAmnesia()) {
                logger.info("Starting Amnesia threads");
                startAccountSnapshotCleaningThreads();
            }
            /**
             * Snapspoller (Monitors creation time for snapshots)
             */
            if (ApplicationConfiguration.getSnapsPoller()) {
                logger.info("Starting Snapspoller threads");
                startAccountSnapshotCreationTimeThreads();
            }
            /**
             * ErrorChecker (Checks and alerts on any snapshots in error state
             * and deletes them)
             */
            if (ApplicationConfiguration.getErrorChecker()) {
                logger.info("Starting ErrorChecker threads");
                startAccountSnapshotErrorCheckerThreads();
            }
            /**
             * TagChecker (Checks for 'Data' tags with deviceMount point value
             * (ex. /dev/xvdk) and tags them)
             */
            if (ApplicationConfiguration.getTagChecker()) {
                logger.info("Starting TagChecker threads");
                startAccountInstanceTagCheckerThreads();
            }

            /**
             * RDS Automated Manual Snapshot
             *
             */
            if (ApplicationConfiguration.getRDSAutomatedSnapshoter()) {
                logger.info("Starting RDS Automated Manual Snapshot threads");
                startAccountRDSSnapshotThreads();
            }

            /**
             * RDS Automated Snapshot Checker
             *
             */
            if (ApplicationConfiguration.getRDSAutomatedChecker()) {
                logger.info("Starting RDS Automated Snapshot Checker threads");
                startAccountRDSSnapshotCheckerThreads();
            }

            /**
             * RDS Automated Snapshot Cleaner
             *
             */
            if (ApplicationConfiguration.getRDSAutomatedCleaner()) {
                logger.info("Starting RDS Automated Snapshot Cleaner threads");
                startAccountRDSSnapshotCleaningThreads();
            }

            /**
             * RDS Refresh Threads
             *
             */
            if (ApplicationConfiguration.getRDSAutomatedSnapshoter() || ApplicationConfiguration.getRDSAutomatedChecker() || ApplicationConfiguration.getRDSAutomatedCleaner()) {
                logger.info("Starting Account RDS Data Refresh threads");
                startRefreshAwsAccountRDSDataThreads();
            }

            /**
             * Volume synchronizer (Creates an API tier for synchronizing
             * snapshots and validating said snapshots)
             */
            logger.info(ApplicationConfiguration.getVolumeSynchronizer().toString() + " for jetty initialization");
            if (ApplicationConfiguration.getVolumeSynchronizer()) {
                // start the jetty http server
                logger.info("Starting Volume Snapshot Synchronizer threads");
                startSnapshotSyncValidatorThreads();
                logger.info("Attempting to start Jetty Server");

                try {
                    jetty_ = new JettyThread();
                    Thread jetty = new Thread(jetty_);
                    jetty.start();
                } catch (Exception e) {
                    logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                    logger.info("Unable to launch jetty Server. Shutting down application");
                    System.exit(-1);
                }

                logger.info("Started Jetty Server");

            }

            /**
             * RefreshAwsAccountVolumes (Refreshes tagged volumes in our memory)
             */
            startRefreshAwsAccountVolumesThreads();

        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            logger.info("Unable to launch application. Shutting down application");
            System.exit(-1);
        }
        Threads.sleepMinutes(5);

        while (true) {
            try {

                MyRefreshAwsAccountVolumesThreads_.clear();

                startRefreshAwsAccountVolumesThreads();

                Threads.sleepMinutes(5);

            } catch (Exception e) {
                logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));

            }
        }

    }

    public static boolean initializeApplication() {
        boolean isLogbackSuccess = readAndSetLogbackConfiguration(System.getProperty("user.dir"), "logback_config.xml");

        boolean isApplicationConfigSuccess = ApplicationConfiguration.initialize(System.getProperty("user.dir") + File.separator + "application.properties");

        if (!isApplicationConfigSuccess || !isLogbackSuccess) {
            logger.error("An error during application initialization. Exiting...");
            return false;
        }

        logger.info("Finish - Initialize application");

        return true;
    }

    public static boolean readAndSetLogbackConfiguration(String filePath, String fileName) {

        boolean doesConfigFileExist = FileIo.doesFileExist(filePath, fileName);

        if (doesConfigFileExist) {
            File logggerConfigFile = new File(filePath + File.separator + fileName);

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(logggerConfigFile);
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
                return true;
            } catch (Exception e) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
                return false;
            }
        } else {
            return false;
        }
    }

    public static void startAccountSnapshotThreads() {
        List<MonitorSnapshotVolumeTime> MyMonitorSnapshotTime_ = new ArrayList<>();
        List<MonitorSnapshotVolumeNoTime> MyMonitorSnapshotNoTime_ = new ArrayList<>();

        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorSnapshotTime_.add(new MonitorSnapshotVolumeTime(awsAccount));
            MyMonitorSnapshotNoTime_.add(new MonitorSnapshotVolumeNoTime(awsAccount, ApplicationConfiguration.getrunTimeInterval()));
        }

        for (int i = 0; i < MyMonitorSnapshotTime_.size(); i++) {
            MyMonitorSnapshotVolumeTimeThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotTime_.get(i)));
            MyMonitorSnapshotVolumeNoTimeThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotNoTime_.get(i)));
            MyMonitorSnapshotVolumeTimeThreads_.get("Thread_" + i).start();
            MyMonitorSnapshotVolumeNoTimeThreads_.get("Thread_" + i).start();
        }

    }

    public static void startAccountCopySnapshotThreads() {
        /**
         * TransportSnapshots are already initialized at this point
         */
        List<MonitorCopySnapshot> MyMonitorCopySnapshot_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorCopySnapshot_.add(new MonitorCopySnapshot(awsAccount, 10));
        }

        for (int i = 0; i < MyMonitorCopySnapshot_.size(); i++) {
            MyMonitorCopySnapshotThreads_.put("Thread_" + i, new Thread(MyMonitorCopySnapshot_.get(i)));
            MyMonitorCopySnapshotThreads_.get("Thread_" + i).start();
        }

    }

    public static void startAccountSnapshotCheckerThreads() {
        List<MonitorSnapshotChecker> MyMonitorSnapshotChecker_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorSnapshotChecker_.add(new MonitorSnapshotChecker(awsAccount));
        }

        for (int i = 0; i < MyMonitorSnapshotChecker_.size(); i++) {
            MyMonitorSnapshotCheckerThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotChecker_.get(i)));
            MyMonitorSnapshotCheckerThreads_.get("Thread_" + i).start();
        }

    }

    public static void startAccountSnapshotCleaningThreads() {
        List<MonitorSnapshotCleaner> MyMonitorSnapshotCleaner_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorSnapshotCleaner_.add(new MonitorSnapshotCleaner(awsAccount));
        }

        for (int i = 0; i < MyMonitorSnapshotCleaner_.size(); i++) {
            MyMonitorSnapshotCleanerThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotCleaner_.get(i)));
            MyMonitorSnapshotCleanerThreads_.get("Thread_" + i).start();
        }
    }

    public static void startAccountSnapshotCreationTimeThreads() {
        List<MonitorSnapshotCreationTime> MyMonitorSnapshotCreationTime_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorSnapshotCreationTime_.add(new MonitorSnapshotCreationTime(awsAccount));
        }

        for (int i = 0; i < MyMonitorSnapshotCreationTime_.size(); i++) {
            MyMonitorSnapshotCreationTimeThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotCreationTime_.get(i)));
            MyMonitorSnapshotCreationTimeThreads_.get("Thread_" + i).start();
        }
    }

    private static void startAccountSnapshotErrorCheckerThreads() {
        List<MonitorErrorChecker> MyMonitorSnapshotErrorChecker_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorSnapshotErrorChecker_.add(new MonitorErrorChecker(awsAccount));
        }

        for (int i = 0; i < MyMonitorSnapshotErrorChecker_.size(); i++) {
            MyMonitorSnapshotErrorCheckerThreads_.put("Thread_" + i, new Thread(MyMonitorSnapshotErrorChecker_.get(i)));
            MyMonitorSnapshotErrorCheckerThreads_.get("Thread_" + i).start();
        }
    }

    private static void startAccountInstanceTagCheckerThreads() {
        List<MonitorTagChecker> MyMonitorTagChecker_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyMonitorTagChecker_.add(new MonitorTagChecker(awsAccount));
        }

        for (int i = 0; i < MyMonitorTagChecker_.size(); i++) {
            MyMonitorInstanceTagCheckerThreads_.put("Thread_" + i, new Thread(MyMonitorTagChecker_.get(i)));
            MyMonitorInstanceTagCheckerThreads_.get("Thread_" + i).start();
        }
    }

    private static void startSnapshotSyncValidatorThreads() {
        List<MonitorVolumeSyncValidator> MySnapshotSyncValidator_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MySnapshotSyncValidator_.add(new MonitorVolumeSyncValidator(awsAccount, 30));
        }

        for (int i = 0; i < MySnapshotSyncValidator_.size(); i++) {
            MyMonitorSnapshotSyncValidatorThreads_.put("Thread_" + i, new Thread(MySnapshotSyncValidator_.get(i)));
            MyMonitorSnapshotSyncValidatorThreads_.get("Thread_" + i).start();
        }
    }

    private static void startRefreshAwsAccountVolumesThreads() {
        List<RefreshAwsAccountVolumes> MyRefreshAwsAccountVolumes_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyRefreshAwsAccountVolumes_.add(new RefreshAwsAccountVolumes(awsAccount));
            logger.info("Starting global account refresh threads for account id: " + awsAccount.getAwsAccountId());
        }

        for (int i = 0; i < MyRefreshAwsAccountVolumes_.size(); i++) {
            MyRefreshAwsAccountVolumesThreads_.put("Thread_" + i, new Thread(MyRefreshAwsAccountVolumes_.get(i)));
            MyRefreshAwsAccountVolumesThreads_.get("Thread_" + i).start();
        }
    }

    private static void startApiRequestCounterResetThreads() {
        ApiRequestCounterResetThread apiRequestCounterResetThread = new ApiRequestCounterResetThread(ApplicationConfiguration.getAwsAccounts(), 1000);
        Thread apiRequestCounterResetThread_Thread = new Thread(apiRequestCounterResetThread);
        apiRequestCounterResetThread_Thread.start();
    }

    private static void startAwsAccountInitialization() {
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            awsAccount.initializeEC2Snapshots();
            if (!awsAccount.prohibitRDSCalls() && ApplicationConfiguration.getRDSAutomatedSnapshoter()) {
                logger.info("Launching RDS AwsAccountInitialization threads for account id: " + awsAccount.getAwsAccountId());
                awsAccount.initializeRDSSnapshots();
            }
            
        }
    }

    private static void startAccountRDSSnapshotThreads() {
        List<MonitorRDSSnapshotTime> MyMonitorRDSSnapshotTime_ = new ArrayList<>();
        List<MonitorRDSSnapshotNoTime> MyMonitorRDSSnapshotNoTime_ = new ArrayList<>();

        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            if (!awsAccount.prohibitRDSCalls()) {
                logger.info("Launching RDS AccountRDSSnapshot threads for account id: " + awsAccount.getAwsAccountId());
                MyMonitorRDSSnapshotTime_.add(new MonitorRDSSnapshotTime(awsAccount));
                MyMonitorRDSSnapshotNoTime_.add(new MonitorRDSSnapshotNoTime(awsAccount, ApplicationConfiguration.getrunTimeInterval()));
            }
        }

        for (int i = 0; i < MyMonitorRDSSnapshotTime_.size(); i++) {
            MyMonitorRDSSnapshotTimeThreads_.put("Thread_" + i, new Thread(MyMonitorRDSSnapshotTime_.get(i)));
            MyMonitorRDSSnapshotNoTimeThreads_.put("Thread_" + i, new Thread(MyMonitorRDSSnapshotNoTime_.get(i)));
            MyMonitorRDSSnapshotTimeThreads_.get("Thread_" + i).start();
            MyMonitorRDSSnapshotNoTimeThreads_.get("Thread_" + i).start();
        }
    }

    private static void startAccountRDSSnapshotCheckerThreads() {
        List<MonitorRDSSnapshotChecker> MyMonitorRDSSnapshotChecker_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            if (!awsAccount.prohibitRDSCalls()) {
                logger.info("Launching RDS AccountRDSSnapshotChecker threads for account id: " + awsAccount.getAwsAccountId());
                MyMonitorRDSSnapshotChecker_.add(new MonitorRDSSnapshotChecker(awsAccount));
            }
        }

        for (int i = 0; i < MyMonitorRDSSnapshotChecker_.size(); i++) {
            MyMonitorRDSSnapshotCheckerThreads_.put("Thread_" + i, new Thread(MyMonitorRDSSnapshotChecker_.get(i)));
            MyMonitorRDSSnapshotCheckerThreads_.get("Thread_" + i).start();
        }
    }

    private static void startAccountRDSSnapshotCleaningThreads() {
        List<MonitorRDSSnapshotCleaner> MyMonitorRDSSnapshotCleaner_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            if (!awsAccount.prohibitRDSCalls()) {
                logger.info("Launching RDS AccountRDSSnapshotCleaning threads for account id: " + awsAccount.getAwsAccountId());
                MyMonitorRDSSnapshotCleaner_.add(new MonitorRDSSnapshotCleaner(awsAccount));
            }
        }

        for (int i = 0; i < MyMonitorRDSSnapshotCleaner_.size(); i++) {
            MyMonitorRDSSnapshotCleanerThreads_.put("Thread_" + i, new Thread(MyMonitorRDSSnapshotCleaner_.get(i)));
            MyMonitorRDSSnapshotCleanerThreads_.get("Thread_" + i).start();
        }
    }

    private static void startRefreshAwsAccountRDSDataThreads() {
        List<RefreshAwsAccountRDSData> MyRefreshAwsAccountRDSData_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            if (!awsAccount.prohibitRDSCalls()) {
                logger.info("Launching RefreshAwsAccountRDS threads for account id: " + awsAccount.getAwsAccountId());
                MyRefreshAwsAccountRDSData_.add(new RefreshAwsAccountRDSData(awsAccount));
            }
        }

        for (int i = 0; i < MyRefreshAwsAccountRDSData_.size(); i++) {
            MyRefreshAwsAccountRDSDataThreads_.put("Thread_" + i, new Thread(MyRefreshAwsAccountRDSData_.get(i)));
            MyRefreshAwsAccountRDSDataThreads_.get("Thread_" + i).start();
        }
    }
    
    private static void startRDSTagPropagatorThreads() {
        List<MonitorRDSTagPropagator> MyMonitorRDSTagPropagator_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            if (!awsAccount.prohibitRDSCalls()) {
                logger.info("Launching RDS TagPropagator threads for account id: " + awsAccount.getAwsAccountId());
                MyMonitorRDSTagPropagator_.add(new MonitorRDSTagPropagator(awsAccount));
                
            }
        }

        for (int i = 0; i < MyMonitorRDSTagPropagator_.size(); i++) {
            MyMonitorRDSTagPropagatorThreads_.put("Thread_" + i, new Thread(MyMonitorRDSTagPropagator_.get(i)));
            MyMonitorRDSTagPropagatorThreads_.get("Thread_" + i).start();
        }
    }

}
