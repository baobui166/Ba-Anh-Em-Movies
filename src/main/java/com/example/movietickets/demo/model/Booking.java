package com.example.movietickets.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "Booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOOKING_ID")
    private Long  id;

    @Column(name = "FILM_NAME")
    private String filmName;

    @Column(name = "POSTER")
    private String poster;

    @Column(name = "CINEMA_NAME")
    private String cinemaName;

    @Column(name = "ROOM_NAME")
    private String roomName;

    @Column(name = "CINEMA_ADDRESS")
    private String cinemaAddress;

    @Column(name = "LICH_CHIEU")
    private Date startTime;

    @Column(name = "CREAT_AT")
    private Date createAt;

    @Column(name = "PRICE")
    private Long price;

    @Column(name = "SEAT_NAME")
    private String  seatName;

    @Column(name = "PAYMENT")
    private String payment;

    @Column(name = "STATUS")
    private Boolean status;

    @Column(name = "ORDER_ID")
    private String orderId;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne
    @JoinColumn(name = "COMBO_ID")
    private ComboFood comboFood;

    @JsonBackReference
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingDetail> bookingDetails;

}
