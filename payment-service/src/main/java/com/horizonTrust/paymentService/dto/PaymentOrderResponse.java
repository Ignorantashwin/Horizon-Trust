package com.horizonTrust.paymentService.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentOrderResponse(String paymentId, String razorpayId, BigDecimal amount, String status, String currency, String razorpayKeyId) {
}
