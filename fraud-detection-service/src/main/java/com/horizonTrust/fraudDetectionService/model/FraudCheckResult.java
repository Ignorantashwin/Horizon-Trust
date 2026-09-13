package com.horizonTrust.fraudDetectionService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {
    private boolean fraud;
    private String reason;
}
