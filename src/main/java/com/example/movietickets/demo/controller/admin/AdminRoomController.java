package com.example.movietickets.demo.controller.admin;

import com.example.movietickets.demo.DTO.SeatDto;
import com.example.movietickets.demo.model.*;
import com.example.movietickets.demo.service.*;
import com.example.movietickets.demo.ultillity.CheckingNumber;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@AllArgsConstructor
@RequestMapping("/admin")
public class AdminRoomController {
    @Autowired
    private final RoomService roomService;
    @Autowired
    private final CinemaService cinemaService;
    @Autowired
    private final SeatService seatService;
    //
//    @Autowired
//    private final SeatStatusService seatStatusService;
    @Autowired
    private final ScheduleServiceImpl scheduleService;

    // Hiển thị danh sách danh mục
    @GetMapping("/rooms")
    public String listRooms(Model model) {
        List<Room> listRoom = roomService.getAllRooms();
        model.addAttribute("rooms", listRoom);
        model.addAttribute("title", "Danh sách phòng chiếu");
        return "/admin/room/room-list";
    }

    @GetMapping("/rooms/{id}")
    public String getRoom(@PathVariable Long id, Model model,
                          @RequestParam(value = "scheduleId", required = false) Long scheduleId) {
        model.addAttribute("title", "Chi tiết phòng");
        Optional<Room> room = roomService.getRoomById(id);

        List<Schedule> schedules = scheduleService.getByRoomId(id);

        // Nếu không có scheduleId, chọn mặc định lịch đầu tiên
        if (scheduleId == null && !schedules.isEmpty()) {
            scheduleId = schedules.get(0).getId();
        }

        List<SeatDto> seats;
        // Nếu chọn "Tất cả" (value = 0)
        if (scheduleId == 0) {
            seats = seatService.getAllSeatsByRoom(id);
        } else {
            seats = seatService.getSeatsByRoomAndSchedule(id, scheduleId);
        }

        model.addAttribute("schedules", schedules);
        model.addAttribute("seats", seats);
        model.addAttribute("room", room.get());
        model.addAttribute("selectedScheduleId", scheduleId);

        return "/admin/room/room-detail";
    }

    //gửi response ra view add
    @GetMapping("/rooms/add")
    public String showAddForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "/admin/room/room-add";
    }

    //gọi phương thức mapp tới form add
    @PostMapping("/rooms/add")
    public String addRoom(@Valid Room room, BindingResult result) {
        roomService.addRoom(room);
        return "redirect:/admin/rooms";
    }

    // GET request to show room edit form
    @GetMapping("/rooms/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room Id:" + id));
        model.addAttribute("room", room);
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "/admin/room/room-edit";
    }

    // POST request to update room
    @PostMapping("/rooms/edit/{id}")
    public String updateRoom(@PathVariable("id") Long id, @Valid Room room, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("cinemas", cinemaService.getAllCinemas());
            return "/admin/room/room-edit";
        }

        Room existingRoom = roomService.getRoomById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room Id:" + id));

        // Update the fields
        existingRoom.setName(room.getName());
        existingRoom.setDescription(room.getDescription());
        existingRoom.setCinema(room.getCinema());

        roomService.updateRoom(existingRoom);
        model.addAttribute("rooms", roomService.getAllRooms());
        return "redirect:/admin/rooms";
    }

    // GET request for deleting room
    @GetMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, Model model) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phong chieu Id:" + id));

        roomService.deleteRoomById(id);
        model.addAttribute("rooms", roomService.getAllRooms());
        return "redirect:/admin/rooms";
    }
}