package com.booking.reviews.service;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    @Mock
    private AWSSimpleSystemsManagement ssmClient;

    @InjectMocks
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        // Use reflection or constructor injection - for now, we'll test with mocked client
        // In real scenario, we'd use @InjectMocks properly
    }

    @Test
    void isGlobalWriteReviewEnabled_True() throws Exception {
        // Arrange
        AWSSimpleSystemsManagement mockClient = mock(AWSSimpleSystemsManagement.class);
        GetParameterResult result = new GetParameterResult();
        Parameter parameter = new Parameter();
        parameter.setValue("true");
        result.setParameter(parameter);

        when(mockClient.getParameter(any(GetParameterRequest.class))).thenReturn(result);

        FeatureToggleService service = new FeatureToggleService(
                mockClient,
                "/review-system/global/write-review-enabled",
                false // localOverrideEnabled = false to test AWS behavior
        );

        // Act
        boolean enabled = service.isGlobalWriteReviewEnabled();

        // Assert
        assertTrue(enabled);
        verify(mockClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    void isGlobalWriteReviewEnabled_False() throws Exception {
        // Arrange
        AWSSimpleSystemsManagement mockClient = mock(AWSSimpleSystemsManagement.class);
        GetParameterResult result = new GetParameterResult();
        Parameter parameter = new Parameter();
        parameter.setValue("false");
        result.setParameter(parameter);

        when(mockClient.getParameter(any(GetParameterRequest.class))).thenReturn(result);

        FeatureToggleService service = new FeatureToggleService(
                mockClient,
                "/review-system/global/write-review-enabled",
                false // localOverrideEnabled = false to test AWS behavior
        );

        // Act
        boolean enabled = service.isGlobalWriteReviewEnabled();

        // Assert
        assertFalse(enabled);
    }

    @Test
    void isGlobalWriteReviewEnabled_ParameterNotFound() throws Exception {
        // Arrange
        AWSSimpleSystemsManagement mockClient = mock(AWSSimpleSystemsManagement.class);
        when(mockClient.getParameter(any(GetParameterRequest.class)))
                .thenThrow(new ParameterNotFoundException("Parameter not found"));

        FeatureToggleService service = new FeatureToggleService(
                mockClient,
                "/review-system/global/write-review-enabled",
                false // localOverrideEnabled = false to test AWS behavior
        );

        // Act
        boolean enabled = service.isGlobalWriteReviewEnabled();

        // Assert
        assertFalse(enabled); // Should default to false
    }

    @Test
    void isGlobalWriteReviewEnabled_Exception() throws Exception {
        // Arrange
        AWSSimpleSystemsManagement mockClient = mock(AWSSimpleSystemsManagement.class);
        when(mockClient.getParameter(any(GetParameterRequest.class)))
                .thenThrow(new RuntimeException("AWS error"));

        FeatureToggleService service = new FeatureToggleService(
                mockClient,
                "/review-system/global/write-review-enabled",
                false // localOverrideEnabled = false to test AWS behavior
        );

        // Act
        boolean enabled = service.isGlobalWriteReviewEnabled();

        // Assert
        assertFalse(enabled); // Should default to false on error
    }

    @Test
    void isGlobalWriteReviewEnabled_LocalOverrideEnabled() {
        // Arrange
        AWSSimpleSystemsManagement mockClient = mock(AWSSimpleSystemsManagement.class);

        FeatureToggleService service = new FeatureToggleService(
                mockClient,
                "/review-system/global/write-review-enabled",
                true // localOverrideEnabled = true
        );

        // Act
        boolean enabled = service.isGlobalWriteReviewEnabled();

        // Assert
        assertTrue(enabled); // Should return true when local override is enabled
        verify(mockClient, never()).getParameter(any(GetParameterRequest.class)); // Should not call AWS
    }
}

