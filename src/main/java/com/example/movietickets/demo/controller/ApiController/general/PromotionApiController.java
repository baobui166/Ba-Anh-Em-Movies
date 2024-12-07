package com.example.movietickets.demo.controller.ApiController.general;

import com.example.movietickets.demo.model.Promotion;
import com.example.movietickets.demo.model.User;
import com.example.movietickets.demo.service.PromotionService;
import com.example.movietickets.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/promotion")
public class PromotionApiController {
    @Autowired
    private PromotionService promotionService;

    @Autowired
    private UserService userService;

    @PostMapping("/redeem/{id}")
    public ResponseEntity<String> redeemPromotion(@PathVariable Long id, Principal principal) {

//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();

        User user = userService.getUserByUsername(principal.getName());
        User currentUser = userService.getCurrentUser();
        // Lấy Promotion theo ID
        Promotion promotion = promotionService.getPromotionById(id);
        if (promotion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Promotion không tồn tại");
        }

        // Kiểm tra điểm của user
        if (user.getPointSaving() < promotion.getPointToRedeem()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không đủ điểm để quy đổi");
        }

        // Trừ điểm user
        user.setPointSaving(user.getPointSaving() - promotion.getPointToRedeem());
        user.getPromotions().add(promotion);
        userService.saveWithoutEncodingPassword(user);
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        User updatedUser = userService.getUserByUsername(user.getUsername());
//        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
//                updatedUser, authentication.getCredentials(), authentication.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return ResponseEntity.ok("Quy đổi thành công");
    }

}