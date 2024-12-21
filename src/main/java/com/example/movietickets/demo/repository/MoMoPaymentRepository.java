package com.example.movietickets.demo.repository;

import com.example.movietickets.demo.model.MoMoPaymentSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MoMoPaymentRepository extends JpaRepository<MoMoPaymentSave,Long> {
    Optional<MoMoPaymentSave> findByOrderId(String orderId);
}
