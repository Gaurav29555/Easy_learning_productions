package com.fixcart.fixcart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixcart.fixcart.dto.ConfirmPaymentRequest;
import com.fixcart.fixcart.dto.CreatePaymentOrderRequest;
import com.fixcart.fixcart.dto.PaymentResponse;
import com.fixcart.fixcart.dto.WebhookAckResponse;
import com.fixcart.fixcart.entity.Booking;
import com.fixcart.fixcart.entity.Payment;
import com.fixcart.fixcart.entity.User;
import com.fixcart.fixcart.entity.enums.PaymentProvider;
import com.fixcart.fixcart.entity.enums.PaymentStatus;
import com.fixcart.fixcart.entity.enums.UserRole;
import com.fixcart.fixcart.exception.BadRequestException;
import com.fixcart.fixcart.exception.ResourceNotFoundException;
import com.fixcart.fixcart.repository.BookingRepository;
import com.fixcart.fixcart.repository.PaymentRepository;
import com.fixcart.fixcart.repository.UserRepository;
import com.fixcart.fixcart.util.FixcartCryptoUtils;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${fixcart.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${fixcart.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${fixcart.payment.razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${fixcart.payment.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${fixcart.payment.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Transactional
    public PaymentResponse createOrder(Long userId, UserRole role, CreatePaymentOrderRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!role.equals(UserRole.ADMIN) && !booking.getCustomer().getId().equals(userId)) {
            throw new BadRequestException("Only booking customer can create payment");
        }

        User customer = userRepository.findById(booking.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setCustomer(customer);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency().toUpperCase());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setProvider(request.provider());

        switch (request.provider()) {
            case MOCK -> payment.setProviderOrderId("fixcart_order_" + UUID.randomUUID());
            case RAZORPAY -> createRazorpayOrder(payment);
            case STRIPE -> createStripePaymentIntent(payment);
            default -> throw new BadRequestException("Unsupported payment provider");
        }

        return map(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse confirmPayment(Long userId, UserRole role, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findByProviderOrderId(request.providerOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        if (!role.equals(UserRole.ADMIN) && !payment.getCustomer().getId().equals(userId)) {
            throw new BadRequestException("User cannot confirm this payment");
        }

        payment.setProviderPaymentId(request.providerPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS);
        return map(paymentRepository.save(payment));
    }

    public List<PaymentResponse> getMyPayments(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::map)
                .toList();
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::map).toList();
    }

    @Transactional
    public WebhookAckResponse handleRazorpayWebhook(String payload, String signature) {
        if (razorpayWebhookSecret.isBlank()) {
            throw new BadRequestException("Razorpay webhook secret is not configured");
        }
        String expected = FixcartCryptoUtils.hmacSha256Hex(razorpayWebhookSecret, payload);
        if (!FixcartCryptoUtils.constantTimeEquals(expected, signature)) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            String orderId = root.path("payload").path("payment").path("entity").path("order_id").asText("");
            String paymentId = root.path("payload").path("payment").path("entity").path("id").asText("");
            Payment payment = paymentRepository.findByProviderOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found for webhook"));

            if ("payment.captured".equals(event) || "payment.authorized".equals(event)) {
                payment.setProviderPaymentId(paymentId);
                payment.setStatus("payment.captured".equals(event) ? PaymentStatus.SUCCESS : PaymentStatus.AUTHORIZED);
            } else if ("payment.failed".equals(event)) {
                payment.setProviderPaymentId(paymentId);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Razorpay webhook failure event");
            }
            paymentRepository.save(payment);
            return new WebhookAckResponse(true, "Razorpay webhook processed");
        } catch (Exception exception) {
            throw new BadRequestException("Invalid Razorpay webhook payload: " + exception.getMessage());
        }
    }

    @Transactional
    public WebhookAckResponse handleStripeWebhook(String payload, String stripeSignatureHeader) {
        if (stripeWebhookSecret.isBlank()) {
            throw new BadRequestException("Stripe webhook secret is not configured");
        }
        verifyStripeSignature(payload, stripeSignatureHeader);
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("type").asText("");
            JsonNode object = root.path("data").path("object");
            String paymentIntentId = object.path("id").asText("");
            Payment payment = paymentRepository.findByProviderOrderId(paymentIntentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stripe payment intent not found"));

            if ("payment_intent.succeeded".equals(event)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderPaymentId(paymentIntentId);
            } else if ("payment_intent.payment_failed".equals(event)) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(object.path("last_payment_error").path("message").asText("Payment failed"));
            }
            paymentRepository.save(payment);
            return new WebhookAckResponse(true, "Stripe webhook processed");
        } catch (Exception exception) {
            throw new BadRequestException("Invalid Stripe webhook payload: " + exception.getMessage());
        }
    }

    private void createRazorpayOrder(Payment payment) {
        if (razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay keys are not configured");
        }
        try {
            long amountMinor = toMinorAmount(payment.getAmount(), payment.getCurrency());
            String json = objectMapper.writeValueAsString(Map.of(
                    "amount", amountMinor,
                    "currency", payment.getCurrency(),
                    "receipt", "fixcart_" + UUID.randomUUID()
            ));
            String auth = Base64.getEncoder().encodeToString(
                    (razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Razorpay order creation failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            payment.setProviderOrderId(root.path("id").asText());
        } catch (Exception exception) {
            throw new BadRequestException("Razorpay order creation failed: " + exception.getMessage());
        }
    }

    private void createStripePaymentIntent(Payment payment) {
        if (stripeSecretKey.isBlank()) {
            throw new BadRequestException("Stripe secret key is not configured");
        }
        try {
            long amountMinor = toMinorAmount(payment.getAmount(), payment.getCurrency());
            String body = "amount=" + amountMinor
                    + "&currency=" + URLEncoder.encode(payment.getCurrency().toLowerCase(), StandardCharsets.UTF_8)
                    + "&automatic_payment_methods[enabled]=true";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/payment_intents"))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Stripe payment intent creation failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            payment.setProviderOrderId(root.path("id").asText());
            payment.setProviderClientSecret(root.path("client_secret").asText(null));
        } catch (Exception exception) {
            throw new BadRequestException("Stripe payment intent creation failed: " + exception.getMessage());
        }
    }

    private void verifyStripeSignature(String payload, String header) {
        if (header == null || header.isBlank()) {
            throw new BadRequestException("Missing Stripe signature header");
        }
        String[] parts = header.split(",");
        String timestamp = null;
        String signature = null;
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                if ("t".equals(kv[0])) {
                    timestamp = kv[1];
                }
                if ("v1".equals(kv[0])) {
                    signature = kv[1];
                }
            }
        }
        if (timestamp == null || signature == null) {
            throw new BadRequestException("Invalid Stripe signature header format");
        }
        String signedPayload = timestamp + "." + payload;
        String expected = FixcartCryptoUtils.hmacSha256Hex(stripeWebhookSecret, signedPayload);
        if (!FixcartCryptoUtils.constantTimeEquals(expected, signature)) {
            throw new BadRequestException("Invalid Stripe webhook signature");
        }
    }

    private long toMinorAmount(BigDecimal amount, String currency) {
        int scale = "JPY".equalsIgnoreCase(currency) ? 0 : 2;
        return amount.movePointRight(scale).longValueExact();
    }

    private PaymentResponse map(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getCustomer().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getProviderClientSecret(),
                payment.getFailureReason(),
                payment.getCreatedAt()
        );
    }
}
