package com.pearson.eidetic.globals;

import com.pearson.eidetic.aws.AwsAccount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pearson.eidetic.utilities.PropertiesConfigurationWrapper;
import com.pearson.eidetic.utilities.StackTrace;

/**
 * @author Jeffrey Schmidt
 */
public class ApplicationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class.getName());

    public static final int VALUE_NOT_SET_CODE = -4444;

    private static Boolean isInitializeSuccess_ = false;

    private static PropertiesConfiguration applicationConfiguration_ = null;

    private static final List<AwsAccount> awsAccounts_ = new ArrayList<>();

    private static int awsCallRetryAttempts_ = VALUE_NOT_SET_CODE;

    private static Integer runTimeInterval_;
    
    private static String defaultEideticTag_;
    
    private static Integer eideticCleanKeepDays_;
    
    private static Integer allSnapshotCleanKeepDays_;
    
    private static Integer syncServerHttpListenerPort_;

    private static Boolean eidetic_;
    private static Boolean eideticExpress_;
    private static Boolean eideticChecker_;
    private static Boolean amnesia_;
    
    private static Boolean snapsPoller_;
    private static Boolean errorChecker_;
    private static Boolean tagChecker_;
    
    private static Boolean volumeSynchronizer_;
    
    private static Boolean rdsAutomatedSnapshoter_;
    private static Boolean rdsAutomatedChecker_;
    private static Boolean rdsAutomatedCleaner_;
    private static Boolean rdsAutomatedTagPropagator_;


    public static boolean initialize(String filePathAndFilename) {

        if (filePathAndFilename == null) {
            return false;
        }
        
        PropertiesConfigurationWrapper propertiesConfigurationWrapper = new PropertiesConfigurationWrapper(filePathAndFilename);
        
        if (!propertiesConfigurationWrapper.isValid()) {
            return false;
        }
        
        applicationConfiguration_ = propertiesConfigurationWrapper.getPropertiesConfiguration();
        
        isInitializeSuccess_ = setApplicationConfigurationValues();
        
        return isInitializeSuccess_;
    }

    private static boolean setApplicationConfigurationValues() {

        try {
            /**
             * *CONFIG_COUNT =
             * applicationConfiguration_.getString("CONFIG_COUNT", "taco");**
             */

            awsAccounts_.addAll(readAwsAccounts());
            defaultEideticTag_ = applicationConfiguration_.getString("default_eidetic_tag_value", " \"CreateSnapshot\" : { \"Interval\" : \"day\", \"Retain\" : \"2\" } } ");
            eideticCleanKeepDays_ = applicationConfiguration_.getInt("eidetic_snapshot_retain_days", 21);
            allSnapshotCleanKeepDays_ = applicationConfiguration_.getInt("all_snapshot_retain_days", 365);
            awsCallRetryAttempts_ = applicationConfiguration_.getInt("aws_call_retry_attempts", 3);
            eidetic_ = applicationConfiguration_.getBoolean("enable_eidetic", false);
            runTimeInterval_ = applicationConfiguration_.getInteger("eidetic_check_volumes_mins", 10);
            eideticExpress_ = applicationConfiguration_.getBoolean("enable_eidetic_express", false);
            eideticChecker_ = applicationConfiguration_.getBoolean("enable_eidetic_checker", false);
            amnesia_ = applicationConfiguration_.getBoolean("enable_amnesia", false);
            snapsPoller_ = applicationConfiguration_.getBoolean("enable_snapshot_time_poller", false);
            errorChecker_ = applicationConfiguration_.getBoolean("enable_snapshot_error_checker", false);
            tagChecker_ = applicationConfiguration_.getBoolean("enable_instance_data_tag_checker", false);
            volumeSynchronizer_ = applicationConfiguration_.getBoolean("enable_volume_snapshot_synchronizer", false);
            syncServerHttpListenerPort_ = applicationConfiguration_.getInteger("synchronizer_http_port", 80);
            rdsAutomatedSnapshoter_ = applicationConfiguration_.getBoolean("enable_rds_automated_snapshot", false);
            rdsAutomatedChecker_ = applicationConfiguration_.getBoolean("enable_rds_automated_snapshot_checker", false);
            rdsAutomatedCleaner_ = applicationConfiguration_.getBoolean("enable_rds_automated_snapshot_cleaner", false);
            rdsAutomatedTagPropagator_ = applicationConfiguration_.getBoolean("enable_rds_automated_tag_propagator", false);
            return true;
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            return false;
        }

    }

    private static List<AwsAccount> readAwsAccounts() {

        List<AwsAccount> awsAccounts = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            String awsAccountNicknameKey = "aws_account_nickname_" + (i + 1);
            String awsAccountNicknameValue = applicationConfiguration_.getString(awsAccountNicknameKey, "Eidetic_Default");
            String awsAccountAccessKeyIdKey = "aws_account_access_key_id_" + (i + 1);
            String awsAccountAccessKeyIdValue = applicationConfiguration_.getString(awsAccountAccessKeyIdKey, null);
            String awsAccountAccessSecretKeyKey = "aws_account_access_key_secret_" + (i + 1);
            String awsAccountAccessSecretKeyValue = applicationConfiguration_.getString(awsAccountAccessSecretKeyKey, null);
            String awsAccountProhibitRDSCallsKey = "aws_account_prohibit_rds_" + (i + 1);
            Boolean awsAccountProhibitRDSCallsValue = applicationConfiguration_.getBoolean(awsAccountProhibitRDSCallsKey, false);
            String awsAccountEnableCachingKey = "aws_account_enable_caching_" + (i + 1);
            Boolean awsAccountEnableCachingValue = applicationConfiguration_.getBoolean(awsAccountEnableCachingKey, false);
            
            if (awsAccountNicknameValue.contains("Eidetic_Default") && awsAccountAccessKeyIdValue == null && awsAccountAccessSecretKeyValue == null) {
                continue;
            }
            
            String awsMaxApiRequestsPerSecondKey = "aws_account_max_api_requests_per_second_" + (i + 1);
            int awsMaxApiRequestsPerSecondValue = applicationConfiguration_.getInt(awsMaxApiRequestsPerSecondKey, 100);

            AwsAccount awsAccount = new AwsAccount((i + 1), 
                    awsAccountNicknameValue,
                    awsAccountAccessKeyIdValue, 
                    awsAccountAccessSecretKeyValue, 
                    awsMaxApiRequestsPerSecondValue, 
                    awsAccountProhibitRDSCallsValue,
                    awsAccountEnableCachingValue);

            if ((awsAccountAccessKeyIdValue != null) && (awsAccountAccessSecretKeyValue != null)) {
                awsAccounts_.add(awsAccount);
            }

        }

        return awsAccounts;
    }

    public static List<AwsAccount> getAwsAccounts() {
        return awsAccounts_;
    }

    public static int getAwsCallRetryAttempts() {
        return awsCallRetryAttempts_;
    }

    public static Boolean getEidetic() {
        return eidetic_;
    }

    public static Integer getrunTimeInterval() {
        return runTimeInterval_;
    }

    public static Boolean getEideticExpress() {
        return eideticExpress_;
    }

    public static Boolean getEideticChecker() {
        return eideticChecker_;
    }

    public static Boolean getAmnesia() {
        return amnesia_;
    }

    public static Boolean getSnapsPoller() {
        return snapsPoller_;
    }

    public static Boolean getErrorChecker() {
        return errorChecker_;
    }

    public static Boolean getTagChecker() {
        return tagChecker_;
    }
    
    public static Boolean getVolumeSynchronizer() {
        return volumeSynchronizer_;
    }

    public static String getDefaultEideticTag() {
        return defaultEideticTag_;
    }
    
    public static Integer geteideticCleanKeepDays() {
        return eideticCleanKeepDays_;
    }

    public static Integer getallSnapshotCleanKeepDays() {
        return allSnapshotCleanKeepDays_;
    }
    
    public static Integer getSyncServerHttpListenerPort() {
        return syncServerHttpListenerPort_;
    }
    
    public static Boolean getRDSAutomatedSnapshoter() {
        return rdsAutomatedSnapshoter_;
    }
    
    public static Boolean getRDSAutomatedChecker() {
        return rdsAutomatedChecker_;
    }
    
    public static Boolean getRDSAutomatedCleaner() {
        return rdsAutomatedCleaner_;
    }
    
    public static Boolean getRDSAutomatedTagPropagator() {
        return rdsAutomatedTagPropagator_;
    }

}
