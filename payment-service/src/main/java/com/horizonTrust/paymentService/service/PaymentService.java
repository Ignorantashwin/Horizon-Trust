package com.horizonTrust.paymentService.service;

import com.horizonTrust.paymentService.dto.CreatePaymentRequest;
import com.horizonTrust.paymentService.dto.PaymentOrderResponse;
import com.horizonTrust.paymentService.entity.Payment;
import com.horizonTrust.paymentService.enums.PaymentStatus;
import com.horizonTrust.paymentService.repository.PaymentRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepo paymentRepo;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.keyid}")
    private String keyId;

    @Value("${razorpay.keysecret}")
    private String keySecret;

    /**
     * Create razorPay payment order
     *
     * flow -
     * create payment order in razorpay
     * payment record in db
     * return payment details to frontend
     *  Frontend show razorpay checkout
     *  user pays
     *  razorpay sends status or calls webhook
     *
     * @param request
     * @return
     */

    public PaymentOrderResponse createPaymentOrder( CreatePaymentRequest request) throws RazorpayException {
        log.info("creating payment order with account : {} , amount : {}", request.accountNumber(), request.amount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        // convert the money in short form

        int convertedMoney = request.amount().multiply(BigDecimal.valueOf(100)).intValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedMoney);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0,10));

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        
         log.info("Razorpay Order created : {}", razorpayOrder.get("id").toString());
         Payment payment = new Payment();
         payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
         payment.setAccountNumber(request.accountNumber());
         payment.setAmount(request.amount());
         payment.setCurrency("INR");
         payment.setStatus(PaymentStatus.CREATED);
         payment.setDescription(request.description());

         Payment savedPayment = paymentRepo.save(payment);

         return PaymentOrderResponse.builder()
                 .paymentId(savedPayment.getId())
                 .razorpayId(razorpayOrder.get("id").toString())
                 .amount(savedPayment.getAmount())
                 .currency(savedPayment.getCurrency())
                 .status(savedPayment.getStatus().toString())
                 .razorpayKeyId(keyId)
                 .build();
    }


    public void handleWebhook(Map<String, Object> payload){
        log.info("Received razorpay Webhook : {}", payload.get("event"));
        String event = (String) payload.get("event");

        if ("payment.captured".equals(event)){
            handlePaymentSuccess(payload);
        }
        else if ("payment.failed".equals(event)){
            handlePaymentFailure(payload);
        }
    }


    private void handlePaymentSuccess(Map<String, Object> payload){
        try{

            Map<String,Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("orderId");
            String paymentId = (String) paymentData.get("id");

            Payment payment = paymentRepo.findByRazorpayOrderId(orderId).orElseThrow(()-> new RuntimeException("Payment not found with order : " + orderId));
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepo.save(payment);

            // publish event - paymentCompleted

            Map<String, Object> event = new HashMap<>();
            event.put("payment_id", payment.getId());
            event.put("account_number", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", paymentId);

            // publish event to Kafka

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(), event);

        }catch (Exception e){
          log.error("Handling Payment Success Failed : {}", e.getMessage());
        }
    }


    private void handlePaymentFailure(Map<String, Object> payload){
        try{
            Map<String, Object>paymentData = extractPaymentData(payload);
            String orderId = paymentData.get("orderId").toString();
            String paymentId = paymentData.get("id").toString();

            Payment failedPayment = paymentRepo.findByRazorpayOrderId(orderId).orElseThrow(()-> new RuntimeException("Payment not found with order : " + orderId));
           failedPayment.setFailureReason("Payment failed via Razorpay");
            failedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepo.save(failedPayment);

            Map<String,Object> event = new HashMap<>();
            event.put("order_id", failedPayment.getId());
            event.put("account_number", failedPayment.getAccountNumber());
            event.put("amount", failedPayment.getAmount());
            event.put("reason", failedPayment.getFailureReason());

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, failedPayment.getId(), event);


        } catch (Exception e) {
         log.error("Handling payment failure failed : {}", e.getMessage());
        }
    }

    private Map<String,Object>extractPaymentData(Map<String, Object> payload){
        Map<String, Object> entity = (Map<String, Object>) payload.get("payload");
        Map<String,Object> paymentWrapper = (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>) paymentWrapper.get(entity);
    }

}
