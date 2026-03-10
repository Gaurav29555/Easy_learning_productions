package com.fixcart.fixcart.controller;

import com.fixcart.fixcart.dto.ConfirmPaymentRequest;
import com.fixcart.fixcart.dto.CreatePaymentOrderRequest;
import com.fixcart.fixcart.dto.PaymentResponse;
import com.fixcart.fixcart.dto.WebhookAckResponse;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.service.PaymentService;
import com.fixcart.fixcart.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @PostMapping("/order")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<PaymentResponse> createOrder(
            Principal principal,
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createOrder(userId, role, request));
    }

    @PatchMapping("/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<PaymentResponse> confirm(
            Principal principal,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        Long userId = userService.extractUserId(principal.getName());
        UserRole role = userService.extractRole(principal.getName());
        return ResponseEntity.ok(paymentService.confirmPayment(userId, role, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> myPayments(Principal principal) {
        Long userId = userService.extractUserId(principal.getName());
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<WebhookAckResponse> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        return ResponseEntity.ok(paymentService.handleRazorpayWebhook(payload, signature));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<WebhookAckResponse> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        return ResponseEntity.ok(paymentService.handleStripeWebhook(payload, signature));
    }
}
