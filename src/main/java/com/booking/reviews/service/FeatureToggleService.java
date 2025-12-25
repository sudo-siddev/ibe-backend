package com.booking.reviews.service;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureToggleService.class);

    private final AWSSimpleSystemsManagement ssmClient;
    private final String globalWriteReviewEnabledParameter;
    private final Boolean localOverrideEnabled;

    public FeatureToggleService(
            AWSSimpleSystemsManagement ssmClient,
            @Value("${aws.parameter-store.global-write-review-enabled:/review-system/global/write-review-enabled}") 
            String globalWriteReviewEnabledParameter,
            @Value("${feature-toggle.local-override.enabled:false}") 
            Boolean localOverrideEnabled) {
        this.ssmClient = ssmClient;
        this.globalWriteReviewEnabledParameter = globalWriteReviewEnabledParameter;
        this.localOverrideEnabled = localOverrideEnabled;
    }

    public boolean isGlobalWriteReviewEnabled() {
        if (localOverrideEnabled != null && localOverrideEnabled) {
            logger.info("Using local override: global write review enabled = true");
            return true;
        }

        try {
            GetParameterRequest request = new GetParameterRequest()
                    .withName(globalWriteReviewEnabledParameter)
                    .withWithDecryption(true);

            GetParameterResult result = ssmClient.getParameter(request);
            String value = result.getParameter().getValue();
            boolean enabled = Boolean.parseBoolean(value);
            logger.debug("Global write review enabled: {}", enabled);
            return enabled;
        } catch (ParameterNotFoundException e) {
            logger.warn("Parameter {} not found in Parameter Store, defaulting to false", 
                    globalWriteReviewEnabledParameter);
            return false;
        } catch (Exception e) {
            logger.error("Error fetching parameter {} from Parameter Store, defaulting to false", 
                    globalWriteReviewEnabledParameter, e);
            return false;
        }
    }
}

