package com.example.movietickets.demo.service;

import com.example.movietickets.demo.model.MoMoPaymentSave;
import com.example.movietickets.demo.repository.MoMoPaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class MoMoPaymentService {

    private final MoMoPaymentRepository paymentRepository;

    public void savePayment(MoMoPaymentSave moMoPaymentSave)
    {
        paymentRepository.save(moMoPaymentSave);
    }
    public Optional<MoMoPaymentSave> getPaymentById(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

}
