package com.example.movietickets.demo.controller.admin;


import com.example.movietickets.demo.model.Promotion;
import com.example.movietickets.demo.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public String showAllPromotions(Model model) {

        model.addAttribute("title","Quản lý khuyến mãi");
        model.addAttribute("promotions", promotionService.getAllPromotions());
        return "admin/promotion/promotion-list";
    }

    @GetMapping("/add")
    public String showAddPromotionForm(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("title","Tạo khuyến mãi");
        return "admin/promotion/promotion-add";
    }

    @PostMapping("/add")
    public String addPromotion(@Valid @ModelAttribute("promotion") Promotion promotion,BindingResult result,Model model ) {
        if (result.hasErrors()) {
            var errors = result.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "admin/promotion/promotion-add";
        }


        promotionService.createPromotion(promotion);
        return "redirect:/admin/promotions";
    }

    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable("id") Long id) {

        promotionService.deletePromotion(id);
        return "redirect:/admin/promotions";
    }


    @GetMapping("/edit/{id}")
    public String showEditPromotionForm(Model model,@PathVariable Long id) {
        var promotion = promotionService.getPromotionById(id);
        model.addAttribute("promotion", promotion);
        model.addAttribute("title","Chỉnh sửa khuyến mãi");
        return "admin/promotion/promotion-edit";
    }

    @PostMapping("/edit")
    public String editPromotion(@Valid @ModelAttribute("promotion") Promotion promotion, BindingResult result,Model model) {
        if (result.hasErrors()) {
            var errors = result.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "admin/promotion/promotion-edit";
        }
        promotionService.editPromotion(promotion);
        return "redirect:/admin/promotions";
    }
}
