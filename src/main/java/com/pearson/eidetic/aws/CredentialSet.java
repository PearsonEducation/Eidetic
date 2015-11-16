package com.pearson.eidetic.aws;

import com.amazonaws.auth.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
public class CredentialSet implements AWSCredentials {
    
    private static final Logger logger = LoggerFactory.getLogger(CredentialSet.class.getName());
    
    private final String awsAccessKeyId_;
    private final String awsSecretKey_;
    
    public CredentialSet(String awsAccessKeyId, String awsSecretKey) {
        this.awsAccessKeyId_ = awsAccessKeyId;
        this.awsSecretKey_ = awsSecretKey;
    }
    
    @Override
    public String getAWSAccessKeyId() {
        return awsAccessKeyId_;
    }

    @Override
    public String getAWSSecretKey() {
        return awsSecretKey_;
    }

}

