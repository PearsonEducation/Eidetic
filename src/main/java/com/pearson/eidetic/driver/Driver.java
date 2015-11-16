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
import com.pearson.eidetic.driver.threads.RefreshAwsAccountVolumes;
import com.pearson.eidetic.globals.ApplicationConfiguration;
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
 * @author uwalkj6
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
    private static final Map<String, Thread> MyMonitorInstanceTagCheckerThreads_ = new HashMap<>();

    private static final Map<String, Thread> MyRefreshAwsAccountVolumesThreads_ = new HashMap<>();

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
             * Eidetic (Creation and deletion of Eidetic tags)
             */
            if (ApplicationConfiguration.getEidetic()) {
                startAccountSnapshotThreads();
            }
            /**
             * Eidetic_Express (Creation and deletion of CopySnapshot)
             */
            if (ApplicationConfiguration.getEideticExpress()) {
                startAccountCopySnapshotThreads();
            }
            /**
             * Eidetic_Checker (Validating the creation and deletion of Eidetic
             * tags)
             */
            if (ApplicationConfiguration.getEideticChecker()) {
                startAccountSnapshotCheckerThreads();
            }
            /**
             * Amnesia (Cleaning all stranded and old snapshots for AWS account)
             */
            if (ApplicationConfiguration.getAmnesia()) {
                startAccountSnapshotCleaningThreads();
            }
            /**
             * Snapspoller (Monitors creation time for snapshots)
             */
            if (ApplicationConfiguration.getSnapsPoller()) {
                startAccountSnapshotCreationTimeThreads();
            }
            /**
             * ErrorChecker (Checks and alerts on any snapshots in error state
             * and deletes them)
             */
            if (ApplicationConfiguration.getErrorChecker()) {
                startAccountSnapshotErrorCheckerThreads();
            }
            /**
             * TagChecker (Checks for 'Data' tags with deviceMount point value
             * (ex. /dev/xvdk) and tags them)
             */
            if (ApplicationConfiguration.getTagChecker()) {
                startAccountInstanceTagCheckerThreads();
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

    private static void startRefreshAwsAccountVolumesThreads() {
        List<RefreshAwsAccountVolumes> MyRefreshAwsAccountVolumes_ = new ArrayList<>();
        for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
            MyRefreshAwsAccountVolumes_.add(new RefreshAwsAccountVolumes(awsAccount));
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
            awsAccount.initializeSnapshots();
        }
    }

}
