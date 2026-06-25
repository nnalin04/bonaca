package com.bonaca.backend.payment.dto;

public record PaymentLinkResponse(String razorpayKeyId, String razorpaySubscriptionId, String paymentLink) {}
