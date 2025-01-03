package com.example.movietickets.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "Schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEDULE_ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FILM_ID")
    @JsonBackReference
    private Film film;

    @ManyToOne
    @JoinColumn(name = "ROOM_ID")
    private Room room;

    @Column(name = "START_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date startTime;


    // Constructor without parameters
    public Schedule() {
    }

    // Constructor with schedule ID
    public Schedule(Long id) {
        this.id = id;
    }

}
