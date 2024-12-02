package com.example.movietickets.demo.controller.general;

import com.example.movietickets.demo.model.*;
import com.example.movietickets.demo.repository.RoomRepository;
import com.example.movietickets.demo.repository.SeatRepository;
import com.example.movietickets.demo.repository.UserRepository;
import com.example.movietickets.demo.service.*;
import com.example.movietickets.demo.service.PaymentServices.ExchangeCurrencyService;
import com.example.movietickets.demo.service.PaymentServices.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleServiceImpl scheduleService;

    @Autowired
    private ComboFoodService comboFoodService;

    @Autowired
    private FilmService filmService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private final PaypalService paypalService;
    @Autowired
    private ExchangeCurrencyService exchangeCurrencyService;
//
//    @Autowired
//    private  MoMoService momoService;

    @GetMapping
    public String showPurchase(Model model, @RequestParam(required = false) Long scheduleId) {
        if (purchaseService.IsExist()) {
            Purchase purchase = purchaseService.Get();
            System.out.println("scheduleId: " + scheduleId);
            System.out.println("selectedSeats: " + purchase.getSeats());
            model.addAttribute("selectedSeats", purchase.getSeats());
            model.addAttribute("filmTitle", purchase.getFilmTitle());
            model.addAttribute("category", purchase.getCategory());
            model.addAttribute("cinemaName", purchase.getCinemaName());
            model.addAttribute("cinemaAddress", purchase.getCinemaAddress());
            model.addAttribute("startTime", purchase.getStartTime());
            model.addAttribute("roomName", purchase.getRoomName());
            model.addAttribute("poster", purchase.getPoster());

            // Logic xử lý mã giảm giá
            String appliedPromoCode = "";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = getUserFromAuthentication(authentication);
            int age = user.getAge(); // Trường age đã lưu sẵn tuổi
            long priceVoucher = 0;


            if (age < 12) {
                appliedPromoCode ="voucherTreEm";
                priceVoucher = 30000;

            } else if (age >= 12 && age <= 22) {
                appliedPromoCode ="voucherHSSV";
                priceVoucher = 20000;


            }
            model.addAttribute("promoCode", appliedPromoCode);
            //format Currency VND
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedTotalPrice = currencyFormat.format(purchase.getTotalPrice() - priceVoucher);
            model.addAttribute("totalPrice", formattedTotalPrice);



            List<ComboFood> comboFoods = comboFoodService.getAllComboFood();
            Room room = roomRepository.findByName(purchase.getRoomName());
            List<Seat> seats = seatRepository.findByRoom(room);
            //add thêm trn header
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("comboFoods", comboFoods);
            //lấy ra các seat booked
            model.addAttribute("purchase", purchase);
            model.addAttribute("seats", seats);
            model.addAttribute("scheduleId", scheduleId);
        }
        return "Purchase/purchase";
    }


    @GetMapping("/clear")
    public String clearPurchase() {
        purchaseService.clearPurchase();
        return "redirect:/purchase";
    }


    @PostMapping("/add")
    public String addPurchase(
            @RequestParam("seatSymbol") String seatSymbols,
            @RequestParam("totalPrice") Long totalPrice,
            @RequestParam("startTime") String startTime,
            @RequestParam("filmTitle") String filmTitle,
            @RequestParam("poster") String poster,
            @RequestParam("category") String category,
            @RequestParam("cinemaName") String cinemaName,
            @RequestParam("cinemaAddress") String cinemaAddress,
            @RequestParam("roomName") String roomName,
            @RequestParam("scheduleId") Long scheduleId,
            Model model
    ) {

        System.out.println("scheduleId in addPurchase: " + scheduleId); // Debugging
        purchaseService.addToBuy(seatSymbols, filmTitle, poster, category, totalPrice, cinemaAddress, cinemaName, startTime, roomName);
        model.addAttribute("scheduleId", scheduleId);
        return "redirect:/purchase?scheduleId=" + scheduleId;
    }

    @GetMapping("/history")
    public String showPurchaseHistory(Model model, @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String message) {
        List<Booking> bookings = bookingService.getBookingsByCurrentUser(); // phương thức này để lấy các booking của người dùng hiện tại
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("bookings", bookings);
        if (status != null) {
            switch (status) {
                case "success": case "Successful":
                    model.addAttribute("message", "Thanh toán thành công!");
                    break;
                case "failed":
                    model.addAttribute("message",
                            message != null ? "Thanh toán thất bại: " + message : "Thanh toán thất bại!");
                    break;
                case "error":
                    model.addAttribute("message",
                            message != null ? message : "Có lỗi xảy ra trong quá trình xử lý!");
                    break;
            }
        }

        return "Purchase/history";
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam("payment") String payment,
            @RequestParam String comboId, //nhận String từ form purchase
            @RequestParam Long scheduleId,
            RedirectAttributes redirectAttributes,
            Model model, HttpSession session

    ) throws PayPalRESTException {
        if (purchaseService.IsExist()) {
            Purchase purchase = purchaseService.Get();
            List<String> seatSymbols = new ArrayList<>();
            for (Purchase.Seat2 seat : purchase.getSeatsList()) {
                seatSymbols.add(seat.getSymbol());
            }


            Room room = roomRepository.findByName(purchase.getRoomName());
            List<Seat> seats = bookingService.getSeatsFromSymbolsAndRoom(seatSymbols, room);

            // Lấy schedule từ scheduleId
            Schedule schedule = scheduleService.getScheduleById(scheduleId).orElseThrow(() -> new IllegalArgumentException("Invalid schedule Id"));

            // Tách comboId và comboPrice từ giá trị của request parameter
            Long comboFoodId = null;
            Long comboPrice = 0L;
            if (!comboId.equals("0-0")) { // Kiểm tra xem có chọn combo hay không
                String[] comboDetails = comboId.split("-");
                comboFoodId = Long.parseLong(comboDetails[0]);
                comboPrice = Long.parseLong(comboDetails[1]);
            }

            Booking booking = new Booking();
            booking.setFilmName(purchase.getFilmTitle());
            booking.setPoster(purchase.getPoster());
            booking.setCinemaName(purchase.getCinemaName());
            booking.setCinemaAddress(purchase.getCinemaAddress());
            booking.setStartTime(parseDate(purchase.getStartTime()));
            booking.setSeatName(purchase.getSeats());
            booking.setRoomName(purchase.getRoomName());
            booking.setPayment(payment);
            booking.setStatus(false); // Hoặc giá trị khác tùy vào logic của bạn
            booking.setCreateAt(new Date());

            // Lấy thông tin người dùng hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = getUserFromAuthentication(authentication);

            // Kiểm tra tuổi từ trường age
            int age = user.getAge(); // Trường age đã lưu sẵn tuổi
            long discount = 0;

            if (age < 12) {
                discount = 30000; // Trẻ em giảm 30k
            } else if (age >= 12 && age <= 22) {
                discount = 20000; // Học sinh, sinh viên giảm 20k
            }

            // Áp dụng giảm giá vào tổng giá
            booking.setPrice(purchase.getTotalPrice() + comboPrice - discount);//cộng thêm giá từ food và trừ discount

            if (comboFoodId != null) {
                ComboFood comboFood = comboFoodService.getComboFoodById(comboFoodId).orElseThrow(() -> new EntityNotFoundException("Combo not found"));
                booking.setComboFood(comboFood);
            }

            if ("Trả tiền tại quầy".equalsIgnoreCase(payment)) {

                booking.setPayment("Thanh toán tại quầy");
            }

            // Kiểm tra phương thức thanh toán
            if ("vnpay".equalsIgnoreCase(payment)) {
                //return "redirect:/api/payment/create_payment?amount=" + purchase.getTotalPrice();
                return "redirect:/api/payment/create_payment?scheduleId=" + scheduleId + "&amount=" + booking.getPrice() + "&comboId=" + comboId;
            }
            Long comboPriceInLong = (long) getComboPrice(comboId);
            BigDecimal totalPriceUSD = exchangeCurrencyService.convertVNDToUSD(purchase.getTotalPrice() + comboPriceInLong - discount);

            if ("paypal".equalsIgnoreCase(payment)) {
                try {
                    String cancelUrl = "http://localhost:8080/purchase/cancel";
                    String successUrl = "http://localhost:8080/purchase/success?scheduleId=" + scheduleId;

                    Payment paypalPayment = paypalService.createPayment(totalPriceUSD.doubleValue(),
                            "USD",
                            "paypal",
                            "sale",
                            "Movie Ticket Purchase",
                            cancelUrl,
                            successUrl
                    );

                    for (Links link : paypalPayment.getLinks()) {
                        if (link.getRel().equals("approval_url")) {
                            return "redirect:" + link.getHref();
                        }
                    }
                } catch (PayPalRESTException e) {
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("message", "Payment failed!");
                    return "redirect:/purchase/history";
                }
            }
            if ("momo".equalsIgnoreCase(payment)) {
                return "redirect:/api/payment/momo/create_momo?scheduleId=" + scheduleId + "&amount=" + purchase.getTotalPrice() + "&comboId=" + comboId;
            }
            // Lấy thông tin người dùng hiện tại

            booking.setUser(user);

            bookingService.saveBooking(booking, seats, schedule);


            redirectAttributes.addFlashAttribute("message", "Đặt vé thành công!");
        } else {
            redirectAttributes.addFlashAttribute("message", "Không có thông tin đặt vé.");
        }
        return "redirect:/purchase/history"; // Chuyển hướng đến trang lịch sử mua vé
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            // Trường hợp đăng nhập thông thường
            String username = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
            return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } else if (authentication.getPrincipal() instanceof DefaultOAuth2User) {
            // Trường hợp đăng nhập bằng OAuth2 (Facebook, Google)
            String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
            return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
        throw new UsernameNotFoundException("User not found");
    }

    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double getComboPrice(String comboId) {
        if (!"0-0".equals(comboId)) {
            String[] comboDetails = comboId.split("-");
            return Long.parseLong(comboDetails[1]);
        }
        return 0;
    }

    @GetMapping("/success")
    public String success(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId,
                          @RequestParam("scheduleId") Long scheduleId,
                          Model model, RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if ("approved".equals(payment.getState())) {
                // Save booking information and proceed
                if (purchaseService.IsExist()) {
                    Purchase purchase = purchaseService.Get();

                    List<String> seatSymbols = new ArrayList<>();
                    for (Purchase.Seat2 seat : purchase.getSeatsList()) {
                        seatSymbols.add(seat.getSymbol());
                    }

                    Room room = roomRepository.findByName(purchase.getRoomName());
                    List<Seat> seats = bookingService.getSeatsFromSymbolsAndRoom(seatSymbols, room);

                    Schedule schedule = scheduleService.getScheduleById(scheduleId).orElseThrow(() -> new IllegalArgumentException("Invalid schedule Id"));

                    Booking booking = new Booking();
                    booking.setFilmName(purchase.getFilmTitle());
                    booking.setPoster(purchase.getPoster());
                    booking.setCinemaName(purchase.getCinemaName());
                    booking.setCinemaAddress(purchase.getCinemaAddress());
                    booking.setStartTime(parseDate(purchase.getStartTime()));
                    booking.setSeatName(purchase.getSeats());
                    booking.setRoomName(purchase.getRoomName());
                    booking.setPayment("paypal");
                    booking.setStatus(true);
                    booking.setCreateAt(new Date());
                    booking.setPrice(purchase.getTotalPrice());

                    // Retrieve the user and set on the booking
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    User user = getUserFromAuthentication(authentication);
                    booking.setUser(user);
                    System.out.println("Schedule: " + schedule.getId() + " " + schedule.getStartTime());
                    bookingService.saveBooking(booking, seats, schedule);

                    // Optionally clear the purchase after successful booking
                    purchaseService.clearPurchase();
                    redirectAttributes.addFlashAttribute("message", "Payment and booking successful!");
                } else {
                    redirectAttributes.addFlashAttribute("message", "No purchase information found.");
                }
                model.addAttribute("message", "Payment successful!");
                System.out.println("Payment successful!");
                System.out.println();
                return "redirect:/purchase/history?status=success";
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
            model.addAttribute("message", "Payment failed!");
        }
        return "redirect:/purchase/history";
    }

    @GetMapping("/cancel")
    public String cancel(Model model) {
        model.addAttribute("message", "Payment cancelled!");
        System.out.println("Payment failed to proceed!");
        System.out.println();
        return "redirect:/purchase/history?status=failed&message=" + URLEncoder.encode("Failed", StandardCharsets.UTF_8);
    }
}
