package com.example.movietickets.demo.service;

import com.example.movietickets.demo.DTO.SeatDto;
import com.example.movietickets.demo.model.BookingDetail;
import com.example.movietickets.demo.model.Seat;
import com.example.movietickets.demo.repository.BookingDetailRepository;
import com.example.movietickets.demo.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatService {


    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private SeatTypeService seatTypeService;

    public List<Seat> getSeatsByRoomId(Long roomId) {
        return seatRepository.findByRoomId(roomId);
    }

    // SeatService.java
    public List<Seat> getSeatsByRoomIdDistinct(Long roomId) {
        // Assuming your repository query or method fetches distinct seats based on roomId
        return seatRepository.findDistinctSeatsByRoomId(roomId);
    }

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public Optional<Seat> getSeatById(Long seatId) {
        return seatRepository.findById(seatId);
    }

    public void addSeat(Seat seat) {
        seatRepository.save(seat);
    }

    public Seat updateSeat(Seat seat){
        if (seat.getId() == null) {
            throw new IllegalArgumentException("Seat ID cannot be null for update");
        }
        return seatRepository.save(seat);
    }
    public Seat saveSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    public void deleteSeat(Long seatId) {
        if (!seatRepository.existsById(seatId)) {
            throw new IllegalStateException("Seat with ID " + seatId + " does not exist.");
        }
        seatRepository.deleteById(seatId);
    }

    public List<Seat> getSeatListByRoomId(Long roomId) {
        return seatRepository.findByRoomId(roomId);
    }


    public List<SeatDto> getSeatsByScheduleAndRoom(Long roomId, Long scheduleId) {
        List<Seat> seats = seatRepository.findSeatsByRoomAndSchedule(roomId, scheduleId);
//        List<SeatType> seatTypes= seatTypeService.

        return seats.stream()
                .map(this::mapToDto) // Chuyển đổi sang SeatDto
                .collect(Collectors.toList());
    }

    public SeatDto mapToDto(Seat seat) {
        SeatDto seatDto = new SeatDto();
        seatDto.setId(seat.getId());
        seatDto.setSymbol(seat.getSymbol());
        seatDto.setSeatType(seat.getSeattype().getType()); // Lấy tên loại ghế
        seatDto.setPrice(seat.getSeattype().getPrice());
        seatDto.setImage(seat.getImage());
        seatDto.setStatus(seat.getStatus());
        return seatDto;
    }

    public List<Seat> getSeatsByScheduleId(Long scheduleId) {
        return seatRepository.findSeatsByScheduleId(scheduleId);
    }

    public List<SeatDto> getSeatsByRoomAndSchedule(Long roomId, Long scheduleId) {
        // Lấy danh sách ghế theo phòng
        List<Seat> seats = getSeatListByRoomId(roomId);
        List<BookingDetail> bookedSeats = bookingDetailRepository.findByScheduleId(scheduleId);
        // Lấy trạng thái ghế theo lịch chiếu
        Set<Long> bookedSeatIds = bookedSeats.stream()
                .map(detail -> detail.getSeat().getId())
                .collect(Collectors.toSet());
        // Map trạng thái ghế vào danh sách ghế
        return seats.stream().map(seat -> {
            SeatDto dto = new SeatDto();
            dto.setId(seat.getId());
            dto.setSymbol(seat.getSymbol());
            dto.setSeatType(seat.getSeattype().getType());
            dto.setRoomName(seat.getRoom().getName());
            dto.setPrice(seat.getSeattype().getPrice());
            dto.setImage(seat.getImage());
            dto.setStatus(bookedSeatIds.contains(seat.getId()));
            return dto;
        }).collect(Collectors.toList());
    }
    public List<SeatDto> getAllSeatsByRoom(Long roomId) {
        List<Seat> seats = getSeatListByRoomId(roomId);

        return seats.stream().map(seat -> {
            SeatDto dto = new SeatDto();
            dto.setId(seat.getId());
            dto.setSymbol(seat.getSymbol());
            dto.setSeatType(seat.getSeattype().getType());
            dto.setRoomName(seat.getRoom().getName());
            dto.setPrice(seat.getSeattype().getPrice());
            dto.setImage(seat.getImage());
            dto.setStatus(false); // Mặc định là ghế trống
            return dto;
        }).collect(Collectors.toList());
    }
}

