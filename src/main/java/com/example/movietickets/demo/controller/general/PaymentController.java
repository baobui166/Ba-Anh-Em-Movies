package com.example.movietickets.demo.controller.general;

import com.example.movietickets.demo.model.MoMoPaymentSave;
import com.example.movietickets.demo.DTO.PaymentResDTO;
import com.example.movietickets.demo.DTO.ScheduleComboDTO;
import com.example.movietickets.demo.config.VnPayConfig;
import com.example.movietickets.demo.model.*;
import com.example.movietickets.demo.repository.RoomRepository;
import com.example.movietickets.demo.repository.SeatRepository;
import com.example.movietickets.demo.repository.UserRepository;
import com.example.movietickets.demo.service.*;
import com.example.movietickets.demo.service.PaymentServices.MoMoRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

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
    private CategoryService categoryService;
    @Autowired
    private UserService userService;

    @Autowired
    private MoMoPaymentService paymentService;


    @GetMapping("create_payment")
    public ResponseEntity<?> createPayment(@RequestParam("amount") long amount, @RequestParam("scheduleId") Long scheduleId,
                                           @RequestParam("comboId") String comboId) throws UnsupportedEncodingException {

        // Kiểm tra giá trị amount
        System.out.println("Amount received: " + amount);

        //String orderType = "other";
        //long amount = Integer.parseInt(req.getParameter("amount"))*100;
        //String bankCode = req.getParameter("bankCode");
        String getUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        //long amount = 10000;
        String amountValue = String.valueOf(amount * 100);
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        //String vnp_IpAddr = Config.getIpAddress(req);
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VnPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VnPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", amountValue);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", "NCB");
        vnp_Params.put("vnp_Locale", "vn");
        //vnp_Params.put("vnp_TransactionNo", "NCB");
        vnp_Params.put("vnp_IpAddr", VnPayConfig.vnp_IpAddr);

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_ReturnUrl",getUrl + VnPayConfig.vnp_ReturnUrl);


        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 5);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.secretKey, hashData.toString());
        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + queryUrl;

        PaymentResDTO paymentResDTO = new PaymentResDTO();
        paymentResDTO.setStatus("OK");
        paymentResDTO.setMessage("Successfully");
        paymentResDTO.setUrl(paymentUrl);

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
        booking.setPayment("vnpay");
        booking.setStatus(true); // Hoặc giá trị khác tùy vào logic của bạn
        booking.setCreateAt(new Date());
        booking.setPrice(purchase.getTotalPrice() + comboPrice); //cộng thêm giá từ food

        if (comboFoodId != null) {
            ComboFood comboFood = comboFoodService.getComboFoodById(comboFoodId).orElseThrow(() -> new EntityNotFoundException("Combo not found"));
            booking.setComboFood(comboFood);
        }


        // Lấy thông tin người dùng hiện tại
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        booking.setUser(user);


        bookingService.saveBooking(booking, seats, schedule);

        // Trả về trang HTML tự động chuyển hướng
        String htmlResponse = "<html><body>"
                + "<form id='paymentForm' action='" + paymentUrl + "' method='GET'></form>"
                + "<script type='text/javascript'>document.getElementById('paymentForm').submit();</script>"
                + "</body></html>";

        // return ResponseEntity.status(HttpStatus.OK).body(htmlResponse);

        //return ResponseEntity.status(HttpStatus.OK).body(paymentResDTO);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(paymentUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);

        //return paymentUrl;

    }

    @GetMapping("/momo/create_momo")
    public ResponseEntity<?> createMoMoPayment(
            @RequestParam(value = "scheduleId") Long scheduleId,
            @RequestParam(value = "amount") Long amount,
            @RequestParam(value = "comboId") String comboId) throws Exception {

        String orderId = String.valueOf(System.currentTimeMillis());

        ObjectMapper objectMapper = new ObjectMapper();
        ScheduleComboDTO dto = new ScheduleComboDTO(scheduleId, comboId);
        String jsonString = objectMapper.writeValueAsString(dto);

        MoMoRequestService.Extra_Data = Base64.getEncoder().encodeToString(jsonString.getBytes());

        // Tạo signature cho yêu cầu thanh toán
        String rawSignature = String.format("accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                MoMoRequestService.accessKey,
                amount,
                MoMoRequestService.Extra_Data,
                MoMoRequestService.getUrl() + MoMoRequestService.ipnUrl,
                orderId,
                MoMoRequestService.orderInfo,
                MoMoRequestService.partnerCode,
                MoMoRequestService.getUrl() + MoMoRequestService.redirectUrl,
                orderId,
                MoMoRequestService.requestType);

        String signature = MoMoRequestService.HmacSHA256(rawSignature, MoMoRequestService.secretKey);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("partnerCode", MoMoRequestService.partnerCode);
        requestBody.addProperty("requestId", orderId);
        requestBody.addProperty("amount", String.valueOf(amount));
        requestBody.addProperty("orderId", orderId);
        requestBody.addProperty("orderInfo", MoMoRequestService.orderInfo);
        requestBody.addProperty("redirectUrl",MoMoRequestService.getUrl()+ MoMoRequestService.redirectUrl);
        requestBody.addProperty("ipnUrl",MoMoRequestService.getUrl()+ MoMoRequestService.ipnUrl);
        requestBody.addProperty("lang", MoMoRequestService.LANGUAGE);
        requestBody.addProperty("requestType", MoMoRequestService.requestType);
        requestBody.addProperty("autoCapture", MoMoRequestService.Auto_Capture);
        requestBody.addProperty("extraData", MoMoRequestService.Extra_Data);
        requestBody.addProperty("signature", signature);

        MoMoPaymentSave payment = new MoMoPaymentSave();
        payment.setOrderId(orderId);
        payment.setRequestId(orderId);
        payment.setAmount(Long.parseLong(String.valueOf(amount)));
        payment.setCreatedAt(new Date());
        paymentService.savePayment(payment);

        System.out.println(requestBody);

        String response = MoMoRequestService.sendToHTTPPost(MoMoRequestService.Endpoint, requestBody.toString());

        System.out.println(response);

        JsonObject responseObject = new JsonParser().parse(response).getAsJsonObject();


        // Kiểm tra phản hồi của MoMo
        if (responseObject.has("payUrl")) {
            String payUrl = responseObject.get("payUrl").getAsString();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", payUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } else {
            String message = responseObject.has("message") ?
                    responseObject.get("message").getAsString() : "Payment creation failed";
            return ResponseEntity.badRequest().body(message);
        }
    }

    @PostMapping("/momo/momo-ipn")
    public ResponseEntity<?> processMoMoIPNCallback(@RequestBody String jsonStr) {
        try {
            JsonObject ipnData = new JsonParser().parse(jsonStr).getAsJsonObject();

            System.out.println("ipnData" + ipnData);

            String orderId = ipnData.get("orderId").getAsString();
            String requestId = ipnData.get("requestId").getAsString();
            Long amount = ipnData.get("amount").getAsLong();
            Long transId = ipnData.get("transId").getAsLong();
            Integer resultCode = ipnData.get("resultCode").getAsInt();
            String payType = ipnData.get("payType").getAsString();
            Long responseTime = ipnData.get("responseTime").getAsLong();

            String extraData = ipnData.get("extraData").getAsString();
            Optional<MoMoPaymentSave> payment = paymentService.getPaymentById(orderId);
            if (payment.isPresent()) {

                MoMoPaymentSave paymentDto = payment.get();
                paymentDto.setAmount(amount);
                paymentDto.setTransactionId(String.valueOf(transId));
                paymentDto.setPaymentMethod(payType); // Giả sử bạn đã thêm trường payType vào MoMoPaymentDto
                paymentService.savePayment(paymentDto);
            } else {
                return ResponseEntity.badRequest().body("{\"message\": \"Payment not found for orderId: " + orderId + "\"}");
            }
            // Verify signature
            String rawSignature = String.format("accessKey=%s&amount=%s&extraData=%s&orderId=%s&orderInfo=%s&partnerCode=%s&requestId=%s&resultCode=%s&payType=%s&responseTime=%s",
                    MoMoRequestService.accessKey,
                    amount,
                    ipnData.get("extraData").getAsString(),
                    orderId,
                    ipnData.get("orderInfo").getAsString(),
                    MoMoRequestService.partnerCode,
                    requestId,
                    resultCode,payType,responseTime);

            String signature = MoMoRequestService.HmacSHA256(rawSignature, MoMoRequestService.secretKey);
            if (!signature.equals(ipnData.get("signature").getAsString())) {
                return ResponseEntity.badRequest().body("{\"message\": \"Invalid signature\"}");
            }
            System.out.println(resultCode);

            if ("0".equals(resultCode)) {
                ResponseEntity.ok(204);
            }
            else {
                // Check the result code and handle errors accordingly
                if ("1001".equals(resultCode)) {
                    // Code 1001 typically indicates insufficient funds
                    return ResponseEntity.badRequest().body("{\"message\": \"Transaction failed due to insufficient funds\"}");
                }
                // Handle other error codes here
                return ResponseEntity.badRequest().body("{\"message\": \"Transaction failed. Error code: " + resultCode + "\"}");
            }
            // Trả về response cho MoMo
            JsonObject response = new JsonObject();
            response.addProperty("partnerCode", MoMoRequestService.partnerCode);
            response.addProperty("requestId", requestId);
            response.addProperty("orderId", orderId);
            response.addProperty("resultCode", resultCode);
            response.addProperty("message", "success");
/*
            response.addProperty("extraData", updatedExtraData);
*/

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("resultCode", "99");
            errorResponse.addProperty("message", "Error processing IPN: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
        }
    }

    @GetMapping("/momo/momo-return")
    public ResponseEntity<?> processMoMoReturn(
            @RequestParam("partnerCode") String partnerCode,
            @RequestParam("extraData") String extraData,
            @RequestParam("orderId") String orderId,
            @RequestParam("requestId") String requestId,
            @RequestParam("amount") String amount,
            @RequestParam("payType") String payType,
            @RequestParam("transId") Long transId,
            @RequestParam("resultCode") String resultCode,
            @RequestParam("responseTime") Long responseTime,
            @RequestParam("message") String message) {

        try {
            HttpHeaders headers = new HttpHeaders();

            String decodedExtraData = new String(Base64.getDecoder().decode(extraData));
            JsonObject extraDataJson = new JsonParser().parse(decodedExtraData).getAsJsonObject();

            System.out.println(extraDataJson);

            // Kiểm tra payment trong database
            Optional<MoMoPaymentSave> payment = paymentService.getPaymentById(orderId);
            MoMoPaymentSave momo = payment.get();

            if ("0".equals(resultCode)) {
                // Thanh toán thành công
                Long scheduleId = extraDataJson.get("scheduleId").getAsLong();
                String comboId = extraDataJson.get("comboId").getAsString();

                Schedule schedule = scheduleService.getScheduleById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid schedule Id"));

                Purchase purchase = purchaseService.Get();
                List<String> seatSymbols = new ArrayList<>();
                for (Purchase.Seat2 seat : purchase.getSeatsList()) {
                    seatSymbols.add(seat.getSymbol());
                }
                Room room = roomRepository.findByName(purchase.getRoomName());
                List<Seat> seats = bookingService.getSeatsFromSymbolsAndRoom(seatSymbols, room);

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
                booking.setPayment("MoMo");
                booking.setStatus(true);
                booking.setCreateAt(new Date());
                booking.setPrice(purchase.getTotalPrice() + comboPrice);

                if (comboFoodId != null) {
                    ComboFood comboFood = comboFoodService.getComboFoodById(comboFoodId)
                            .orElseThrow(() -> new EntityNotFoundException("Combo not found"));
                    booking.setComboFood(comboFood);
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                User user = getUserFromAuthentication(authentication);
                booking.setUser(user);


                bookingService.saveBooking(booking, seats, schedule);
                momo.setBooking(booking);
                momo.setPaymentMethod(payType);
                momo.setTransactionId(Long.toString(transId));
                momo.setPaymentTime(new Date());
                momo.setPaymentStatus("SUCCESS");
                if(message.contains("Thất bại"))
                    momo.setFailureMessage(message);

                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(responseTime), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                String formattedDate = dateTime.format(formatter);

                momo.setResponseTime(LocalDateTime.parse(formattedDate));

                paymentService.savePayment(momo);
                // Luôn chuyển hướng đến trang thành công khi resultCode là 0
                headers.setLocation(URI.create("/purchase/history?status=success"));
            } else {
                // Thanh toán thất bại
                String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
                headers.setLocation(URI.create("/purchase/history?status=failed&message=" + encodedMessage));
            }

            return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);

        } catch (Exception e) {
            HttpHeaders headers = new HttpHeaders();
            String encodedError = URLEncoder.encode("Có lỗi xảy ra: " + e.getMessage(), StandardCharsets.UTF_8);
            headers.setLocation(URI.create("/purchase/history?status=error&message=" + encodedError));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
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


}