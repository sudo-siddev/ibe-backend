package com.booking.reviews.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public AWSSimpleSystemsManagement ssmClient() {
        return AWSSimpleSystemsManagementClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }
}

