package com.horizonTrust.paymentService.controller;

import com.horizonTrust.paymentService.dto.CreatePaymentRequest;
import com.horizonTrust.paymentService.dto.PaymentOrderResponse;
import com.horizonTrust.paymentService.service.PaymentService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("v1/payment")
@RequiredArgsConstructor

public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody CreatePaymentRequest request) throws RazorpayException {
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);
        return ResponseEntity.ok().body(response);
    }

    // Razorpay webhook endpoint

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object>payload){
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().body("Webhook Processed");
    }


}
