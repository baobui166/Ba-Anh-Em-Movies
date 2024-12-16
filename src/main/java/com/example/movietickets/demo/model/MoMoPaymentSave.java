package com.example.movietickets.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class MoMoPaymentSave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Booking booking;

    private Long amount;
    private String orderId;
    private String requestId;
    private String transactionId;
    private String paymentStatus;
    private String paymentMethod;
    private Date createdAt;
    private Date paymentTime;
    private String failureMessage;

    // Getters and setters
}