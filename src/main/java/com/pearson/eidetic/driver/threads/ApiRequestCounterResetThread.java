/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.globals.GlobalVariables;
import com.pearson.eidetic.utilities.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
public class ApiRequestCounterResetThread implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiRequestCounterResetThread.class.getName());
    
    private final List<AwsAccount> awsAccounts_;
    private final int resetAfterMilliseconds_;
           
    private boolean continueRunning_;
            
    public ApiRequestCounterResetThread(List<AwsAccount> awsAccounts, int resetAfterMilliseconds) {
        this.awsAccounts_ = awsAccounts;
        this.resetAfterMilliseconds_ = resetAfterMilliseconds;
        this.continueRunning_ = true;
        
        for (AwsAccount awsAccount : awsAccounts_) {
            GlobalVariables.apiRequestAttemptCountersByAwsAccount.put(awsAccount.getUniqueAwsAccountIdentifier(), new AtomicLong(0));
            GlobalVariables.apiRequestCountersByAwsAccount.put(awsAccount.getUniqueAwsAccountIdentifier(), new AtomicLong(0));
        }
    }
    
    @Override
    public void run() {
        
        while (continueRunning_) {
            
            for (String awsAccountKey : GlobalVariables.apiRequestAttemptCountersByAwsAccount.keySet()) {
                logger.debug("AccountKey=" + awsAccountKey + ", NumRequestAttemptsPerSec=" + GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(awsAccountKey));
                GlobalVariables.apiRequestAttemptCountersByAwsAccount.get(awsAccountKey).set(0);
            }
            
            for (String awsAccountKey : GlobalVariables.apiRequestCountersByAwsAccount.keySet()) {
                logger.debug("AccountKey=" + awsAccountKey + ", NumRequestsPerSec=" + GlobalVariables.apiRequestCountersByAwsAccount.get(awsAccountKey));
                GlobalVariables.apiRequestCountersByAwsAccount.get(awsAccountKey).set(0);
            }
            
            Threads.sleepMilliseconds(resetAfterMilliseconds_);
        }

    }

    public List<AwsAccount> getAwsAccounts() {
        return awsAccounts_;
    }
    
    public int getResetAfterMilliseconds_() {
        return resetAfterMilliseconds_;
    }

    public boolean isContinueRunning() {
        return continueRunning_;
    }

    public void setContinueRunning(boolean continueRunning) {
        continueRunning_ = continueRunning;
    }

}
